/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import ladder.rung1206.enchantment.AddedEnchantmentHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/**
 * 1.20.6's enchanting table candidates in 1.21's shape: the registry's enabled, discoverable (and, unless allowed,
 * non-treasure) enchantments, in its order, go to 1.21's getAvailableEnchantmentResults(power, stack, possible) (a shape
 * patch, AddedEnchantmentHelper), which tests each for the stack as 1.20.6 did. The mods' points in 1.21's method see the
 * table's search.
 */
@Mixin(value = EnchantmentHelper.class, priority = 500)
abstract class EnchantmentTableMixin {
    // an overwrite, not an injector: the mods' points are in 1.21's method, which this calls
    /**
     * @author ladder
     * @reason 1.21's shape of the table's search (1.20.6's tests, in its order)
     */
    @Overwrite
    public static List<EnchantmentInstance> getAvailableEnchantmentResults(FeatureFlagSet flags, int power, ItemStack stack, boolean treasure) {
        return AddedEnchantmentHelper.getAvailableEnchantmentResults(power, stack, BuiltInRegistries.ENCHANTMENT.holders()
                .filter(holder -> holder.value().isEnabled(flags) && (!holder.value().isTreasureOnly() || treasure) && holder.value().isDiscoverable())
                .map(holder -> (Holder<Enchantment>) holder));
    }
}
