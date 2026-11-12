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

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code BlockEntityWithoutLevelRenderer} (a mod's renderer of items with a built-in model, which
 * its item's client extensions give): the 1.12.2 item renderer of the item, calling the 1.21 {@code renderByItem}
 * with the pose and buffers of the Translator. The mods extend it with the 1.21 constructor (dispatcher, models).
 */
@SideOnly(Side.CLIENT)
public class BlockEntityWithoutLevelRenderer extends TileEntityItemStackRenderer {
	public BlockEntityWithoutLevelRenderer(net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher dispatcher,
			io.github.xponer2.translator.client.model.EntityModelSet models) {
	}

	/** 1.21 {@code renderByItem(stack, context, pose, buffers, light, overlay)}: what the mod draws (nothing here). */
	public void renderByItem(ItemStack stack, ItemCameraTransforms.TransformType context, PoseStack poseStack, MultiBufferSource buffer, int light,
			int overlay) {
	}

	/** 1.21 {@code onResourceManagerReload}: nothing cached here. */
	public void onResourceManagerReload(Object resourceManager) {
	}

	@Override
	public void renderByItem(ItemStack stack) {
		this.renderByItem(stack, 1.0F);
	}

	@Override
	public void renderByItem(ItemStack stack, float partialTicks) {
		int light = (int) OpenGlHelper.lastBrightnessX | (int) OpenGlHelper.lastBrightnessY << 16;
		int depth = PoseStack.depth();
		GlStateManager.pushMatrix();
		try {
			this.renderByItem(stack, ItemCameraTransforms.TransformType.NONE, PoseStack.INSTANCE, MultiBufferSource.INSTANCE, light,
					OverlayTexture.NO_OVERLAY);
		} catch (RuntimeException | LinkageError | com.google.common.util.concurrent.ExecutionError | AssertionError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".renderByItem", e);
		} finally {
			PoseStack.restore(depth);
			MultiBufferSource.INSTANCE.endBatch();
			GlStateManager.popMatrix();
		}
	}
}
