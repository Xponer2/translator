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
