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
package io.github.xponer2.translator.item;

import javax.annotation.Nullable;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 1.21 {@code ClampedItemPropertyFunction}: a model property whose value is clamped to 0 to 1; implementations give
 * {@code unclampedCall}. The tables mapped it to ItemPropertyFunction itself, where {@code call} is abstract: a clamped
 * property (the compass's, Iron's Spells' and many mods' lambdas) had no call at all.
 */
@FunctionalInterface
public interface ClampedItemPropertyFunction extends ItemPropertyFunction {
	@Override
	default float call(ItemStack stack, @Nullable World level, @Nullable EntityLivingBase entity, int seed) {
		return Math.max(0.0F, Math.min(1.0F, this.unclampedCall(stack, level, entity, seed)));
	}

	float unclampedCall(ItemStack stack, @Nullable World level, @Nullable EntityLivingBase entity, int seed);
}
