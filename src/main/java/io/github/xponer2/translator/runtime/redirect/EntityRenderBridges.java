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
package io.github.xponer2.translator.runtime.redirect;

import net.minecraft.entity.Entity;

/**
 * Static stand-ins for 1.21 entity calls the renderers of the mods make every frame (neoforge-1.21.1-redirects.tsv): a
 * missing one was left empty and logged at each call (GeckoLib's name tag check, half a million times in a test).
 */
public final class EntityRenderBridges {
	private EntityRenderBridges() {
	}

	/** 1.21 {@code entity.isDiscrete()}: sneaking (a sneaking player's name tag is drawn closer and dimmer). */
	public static boolean isDiscrete(Entity entity) {
		return entity.isSneaking();
	}

	// the vanilla renderers the mods register for their own entities (1.21 constructors from the renderer context)

	public static net.minecraft.client.renderer.entity.RenderFallingBlock newRenderFallingBlock(io.github.xponer2.translator.client.entity.RendererContext context) {
		return new net.minecraft.client.renderer.entity.RenderFallingBlock(context.getEntityRenderDispatcher());
	}

	public static net.minecraft.client.renderer.entity.RenderTippedArrow newRenderTippedArrow(io.github.xponer2.translator.client.entity.RendererContext context) {
		return new net.minecraft.client.renderer.entity.RenderTippedArrow(context.getEntityRenderDispatcher());
	}

	public static net.minecraft.client.renderer.entity.RenderLightningBolt newRenderLightningBolt(io.github.xponer2.translator.client.entity.RendererContext context) {
		return new net.minecraft.client.renderer.entity.RenderLightningBolt(context.getEntityRenderDispatcher());
	}

	public static net.minecraft.client.renderer.entity.RenderEvokerFangs newRenderEvokerFangs(io.github.xponer2.translator.client.entity.RendererContext context) {
		return new net.minecraft.client.renderer.entity.RenderEvokerFangs(context.getEntityRenderDispatcher());
	}

	/** 1.21 {@code new MinecartRenderer(context, layer)}: 1.12.2's minecart (its own model). */
	public static net.minecraft.client.renderer.entity.RenderMinecart<?> newRenderMinecart(io.github.xponer2.translator.client.entity.RendererContext context,
			Object layer) {
		return new net.minecraft.client.renderer.entity.RenderMinecart<>(context.getEntityRenderDispatcher());
	}

	/**
	 * 1.21 {@code playerRenderer.renderRightHand(pose, buffers, light, player)}: the first person arm in the current
	 * pose (Create's and Mining Gadgets' held tools), 1.12.2's arm drawing with the player's skin.
	 */
	public static void renderRightHand(net.minecraft.client.renderer.entity.RenderPlayer renderer, Object pose, Object buffers, int light,
			net.minecraft.client.entity.AbstractClientPlayer player) {
		io.github.xponer2.translator.client.MultiBufferSource.INSTANCE.endBatch();
		net.minecraft.client.Minecraft.getMinecraft().getTextureManager().bindTexture(player.getLocationSkin());
		renderer.renderRightArm(player);
	}

	public static void renderLeftHand(net.minecraft.client.renderer.entity.RenderPlayer renderer, Object pose, Object buffers, int light,
			net.minecraft.client.entity.AbstractClientPlayer player) {
		io.github.xponer2.translator.client.MultiBufferSource.INSTANCE.endBatch();
		net.minecraft.client.Minecraft.getMinecraft().getTextureManager().bindTexture(player.getLocationSkin());
		renderer.renderLeftArm(player);
	}

	/** 1.21 {@code playerRenderer.getRenderOffset(player, partialTick)}: a crouching player is drawn a little lower. */
	public static net.minecraft.util.math.Vec3d getRenderOffset(net.minecraft.client.renderer.entity.RenderPlayer renderer,
			net.minecraft.client.entity.AbstractClientPlayer player, float partialTick) {
		return player.isSneaking() ? new net.minecraft.util.math.Vec3d(0.0, -0.125, 0.0) : net.minecraft.util.math.Vec3d.ZERO;
	}

	/** NeoForge {@code renderPlayerEvent.getRenderer()}: the 1.21 PlayerRenderer of the event's 1.12.2 one (Create's held tools). */
	public static io.github.xponer2.translator.client.entity.PlayerRenderer getRenderer(net.minecraftforge.client.event.RenderPlayerEvent event) {
		return io.github.xponer2.translator.client.entity.VanillaRendererView.of(event.getRenderer());
	}
}
