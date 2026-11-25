/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexSorting;

import ladder.rung1206.provided.MeshData;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * 1.21's {@code RenderType.draw(MeshData)} on 1.20.6 (added to the class), and 1.20.6's {@code end(builder, sorting)}
 * in 1.21's shape: the builder ends, then draw does the state setup, the upload and the state clearing, as 1.20.6's end
 * did them in the same order. Mods aim at the calls inside draw (iris redirects the state setup and clearing), so the
 * game's own draws go through it too.
 */
@Mixin(value = RenderType.class, priority = 500)
abstract class RenderTypeDrawMixin extends RenderStateShard {
    @Shadow
    @Final
    private boolean sortOnUpload;

    RenderTypeDrawMixin(String name, Runnable setup, Runnable clear) {
        super(name, setup, clear);
    }

    public void draw(MeshData mesh) {
        this.setupRenderState();
        BufferUploader.drawWithShader(mesh.buffer());
        this.clearRenderState();
    }

    // an overwrite, not an injector: the mods' points are the calls 1.21 makes in draw, which 1.20.6 makes here
    /**
     * @author ladder
     * @reason 1.21's shape of a render type's draw (1.20.6's steps, in its order)
     */
    @Overwrite
    public void end(BufferBuilder builder, VertexSorting sorting) {
        if (builder.building()) {
            if (this.sortOnUpload) {
                builder.setQuadSorting(sorting);
            }
            this.draw(new MeshData(builder.end()));
        }
    }
}
