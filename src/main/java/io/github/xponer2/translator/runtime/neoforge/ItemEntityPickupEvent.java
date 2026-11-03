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
package io.github.xponer2.translator.runtime.neoforge;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * NeoForge {@code ItemEntityPickupEvent.Pre} as fired by {@code EventHooks.fireItemPickupPre}: answered by the 1.12.2
 * Forge pickup event (cancelled: false, allowed: true, else the default).
 */
public class ItemEntityPickupEvent {
	private ItemEntityPickupEvent() {
	}

	public static Pre fire(EntityItem item, EntityPlayer player) {
		int answer = ForgeEventFactory.onItemPickup(item, player);
		return new Pre(item, player, answer < 0 ? TriState.FALSE : answer > 0 ? TriState.TRUE : TriState.DEFAULT);
	}

	public static final class Pre {
		private final EntityItem item;
		private final EntityPlayer player;
		private TriState canPickup;

		Pre(EntityItem item, EntityPlayer player, TriState canPickup) {
			this.item = item;
			this.player = player;
			this.canPickup = canPickup;
		}

		public EntityItem getItemEntity() {
			return this.item;
		}

		public EntityPlayer getPlayer() {
			return this.player;
		}

		public TriState canPickup() {
			return this.canPickup;
		}

		public void setCanPickup(TriState canPickup) {
			this.canPickup = canPickup;
		}
	}
}
