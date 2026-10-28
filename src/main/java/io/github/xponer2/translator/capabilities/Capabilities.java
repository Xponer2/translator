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

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * NeoForge's standard capabilities on the Forge 1.12.2 ones (read when used: Forge injects them in pre-init).
 */
public final class Capabilities {
	private Capabilities() {
	}

	public static final class ItemHandler {
		public static final BlockCapability<IItemHandler, EnumFacing> BLOCK = new BlockCapability<>(() -> CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
		public static final ItemCapability<IItemHandler, Void> ITEM = new ItemCapability<>(() -> CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
		public static final ItemCapability<IItemHandler, Void> ENTITY = ITEM;

		private ItemHandler() {
		}
	}

	public static final class EnergyStorage {
		public static final BlockCapability<IEnergyStorage, EnumFacing> BLOCK = new BlockCapability<>(() -> CapabilityEnergy.ENERGY);
		public static final ItemCapability<IEnergyStorage, Void> ITEM = new ItemCapability<>(() -> CapabilityEnergy.ENERGY);

		private EnergyStorage() {
		}
	}

	public static final class FluidHandler {
		public static final BlockCapability<IFluidHandler, EnumFacing> BLOCK = new BlockCapability<>(() -> CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
		public static final ItemCapability<IFluidHandlerItem, Void> ITEM = new ItemCapability<>(() -> CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY);

		private FluidHandler() {
		}
	}

	/** 1.21 level.getCapability(capability, pos, context). */
	@Nullable
	public static <T, C> T get(IBlockAccess level, BlockCapability<T, C> capability, BlockPos pos, @Nullable C context) {
		return capability.getCapability(level, pos, context);
	}

	/** 1.21 stack.getCapability(capability). */
	@Nullable
	public static <T, C> T get(ItemStack stack, ItemCapability<T, C> capability) {
		return capability.getCapability(stack, null);
	}
}
