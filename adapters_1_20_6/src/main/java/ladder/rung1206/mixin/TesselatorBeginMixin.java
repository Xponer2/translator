/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import ladder.rung1206.render.VertexAdapters;

/**
 * 1.21's {@code Tesselator.begin(mode, format)} on 1.20.6's tesselator (the method is the class's own, as 1.21 has it):
 * the tesselator's builder, begun. The mods call it, and their mixins aim at it (owo's batched UI keeps its own builder).
 */
@Mixin(value = Tesselator.class, priority = 500)
abstract class TesselatorBeginMixin {
    public BufferBuilder begin(VertexFormat.Mode mode, VertexFormat format) {
        return VertexAdapters.begin((Tesselator) (Object) this, mode, format);
    }
}
