/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

import ladder.rung1206.render.PendingVertex;
import ladder.rung1206.render.VertexAdapters;

/**
 * 1.21's vertices are written when the next one begins or the buffer is built; the adapters keep the begun ones
 * (ladder.rung1206.render.VertexAdapters). Before a 1.20.6 buffer ends, the begun vertices that go into it are written
 * out (its own, and those of wrappers over it); before the game writes a whole vertex, this builder's own; a new batch
 * forgets a begun one.
 */
@Mixin(BufferBuilder.class)
abstract class BufferBuilderMixin implements PendingVertex {
    @Shadow
    private VertexFormat format;

    @Override
    public VertexFormat ladder$format() {
        return this.format;
    }

    @Inject(method = "begin", at = @At("HEAD"))
    private void ladder$begin(CallbackInfo ci) {
        VertexAdapters.forget((BufferBuilder) (Object) this);
    }

    @Inject(method = "vertex(FFFFFFFFFIIFFF)V", at = @At("HEAD"))
    private void ladder$wholeVertex(CallbackInfo ci) {
        VertexAdapters.endVertex((BufferBuilder) (Object) this);
    }

    @Inject(method = "end", at = @At("HEAD"))
    private void ladder$end(CallbackInfoReturnable<BufferBuilder.RenderedBuffer> ci) {
        VertexAdapters.endFor((BufferBuilder) (Object) this);
    }

    @Inject(method = "endOrDiscardIfEmpty", at = @At("HEAD"))
    private void ladder$endOrDiscard(CallbackInfoReturnable<BufferBuilder.RenderedBuffer> ci) {
        VertexAdapters.endFor((BufferBuilder) (Object) this);
    }
}
