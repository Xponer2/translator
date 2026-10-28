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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.21 tooltip event: the tooltip lines are text components. Posted on the Forge event bus for every 1.12.2
 * tooltip event (see {@link Forwarder}, registered on the client); the lines are written back as formatted text.
 */
public class ItemTooltipEvent extends PlayerEvent {
	private final ItemStack itemStack;
	private final ITooltipFlag flags;
	private final List<ITextComponent> toolTip;

	public ItemTooltipEvent(ItemStack itemStack, @Nullable EntityPlayer player, List<ITextComponent> toolTip, ITooltipFlag flags) {
		super(player);
		this.itemStack = itemStack;
		this.toolTip = toolTip;
		this.flags = flags;
	}

	public ItemStack getItemStack() {
		return this.itemStack;
	}

	public ITooltipFlag getFlags() {
		return this.flags;
	}

	public List<ITextComponent> getToolTip() {
		return this.toolTip;
	}

	@Override
	@Nullable
	public EntityPlayer getEntity() {
		return this.getEntityPlayer();
	}

	public static final class Forwarder {
		@SubscribeEvent
		public void onItemTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
			List<String> lines = event.getToolTip();
			List<ITextComponent> components = new ArrayList<>(lines.size());
			for (String line : lines) {
				components.add(new TextComponentString(line));
			}

			MinecraftForge.EVENT_BUS.post(new ItemTooltipEvent(event.getItemStack(), event.getEntityPlayer(), components, event.getFlags()));

			lines.clear();
			for (ITextComponent component : components) {
				lines.add(component.getFormattedText());
			}
		}
	}
}
