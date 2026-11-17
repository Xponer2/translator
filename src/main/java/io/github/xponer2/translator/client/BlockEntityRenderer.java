/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A tile entity renderer (1.21 {@code BlockEntityRenderer}), drawn by a 1.12.2 TESR ({@link TesrAdapter}) at the tile
 * entity's position with the GL matrix. Client only.
 */
public interface BlockEntityRenderer<T extends TileEntity> {
    void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay);

    // 1.12.2 name of 1.21 shouldRenderOffScreen
    default boolean isGlobalRenderer(T blockEntity) {
        return false;
    }

    /**
     * 1.21 NeoForge render bounding box; on 1.12.2 the tile entity's own getRenderBoundingBox counts.
     */
    default AxisAlignedBB getRenderBoundingBox(T blockEntity) {
        return blockEntity.getRenderBoundingBox();
    }

    /** 1.21 {@code shouldRender(blockEntity, cameraPos)}: 1.12.2 culls by the render distance itself. */
    default boolean shouldRender(T blockEntity, net.minecraft.util.math.Vec3d cameraPos) {
        return true;
    }

    /** 1.21 {@code getViewDistance()}: blocks (1.12.2 tile entities draw to 64). */
    default int getViewDistance() {
        return 64;
    }
}
