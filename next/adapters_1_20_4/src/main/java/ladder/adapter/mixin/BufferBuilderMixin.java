/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

import ladder.adapter.render.PendingVertex;
import ladder.adapter.render.RenderAdapters;

/**
 * 1.21's vertices are written when the next one begins or the buffer is built; the adapters keep the begun ones
 * (ladder.adapter.render.RenderAdapters). Before a 1.20.4 buffer ends, every begun vertex is written out (its own, and
 * those of wrappers that may write into it); before the game writes a whole vertex, this builder's own; a new batch
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
        RenderAdapters.forget((BufferBuilder) (Object) this);
    }

    @Inject(method = "vertex(FFFFFFFFFIIFFF)V", at = @At("HEAD"))
    private void ladder$wholeVertex(CallbackInfo ci) {
        RenderAdapters.endVertex((BufferBuilder) (Object) this);
    }

    @Inject(method = "end", at = @At("HEAD"))
    private void ladder$end(CallbackInfoReturnable<BufferBuilder.RenderedBuffer> ci) {
        RenderAdapters.endAll();
    }

    @Inject(method = "endOrDiscardIfEmpty", at = @At("HEAD"))
    private void ladder$endOrDiscard(CallbackInfoReturnable<BufferBuilder.RenderedBuffer> ci) {
        RenderAdapters.endAll();
    }
}
