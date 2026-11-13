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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

/**
 * 1.21 {@code ItemCombinerMenuSlotDefinition} (C row): where an anvil-like menu (an ItemCombinerMenu) puts its input
 * slots and its result slot, and what each input slot takes.
 */
public class ItemCombinerMenuSlotDefinition {
	private final List<SlotDefinition> slots;
	private final SlotDefinition resultSlot;

	ItemCombinerMenuSlotDefinition(List<SlotDefinition> slots, SlotDefinition resultSlot) {
		this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
		this.resultSlot = resultSlot;
	}

	public static Builder create() {
		return new Builder();
	}

	public SlotDefinition getSlot(int index) {
		return this.slots.get(index);
	}

	public SlotDefinition getResultSlot() {
		return this.resultSlot;
	}

	public List<SlotDefinition> getSlots() {
		return this.slots;
	}

	public int getNumOfInputSlots() {
		return this.slots.size();
	}

	/** The result slot comes right after the input slots. */
	public int getResultSlotIndex() {
		return this.getNumOfInputSlots();
	}

	public List<Integer> getInputSlotIndexes() {
		List<Integer> indexes = new ArrayList<>(this.slots.size());
		for (SlotDefinition slot : this.slots) {
			indexes.add(slot.slotIndex());
		}
		return indexes;
	}

	/** 1.21 {@code ItemCombinerMenuSlotDefinition.Builder}. */
	public static class Builder {
		private final List<SlotDefinition> slots = new ArrayList<>();
		private SlotDefinition resultSlot = SlotDefinition.EMPTY;

		public Builder withSlot(int slotIndex, int x, int y, Predicate<ItemStack> mayPlace) {
			this.slots.add(new SlotDefinition(slotIndex, x, y, mayPlace));
			return this;
		}

		public Builder withResultSlot(int slotIndex, int x, int y) {
			this.resultSlot = new SlotDefinition(slotIndex, x, y, stack -> false);
			return this;
		}

		/** The definition; the input slots are numbered from 0 and the result slot follows them, as 1.21 checks. */
		public ItemCombinerMenuSlotDefinition build() {
			for (int i = 0; i < this.slots.size(); i++) {
				if (this.slots.get(i).slotIndex() != i) {
					throw new IllegalArgumentException("Expected input slots to have continous indexes");
				}
			}
			if (this.resultSlot.slotIndex() != this.slots.size()) {
				throw new IllegalArgumentException("Expected result slot index to follow last input slot");
			}
			return new ItemCombinerMenuSlotDefinition(this.slots, this.resultSlot);
		}
	}

	/** 1.21 record {@code SlotDefinition(slotIndex, x, y, mayPlace)}. */
	public static final class SlotDefinition {
		static final SlotDefinition EMPTY = new SlotDefinition(0, 0, 0, stack -> true);

		private final int slotIndex;
		private final int x;
		private final int y;
		private final Predicate<ItemStack> mayPlace;

		public SlotDefinition(int slotIndex, int x, int y, Predicate<ItemStack> mayPlace) {
			this.slotIndex = slotIndex;
			this.x = x;
			this.y = y;
			this.mayPlace = mayPlace;
		}

		public int slotIndex() {
			return this.slotIndex;
		}

		public int x() {
			return this.x;
		}

		public int y() {
			return this.y;
		}

		public Predicate<ItemStack> mayPlace() {
			return this.mayPlace;
		}
	}
}
