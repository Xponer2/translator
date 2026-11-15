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

import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Which 1.12.2 client events the 1.21 mods' listeners get (ForgeBus asks for the client ones). */
@SideOnly(Side.CLIENT)
public final class ClientEventFilters {
	private ClientEventFilters() {
	}

	/**
	 * DrawBlockHighlightEvent as 1.21's RenderHighlightEvent.Block: only when the cursor is on a block (1.12.2 posts it
	 * for an entity or nothing too, with no block position).
	 */
	public static boolean blockHighlight(Event event) {
		var target = ((DrawBlockHighlightEvent) event).getTarget();
		return target != null && target.typeOfHit == net.minecraft.util.math.RayTraceResult.Type.BLOCK && target.getBlockPos() != null;
	}

	/**
	 * RenderGameOverlayEvent.Pre/Post as 1.21's RenderGuiEvent.Pre/Post: once a frame, for the whole HUD. 1.12.2 posts
	 * them for every HUD element too (health, food, chat...): JourneyMap drew its minimap a dozen times a frame, and
	 * one of them, for the chat's own event, left the chat out of the screen to the left. A listener for a subclass
	 * (Text: 1.21's debug text event) gets its own events as before.
	 */
	public static boolean wholeHud(Class<?> listenerType, Event event) {
		if (listenerType != net.minecraftforge.client.event.RenderGameOverlayEvent.Pre.class
				&& listenerType != net.minecraftforge.client.event.RenderGameOverlayEvent.Post.class
				&& listenerType != net.minecraftforge.client.event.RenderGameOverlayEvent.class) {
			return true;
		}
		return event instanceof net.minecraftforge.client.event.RenderGameOverlayEvent hud
				&& hud.getType() == net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL
				&& (event.getClass() == net.minecraftforge.client.event.RenderGameOverlayEvent.Pre.class
						|| event.getClass() == net.minecraftforge.client.event.RenderGameOverlayEvent.Post.class);
	}
}
