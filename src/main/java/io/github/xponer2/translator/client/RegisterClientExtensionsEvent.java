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

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * NeoForge client extension registration (items), posted by the Translator on the client in init; {@link HandRenderer}
 * applies the hand transforms.
 */
@SideOnly(Side.CLIENT)
public class RegisterClientExtensionsEvent extends Event {
	private static final Map<Item, IClientItemExtensions> ITEMS = new IdentityHashMap<>();

	public void registerItem(IClientItemExtensions extensions, Item... items) {
		for (var item : items) {
			ITEMS.put(item, extensions);
		}
	}

	public static final class HandRenderer {
		@SubscribeEvent
		public void onRenderHand(RenderSpecificHandEvent event) {
			var stack = event.getItemStack();
			var extensions = stack.isEmpty() ? null : ITEMS.get(stack.getItem());
			var player = Minecraft.getMinecraft().player;
			if (extensions == null || player == null) {
				return;
			}

			var arm = event.getHand() == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
			extensions.applyHandTransform(player, arm == null ? EnumHandSide.RIGHT : arm, stack, event.getPartialTicks(),
					event.getEquipProgress(), event.getSwingProgress());
		}
	}
}
