/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import ladder.rung1206.render.VertexAdapters;

/**
 * 1.21's {@code BufferBuilder.addVertex(x, y, z, color, u, v, overlay, light, nx, ny, nz)} on 1.20.6's builder (the
 * method is the class's own, as 1.21 has it): a fast format writes the whole vertex at once (1.20.6's whole-vertex
 * call), another element by element (1.21's default of VertexConsumer). Mods' mixins aim at its fastFormat read (iris
 * sends its formats the element way).
 */
@Mixin(value = BufferBuilder.class, priority = 500)
abstract class BufferBuilderAddVertexMixin {
    @Shadow
    private boolean fastFormat;

    public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        VertexConsumer self = (VertexConsumer) (Object) this;
        if (this.fastFormat) {
            VertexAdapters.addWholeVertex(self, x, y, z, color, u, v, overlay, light, nx, ny, nz);
        } else {
            VertexAdapters.addVertex(self, x, y, z);
            VertexAdapters.setColor(self, color);
            VertexAdapters.setUv(self, u, v);
            VertexAdapters.setOverlay(self, overlay);
            VertexAdapters.setLight(self, light);
            VertexAdapters.setNormal(self, nx, ny, nz);
        }
    }
}
