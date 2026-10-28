/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
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
}
