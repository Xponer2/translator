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
}
