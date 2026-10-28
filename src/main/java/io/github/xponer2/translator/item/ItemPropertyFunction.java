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

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * An item model property (1.21 {@code ItemPropertyFunction}); the client registers it as a 1.12.2
 * {@code IItemPropertyGetter} (which has no seed argument) with {@code Item#addPropertyOverride}.
 */
@FunctionalInterface
public interface ItemPropertyFunction {
    float call(ItemStack stack, @Nullable World level, @Nullable EntityLivingBase entity, int seed);
}
