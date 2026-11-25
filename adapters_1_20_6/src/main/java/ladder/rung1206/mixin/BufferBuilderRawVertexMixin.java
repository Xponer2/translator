/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;


/**
 * 1.21's raw vertex writing on 1.20.6's builder (the methods are the class's own, as 1.21 has them): beginVertex makes a
 * whole vertex's room at the write position (counted, its address returned: the mod writes its elements there, as
 * particle_core's particles do), putRgba and putPackedUv write a colour and a packed light/overlay at an address. A
 * vertex begun so in a lines mode is doubled when the next one begins or the batch ends (1.21 doubles it then; 1.20.6's
 * own vertices are doubled by their endVertex).
 */
@Mixin(value = BufferBuilder.class, priority = 500)
abstract class BufferBuilderRawVertexMixin {
    @Unique
    private static final boolean LADDER$LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    @Shadow
    private ByteBuffer buffer;
    @Shadow
    private int nextElementByte;
    @Shadow
    private int vertices;
    @Shadow
    private VertexFormat format;
    @Shadow
    private VertexFormat.Mode mode;

    /** whether the last vertex was begun raw (its lines-mode double still due) */
    @Unique
    private boolean ladder$rawLast;

    @Shadow
    private void ensureCapacity(int bytes) {
        throw new AssertionError();
    }

    /** 1.21's beginVertex(): the last raw vertex closed, a new vertex's room at the write position, its address */
    private long beginVertex() {
        this.ladder$closeRaw();
        int size = this.format.getVertexSize();
        this.ensureCapacity(size * 2);
        long address = MemoryUtil.memAddress(this.buffer) + this.nextElementByte;
        this.nextElementByte += size;
        this.vertices++;
        this.ladder$rawLast = true;
        return address;
    }

    /** 1.21's putRgba(address, argb): the colour as its bytes in the vertex (ABGR in memory order) */
    private static void putRgba(long address, int argb) {
        int abgr = argb & 0xFF00FF00 | (argb >> 16 & 0xFF) | (argb & 0xFF) << 16;
        MemoryUtil.memPutInt(address, LADDER$LITTLE_ENDIAN ? abgr : Integer.reverseBytes(abgr));
    }

    /** 1.21's putPackedUv(address, packed): two shorts (u low, v high) */
    private static void putPackedUv(long address, int packed) {
        if (LADDER$LITTLE_ENDIAN) {
            MemoryUtil.memPutInt(address, packed);
        } else {
            MemoryUtil.memPutShort(address, (short) (packed & 65535));
            MemoryUtil.memPutShort(address + 2L, (short) (packed >> 16 & 65535));
        }
    }

    /** 1.21's end of the last vertex for a raw one: in a lines mode, the vertex doubled */
    @Unique
    private void ladder$closeRaw() {
        if (this.ladder$rawLast && (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP)) {
            int size = this.format.getVertexSize();
            this.ensureCapacity(size * 2);
            long at = MemoryUtil.memAddress(this.buffer) + this.nextElementByte;
            MemoryUtil.memCopy(at - size, at, size);
            this.nextElementByte += size;
            this.vertices++;
        }
        this.ladder$rawLast = false;
    }

    @Inject(method = {"end()Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;",
            "endOrDiscardIfEmpty()Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;"}, at = @At("HEAD"))
    private void ladder$closeRawAtEnd(CallbackInfoReturnable<?> cir) {
        this.ladder$closeRaw();
    }
}
