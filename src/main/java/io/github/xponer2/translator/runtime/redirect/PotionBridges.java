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
package io.github.xponer2.translator.runtime.redirect;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionHelper;

/**
 * 1.21's brewing (an instance, {@code level.potionBrewing()}) on 1.12.2's PotionHelper (all static). The table maps
 * the class and renames the methods; the calls stayed instance calls of static methods and threw
 * IncompatibleClassChangeError (Iron's Spells' alchemist cauldron). X lines send them here, the receiver dropped. The
 * finer checks (potion or container ingredient) are PotionHelper's private ones: its reagent check stands for both.
 */
public final class PotionBridges {
	private PotionBridges() {
	}

	public static boolean isReagent(@Nullable Object brewing, ItemStack stack) {
		return PotionHelper.isReagent(stack);
	}

	public static boolean isTypeConversionReagent(@Nullable Object brewing, ItemStack stack) {
		return PotionHelper.isReagent(stack);
	}

	public static boolean isItemConversionReagent(@Nullable Object brewing, ItemStack stack) {
		return PotionHelper.isReagent(stack);
	}

	public static boolean hasConversions(@Nullable Object brewing, ItemStack potion, ItemStack ingredient) {
		return PotionHelper.hasConversions(potion, ingredient);
	}

	public static boolean hasTypeConversions(@Nullable Object brewing, ItemStack potion, ItemStack ingredient) {
		return PotionHelper.hasConversions(potion, ingredient);
	}

	public static boolean hasItemConversions(@Nullable Object brewing, ItemStack potion, ItemStack ingredient) {
		return PotionHelper.hasConversions(potion, ingredient);
	}

	public static ItemStack doReaction(@Nullable Object brewing, ItemStack ingredient, ItemStack potion) {
		return PotionHelper.doReaction(ingredient, potion);
	}
}
