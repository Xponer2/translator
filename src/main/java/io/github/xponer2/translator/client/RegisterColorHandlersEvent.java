package io.github.xponer2.translator.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * NeoForge color handler registration (event.register(color, items/blocks)) on the 1.12.2 color handler events.
 */
@SideOnly(Side.CLIENT)
public class RegisterColorHandlersEvent extends Event {
	public static class Block extends RegisterColorHandlersEvent {
		private final BlockColors colors;

		Block(BlockColors colors) {
			this.colors = colors;
		}

		public BlockColors getBlockColors() {
			return this.colors;
		}

		public void register(IBlockColor color, net.minecraft.block.Block... blocks) {
			this.colors.registerBlockColorHandler(color, blocks);
		}
	}

	public static class Item extends RegisterColorHandlersEvent {
		private final ItemColors colors;
		private final BlockColors blockColors;

		Item(ItemColors colors, BlockColors blockColors) {
			this.colors = colors;
			this.blockColors = blockColors;
		}

		public ItemColors getItemColors() {
			return this.colors;
		}

		public BlockColors getBlockColors() {
			return this.blockColors;
		}

		public void register(IItemColor color, net.minecraft.item.Item... items) {
			this.colors.registerItemColorHandler(color, items);
		}

		public void register(IItemColor color, net.minecraft.block.Block... blocks) {
			this.colors.registerItemColorHandler(color, blocks);
		}
	}

	public static final class Forwarder {
		@SubscribeEvent
		public void onBlockColors(ColorHandlerEvent.Block event) {
			MinecraftForge.EVENT_BUS.post(new Block(event.getBlockColors()));
		}

		@SubscribeEvent
		public void onItemColors(ColorHandlerEvent.Item event) {
			MinecraftForge.EVENT_BUS.post(new Item(event.getItemColors(), event.getBlockColors()));
		}
	}
}
