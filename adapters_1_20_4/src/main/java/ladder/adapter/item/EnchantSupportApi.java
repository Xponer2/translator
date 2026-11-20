/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * NeoForge 21.1's {@code IItemExtension.supportsEnchantment(stack, enchantment)}: whether an enchantment may go on the
 * item (the anvil, enchant_randomly loot, /enchant). 1.20.4 asks {@code Enchantment.canEnchant(stack)} there;
 * AnvilEnchantMixin, EnchantRandomlyMixin and EnchantCommandMixin ask a converted mod item's override instead.
 */
public interface EnchantSupportApi {
    default boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return EnchantSupport.byDefault(stack, enchantment);
    }
}
