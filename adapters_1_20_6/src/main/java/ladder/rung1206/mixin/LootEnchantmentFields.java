/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithLootingCondition;

/**
 * 1.21's enchanted count increase function and random chance with enchanted bonus condition name the enchantment they
 * count; 1.20.6's (their looting twins) always count looting. The field is added to each (a mod's mixin shadows it).
 */
final class LootEnchantmentFields {
    private LootEnchantmentFields() {
    }

    @Mixin(value = LootingEnchantFunction.class, priority = 500)
    abstract static class CountIncrease {
        private final Holder<Enchantment> enchantment = BuiltInRegistries.ENCHANTMENT.wrapAsHolder(Enchantments.LOOTING);
    }

    @Mixin(value = LootItemRandomChanceWithLootingCondition.class, priority = 500)
    abstract static class RandomChance {
        private final Holder<Enchantment> enchantment = BuiltInRegistries.ENCHANTMENT.wrapAsHolder(Enchantments.LOOTING);
    }
}
