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
package io.github.xponer2.translator.block;

import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;

/**
 * 1.21 {@code DispenseItemBehavior} (neoforge-1.21.1-runtime.tsv): a 1.12.2 dispenser behavior whose method takes the
 * 1.21 source. A mod's behavior (a class or a lambda) is dispensed by the game through the 1.12.2 method.
 */
@FunctionalInterface
public interface DispenseItemBehavior extends IBehaviorDispenseItem {
	ItemStack dispense(BlockSource source, ItemStack stack);

	@Override
	default ItemStack dispense(IBlockSource source, ItemStack stack) {
		return this.dispense(new BlockSource(source), stack);
	}

	/** A 1.12.2 behavior (the dispenser registry's, ArmorItem's) as a 1.21 one. */
	static DispenseItemBehavior of(IBehaviorDispenseItem behavior) {
		return behavior instanceof DispenseItemBehavior own ? own : (source, stack) -> behavior.dispense(source.source(), stack);
	}
}
