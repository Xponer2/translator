/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;

import ladder.rung1206.provided.MeshData;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * 1.21's buffer source ends a batch in its own {@code endBatch(type, builder)} (the mods' mixins aim at it: iris's
 * flushes, sodium's), which endBatch(type) calls for a started builder; 1.20.6 ends it inline. The source gets 1.21's
 * method (1.20.6's end of the type, the shared type forgotten) and its endBatch(type) calls it, 1.20.6's choice of
 * builders kept.
 */
@Mixin(value = MultiBufferSource.BufferSource.class, priority = 500)
abstract class BufferSourceEndBatchMixin {
    @Shadow
    protected Optional<RenderType> lastState;

    @Shadow
    @Final
    protected BufferBuilder builder;

    @Shadow
    @Final
    protected Set<BufferBuilder> startedBuffers;

    @Shadow
    private BufferBuilder getBuilderRaw(RenderType type) {
        throw new AssertionError();
    }

    // an overwrite, not an injector: the mods' points are 1.21's endBatch(type, builder) call here
    /**
     * @author ladder
     * @reason 1.21's endBatch(type): a started builder of the type is ended in endBatch(type, builder)
     */
    @Overwrite
    public void endBatch(RenderType type) {
        BufferBuilder started = this.getBuilderRaw(type);
        boolean shared = Objects.equals(this.lastState, type.asOptional());
        if ((shared || started != this.builder) && this.startedBuffers.remove(started)) {
            this.endBatch(type, started);
        }
    }

    /**
     * 1.21's endBatch(type, builder) in its shape: the builder builds, the mesh sorts its quads, the type draws it. 1.20.6
     * sorts while the builder ends, so the sorting is set on it first (the mesh's sortQuads keeps the quads as they are)
     * and the translucent order is 1.20.6's; the steps are 1.20.6's end of the type's batch. The shared type, when it was
     * this one, forgotten.
     */
    private void endBatch(RenderType type, BufferBuilder started) {
        if (started.building()) {
            VertexSorting sorting = RenderSystem.getVertexSorting();
            boolean sort = ladder.rung1206.render.RenderTypes.sortOnUpload(type);
            if (sort) {
                started.setQuadSorting(sorting);
            }
            MeshData mesh = new MeshData(started.end());
            if (sort) {
                mesh.sortQuads(ladder.rung1206.render.ByteBuffers.sharedBufferIfCarried((MultiBufferSource.BufferSource) (Object) this), sorting);
            }
            ((ladder.rung1206.render.RenderTypeAdded) (Object) type).draw(mesh);
        }
        if (Objects.equals(this.lastState, type.asOptional())) {
            this.lastState = Optional.empty();
        }
    }
}
