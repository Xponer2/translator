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

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * One stack somewhere that can be read and replaced (1.21 {@code SlotAccess}): the carried stack of a menu (an item
 * clicked onto another one in a slot: a backpack taking the item, a bundle), a container's slot, an equipment slot.
 */
public interface SlotAccess {
	SlotAccess NULL = new SlotAccess() {
		@Override
		public ItemStack get() {
			return ItemStack.EMPTY;
		}

		@Override
		public boolean set(ItemStack stack) {
			return false;
		}
	};

	ItemStack get();

	boolean set(ItemStack stack);

	static SlotAccess of(Supplier<ItemStack> getter, Consumer<ItemStack> setter) {
		return new SlotAccess() {
			@Override
			public ItemStack get() {
				return getter.get();
			}

			@Override
			public boolean set(ItemStack stack) {
				setter.accept(stack);
				return true;
			}
		};
	}

	static SlotAccess forContainer(IInventory container, int slot, Predicate<ItemStack> filter) {
		return new SlotAccess() {
			@Override
			public ItemStack get() {
				return container.getStackInSlot(slot);
			}

			@Override
			public boolean set(ItemStack stack) {
				if (!filter.test(stack)) {
					return false;
				}
				container.setInventorySlotContents(slot, stack);
				return true;
			}
		};
	}

	static SlotAccess forContainer(IInventory container, int slot) {
		return forContainer(container, slot, stack -> true);
	}

	static SlotAccess forEquipmentSlot(EntityLivingBase entity, EntityEquipmentSlot slot, Predicate<ItemStack> filter) {
		return new SlotAccess() {
			@Override
			public ItemStack get() {
				return entity.getItemStackFromSlot(slot);
			}

			@Override
			public boolean set(ItemStack stack) {
				if (!filter.test(stack)) {
					return false;
				}
				entity.setItemStackToSlot(slot, stack);
				return true;
			}
		};
	}

	static SlotAccess forEquipmentSlot(EntityLivingBase entity, EntityEquipmentSlot slot) {
		return forEquipmentSlot(entity, slot, stack -> true);
	}
}
