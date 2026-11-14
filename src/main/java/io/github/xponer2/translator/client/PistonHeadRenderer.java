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

import net.minecraft.client.renderer.tileentity.TileEntityPistonRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code PistonHeadRenderer} (S row) for the renderers that extend it (Supplementaries' moving
 * pulley block): the 1.21 constructor from a renderer context, and its render of the moved block by 1.12.2's piston
 * renderer. 1.12.2's has no such constructor: the renderer wasn't made and the block entity's renderer failed.
 */
@SideOnly(Side.CLIENT)
public class PistonHeadRenderer implements BlockEntityRenderer<TileEntityPiston> {
	private final TileEntityPistonRenderer vanilla = new TileEntityPistonRenderer();

	public PistonHeadRenderer(BlockEntityRendererProvider.Context context) {
		this.vanilla.setRendererDispatcher(TileEntityRendererDispatcher.instance);
	}

	/** The moved block at its offset: the GL matrix is at the block entity's position already. */
	@Override
	public void render(TileEntityPiston piston, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		MultiBufferSource.INSTANCE.endBatch();
		this.vanilla.render(piston, 0.0D, 0.0D, 0.0D, partialTick, -1, 1.0F);
	}
}
