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
package io.github.xponer2.translator.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import io.github.xponer2.translator.client.MultiBufferSource;
import io.github.xponer2.translator.client.PoseStack;

/**
 * 1.21 {@code PlayerRenderer} (C row): a view of 1.12.2's player renderer of a skin, as a 1.21 living renderer the mods
 * add their layers to (the Aether's capes, darts and glow; Iron's Spells', Curios' and Mekanism's layers) and cast to
 * PlayerRenderer, with its first person arms. One per 1.12.2 renderer ({@link VanillaRendererView#of}).
 */
@SideOnly(Side.CLIENT)
public class PlayerRenderer extends VanillaRendererView<AbstractClientPlayer, ModelPlayer> {
	private final RenderPlayer player;

	PlayerRenderer(RenderPlayer player) {
		super(player);
		this.player = player;
	}

	/** The 1.12.2 renderer it shows. */
	public RenderPlayer vanilla() {
		return this.player;
	}

	/** 1.21 {@code renderRightHand(pose, buffers, light, player)}: the first person arm in the current pose (Create's and Mining Gadgets' held tools). */
	public void renderRightHand(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player) {
		MultiBufferSource.INSTANCE.endBatch();
		Minecraft.getMinecraft().getTextureManager().bindTexture(player.getLocationSkin());
		this.player.renderRightArm(player);
	}

	public void renderLeftHand(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player) {
		MultiBufferSource.INSTANCE.endBatch();
		Minecraft.getMinecraft().getTextureManager().bindTexture(player.getLocationSkin());
		this.player.renderLeftArm(player);
	}

	/** 1.21 {@code getRenderOffset(player, partialTick)}: a crouching player is drawn a little lower. */
	public Vec3d getRenderOffset(AbstractClientPlayer player, float partialTick) {
		return player.isSneaking() ? new Vec3d(0.0, -0.125, 0.0) : Vec3d.ZERO;
	}
}
