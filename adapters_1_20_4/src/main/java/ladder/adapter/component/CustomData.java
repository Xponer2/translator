/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;

/** 1.20.5's {@code CustomData}: a stack's own NBT (on 1.20.4: the stack's tag, past the game's own keys). */
public final class CustomData {
    public static final CustomData EMPTY = new CustomData(new CompoundTag());
    public static final Codec<CustomData> CODEC = CompoundTag.CODEC.xmap(CustomData::new, data -> data.tag);
    /** 1.21's {@code ByteBufCodecs.COMPOUND_TAG.map(CustomData::new, data -> data.tag)} */
    public static final ladder.carried.net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, CustomData> STREAM_CODEC =
            new ladder.carried.net.minecraft.network.codec.StreamCodec<>() {
                @Override
                public CustomData decode(io.netty.buffer.ByteBuf buf) {
                    CompoundTag tag = new net.minecraft.network.FriendlyByteBuf(buf).readNbt();
                    return new CustomData(tag == null ? new CompoundTag() : tag);
                }

                @Override
                public void encode(io.netty.buffer.ByteBuf buf, CustomData data) {
                    new net.minecraft.network.FriendlyByteBuf(buf).writeNbt(data.tag);
                }
            };
    private final CompoundTag tag;

    private CustomData(CompoundTag tag) {
        this.tag = tag;
    }

    public static CustomData of(CompoundTag tag) {
        return new CustomData(tag.copy());
    }

    public CompoundTag copyTag() {
        return this.tag.copy();
    }

    public boolean isEmpty() {
        return this.tag.isEmpty();
    }

    public boolean contains(String key) {
        return this.tag.contains(key);
    }

    /** read only: a copy is the way to change it */
    CompoundTag unsafe() {
        return this.tag;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CustomData data && this.tag.equals(data.tag);
    }

    @Override
    public int hashCode() {
        return this.tag.hashCode();
    }

    @Override
    public String toString() {
        return this.tag.toString();
    }
}
