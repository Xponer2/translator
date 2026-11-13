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
package io.github.xponer2.translator.menu;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import net.minecraft.block.BlockAnvil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * 1.21 {@code AnvilMenu} (C row) for the mods that make or extend one: Mekanism's repair robit, Sophisticated
 * Backpacks' anvil upgrade, JEI's anvil prices. The tables had made it 1.12.2's anvil container, whose constructor
 * takes a world and a position, so none of them could be made. The vanilla anvil block stays 1.12.2's.
 *
 * <p>The repair itself is 1.12.2's: a hidden ContainerRepair works on this menu's input and result containers (its
 * cost, its material count, the AnvilUpdateEvent and AnvilRepairEvent, the anvil's damage).
 */
public class AnvilMenu extends ItemCombinerMenu {
	/** 1.21 {@code cost} (a DataSlot there; F row): the levels the repair takes. */
	public int maximumCost;
	/** 1.21 {@code repairItemCountCost} (F row): the repair items the repair uses. */
	public int materialCost;
	/** 1.21 {@code itemName} (F row): the new name, or null for none. */
	@Nullable
	public String repairedItemName;
	/** 1.12.2's repair on this menu's containers; null until the constructor has run. */
	@Nullable
	private ContainerRepair vanilla;

	public AnvilMenu(int containerId, InventoryPlayer playerInventory) {
		this(containerId, playerInventory, ContainerLevelAccess.NULL);
	}

	public AnvilMenu(int containerId, InventoryPlayer playerInventory, ContainerLevelAccess access) {
		super(MenuType.ANVIL, containerId, playerInventory, access);
		this.addDataSlots(new ContainerData() {
			@Override
			public int get(int index) {
				return AnvilMenu.this.maximumCost;
			}

			@Override
			public void set(int index, int value) {
				AnvilMenu.this.maximumCost = value;
			}

			@Override
			public int getCount() {
				return 1;
			}
		});
		EntityPlayer player = playerInventory.player;
		World level = access.evaluate((world, pos) -> world, player.world);
		BlockPos pos = access.evaluate((world, at) -> at, player.getPosition());
		this.vanilla = repairOn(new ContainerRepair(playerInventory, level, pos, player), this);
	}

	/** 1.12.2's anvil container working on the menu's input and result containers instead of its own. */
	@Nullable
	private static ContainerRepair repairOn(ContainerRepair repair, AnvilMenu menu) {
		try {
			set(repair, "field_82853_g", menu.inputSlots);
			set(repair, "field_82852_f", menu.resultSlots);
			return repair;
		} catch (ReflectiveOperationException | RuntimeException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("AnvilMenu: 1.12.2's anvil repair", e);
			return null;
		}
	}

	private static void set(ContainerRepair repair, String srgName, Object value) throws ReflectiveOperationException {
		Field field = field(srgName);
		if (field == null) {
			throw new NoSuchFieldException(srgName);
		}
		field.set(repair, value);
	}

	@Override
	protected ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
		return ItemCombinerMenuSlotDefinition.create()
				.withSlot(0, 27, 47, stack -> true)
				.withSlot(1, 76, 47, stack -> true)
				.withResultSlot(2, 134, 47)
				.build();
	}

	@Override
	protected boolean isValidBlock(IBlockState state) {
		return state.getBlock() instanceof BlockAnvil;
	}

	/** The result can be taken with the levels it costs (or in creative), and only if it costs some. */
	@Override
	protected boolean mayPickup(EntityPlayer player, boolean hasStack) {
		return (player.capabilities.isCreativeMode || player.experienceLevel >= this.maximumCost) && this.maximumCost > 0;
	}

	/** Taking the result: the levels, the inputs and maybe the anvil, as 1.12.2's anvil takes them. */
	@Override
	protected void onTake(EntityPlayer player, ItemStack stack) {
		ContainerRepair repair = this.vanilla;
		if (repair == null) {
			return;
		}
		this.toVanilla(repair);
		repair.getSlot(2).onTake(player, stack);
		this.fromVanilla(repair);
	}

	/** 1.21 {@code createResult()}: the repaired, combined or renamed item and its cost. */
	@Override
	public void updateRepairOutput() {
		ContainerRepair repair = this.vanilla;
		if (repair == null) {
			return;
		}
		this.toVanilla(repair);
		repair.updateRepairOutput();
		this.fromVanilla(repair);
		this.detectAndSendChanges();
	}

	private void toVanilla(ContainerRepair repair) {
		repair.maximumCost = this.maximumCost;
		repair.materialCost = this.materialCost;
		if (REPAIRED_ITEM_NAME != null) {
			try {
				REPAIRED_ITEM_NAME.set(repair, this.repairedItemName == null ? "" : this.repairedItemName);
			} catch (IllegalAccessException e) {
				// the name stays 1.12.2's
			}
		}
	}

	/** 1.12.2's private name field of the anvil. */
	@Nullable
	private static final Field REPAIRED_ITEM_NAME = field("field_82857_m");

	@Nullable
	private static Field field(String srgName) {
		try {
			Field field = ObfuscationReflectionHelper.findField(ContainerRepair.class, srgName);
			field.setAccessible(true);
			return field;
		} catch (RuntimeException e) {
			return null;
		}
	}

	private void fromVanilla(ContainerRepair repair) {
		this.maximumCost = repair.maximumCost;
		this.materialCost = repair.materialCost;
	}

	/**
	 * 1.21 {@code setItemName(name)}: the new name of the item (blank takes a custom name away); false for a name too
	 * long.
	 */
	public boolean updateItemName(@Nullable String name) {
		String valid = name == null ? null : ChatAllowedCharacters.filterAllowedCharacters(name);
		if (valid == null || valid.length() > 50) {
			return false;
		}
		this.repairedItemName = valid;
		Slot result = this.getSlot(2);
		if (result.getHasStack()) {
			ItemStack stack = result.getStack();
			if (StringUtils.isBlank(valid)) {
				stack.clearCustomName();
			} else {
				stack.setStackDisplayName(valid);
			}
		}
		this.updateRepairOutput();
		return true;
	}

	/** 1.21 {@code getCost()}. */
	public int getCost() {
		return this.maximumCost;
	}

	/** NeoForge {@code setMaximumCost(cost)}. */
	public void setMaximumCost(int cost) {
		this.maximumCost = cost;
	}

	/** NeoForge 21.1 {@code setMaximumCost(long)} (its anvil hooks give a long cost). */
	public void setMaximumCost(long cost) {
		this.maximumCost = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, cost));
	}

	/** 1.21 {@code calculateIncreasedRepairCost(cost)}: the prior work penalty after one more use. */
	public static int calculateIncreasedRepairCost(int cost) {
		return cost * 2 + 1;
	}
}
