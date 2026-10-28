package io.github.xponer2.translator.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.pipeline.LightUtil;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The 1.21 item/block drawing calls of renderers, drawn at once with the current GL matrix ({@link PoseStack}).
 * Client only.
 */
public final class Rendering {
    private static final EnumFacing[] SIDES = { EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, null };

    private Rendering() { }

    /**
     * 1.21 {@code ItemRenderer#renderStatic(stack, context, light, overlay, pose, buffer, level, seed)}.
     */
    public static void renderStatic(ItemStack stack, ItemCameraTransforms.TransformType context, int light, int overlay,
                                    PoseStack pose, MultiBufferSource buffer, @Nullable World level, int seed) {
        if (stack.isEmpty()) {
            return;
        }
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >>> 16);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, context);
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }

    /**
     * 1.21 {@code BlockRenderDispatcher#renderBatched(state, pos, level, pose, consumer, checkSides, random, data,
     * type)}: the block model at the current matrix, in the render state of the consumer.
     */
    public static void renderBatched(IBlockState state, BlockPos pos, World level, PoseStack pose, VertexConsumer consumer,
                                     boolean checkSides, Random random, ModelData data, @Nullable RenderType type) {
        var minecraft = Minecraft.getMinecraft();
        var model = minecraft.getBlockRendererDispatcher().getModelForState(state);
        minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        consumer.type().setupRenderState();
        var tessellator = Tessellator.getInstance();
        var buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
        for (var side : SIDES) {
            for (var quad : model.getQuads(state, side, 0L)) {
                LightUtil.renderQuadColor(buffer, quad, 0xFFFFFFFF);
            }
        }
        tessellator.draw();
        consumer.type().clearRenderState();
    }

    /**
     * 1.21 EntityRenderDispatcher#render(entity, x, y, z, yaw, partialTicks, pose, buffer, light): the entity at the
     * current matrix.
     */
    public static void renderEntity(net.minecraft.entity.Entity entity, double x, double y, double z, float yaw, float partialTicks,
                                    Object pose, Object buffer, int light) {
        var manager = Minecraft.getMinecraft().getRenderManager();
        net.minecraft.client.renderer.GlStateManager.enableLighting();
        manager.renderEntity(entity, x, y, z, yaw, partialTicks, false);
        net.minecraft.client.renderer.GlStateManager.disableLighting();
    }
}
