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
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * NeoForge {@code ItemEntityPickupEvent}: {@code Pre} posted for every 1.12.2 Forge pickup event (a player touching
 * an item; a backpack's pickup upgrade takes the item), its answer written back (FALSE: not picked up, TRUE: picked up
 * as 1.12.2's ALLOW); {@code Post} for 1.12.2's item pickup event after it. {@code EventHooks.fireItemPickupPre} of a
 * mod asks the same way.
 */
public class ItemEntityPickupEvent {
	private ItemEntityPickupEvent() {
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new Forwarder());
	}

	public static Pre fire(EntityItem item, EntityPlayer player) {
		int answer = ForgeEventFactory.onItemPickup(item, player);
		return new Pre(item, player, answer < 0 ? TriState.FALSE : answer > 0 ? TriState.TRUE : TriState.DEFAULT);
	}

	public static final class Forwarder {
		@SubscribeEvent
		public void onPickup(EntityItemPickupEvent event) {
			if (event.getItem() == null || event.getEntityPlayer() == null) {
				return;
			}
			Pre pre = new Pre(event.getItem(), event.getEntityPlayer(), TriState.DEFAULT);
			MinecraftForge.EVENT_BUS.post(pre);
			if (pre.canPickup() == TriState.FALSE) {
				event.setCanceled(true);
			} else if (pre.canPickup() == TriState.TRUE) {
				event.setResult(Event.Result.ALLOW);
			}
		}

		@SubscribeEvent
		public void onPickedUp(net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemPickupEvent event) {
			if (event.player != null && event.getOriginalEntity() != null) {
				MinecraftForge.EVENT_BUS.post(new Post(event.player, event.getOriginalEntity(), event.getStack()));
			}
		}
	}

	public static final class Pre extends Event {
		private final EntityItem item;
		private final EntityPlayer player;
		private TriState canPickup;

		/** What Forge's event bus makes one with to list its listeners. */
		public Pre() {
			this(null, null, TriState.DEFAULT);
		}

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

	public static final class Post extends Event {
		private final EntityPlayer player;
		private final EntityItem item;
		private final ItemStack original;

		public Post() {
			this(null, null, ItemStack.EMPTY);
		}

		Post(EntityPlayer player, EntityItem item, ItemStack original) {
			this.player = player;
			this.item = item;
			this.original = original == null ? ItemStack.EMPTY : original;
		}

		public EntityPlayer getPlayer() {
			return this.player;
		}

		public EntityItem getItemEntity() {
			return this.item;
		}

		/** The stack before the pickup. */
		public ItemStack getOriginalStack() {
			return this.original;
		}

		/** What is left on the ground. */
		public ItemStack getCurrentStack() {
			return this.item == null ? ItemStack.EMPTY : this.item.getItem();
		}
	}
}
