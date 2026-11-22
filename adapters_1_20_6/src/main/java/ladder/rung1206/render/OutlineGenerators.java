/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.OutlineBufferSource;

/**
 * 1.21's outline generator, a record of the consumer it writes to and the outline's ARGB colour, on 1.20.6's (made public
 * by the adapters' access transformer), which keeps the colour as its default one, channel by channel.
 */
public final class OutlineGenerators {
    private OutlineGenerators() {
    }

    /** 1.21's {@code new EntityOutlineGenerator(delegate, color)} */
    public static OutlineBufferSource.EntityOutlineGenerator create(VertexConsumer delegate, int color) {
        return new OutlineBufferSource.EntityOutlineGenerator(delegate, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24);
    }

    /** 1.21's {@code generator.color()}: its ARGB colour */
    public static int color(OutlineBufferSource.EntityOutlineGenerator generator) {
        return (generator.defaultA & 0xFF) << 24 | (generator.defaultR & 0xFF) << 16 | (generator.defaultG & 0xFF) << 8 | generator.defaultB & 0xFF;
    }

    /** 1.21's {@code generator.delegate()} */
    public static VertexConsumer delegate(OutlineBufferSource.EntityOutlineGenerator generator) {
        return generator.delegate;
    }
}
