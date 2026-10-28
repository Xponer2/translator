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
package io.github.xponer2.translator.capabilities;

import io.github.xponer2.translator.registry.BlockEntityType;
import io.github.xponer2.translator.util.ItemLike;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 1.21 capability registration, posted once on the Forge event bus in init ({@link CapabilityAttacher#registerAll}).
 * The providers are attached to the tile entities and stacks created afterwards.
 */
public class RegisterCapabilitiesEvent extends Event {
	public <T, C, BE extends TileEntity> void registerBlockEntity(BlockCapability<T, C> capability, BlockEntityType<BE> type,
			ICapabilityProvider<? super BE, C, T> provider) {
		CapabilityAttacher.addBlockEntity(capability, type, provider);
	}

	public <T, C> void registerItem(ItemCapability<T, C> capability, ICapabilityProvider<ItemStack, C, T> provider, ItemLike... items) {
		for (var item : items) {
			CapabilityAttacher.addItem(capability, item.asItem(), provider);
		}
	}

	/** Item capabilities of a whole item class, for items given as 1.12.2 Items. */
	public <T, C> void registerItem(ItemCapability<T, C> capability, ICapabilityProvider<ItemStack, C, T> provider, net.minecraft.item.Item... items) {
		for (var item : items) {
			CapabilityAttacher.addItem(capability, item, provider);
		}
	}
}
