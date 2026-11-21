/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.misc;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** 1.21's enchantment effects and tags on 1.20.4's enchantment classes: the curses (binding, vanishing). */
public final class EnchantmentEffects {
    private static final TagKey<Enchantment> CURSE = TagKey.create(Registries.ENCHANTMENT, new ResourceLocation("minecraft", "curse"));
    private static final TagKey<Enchantment> TREASURE = TagKey.create(Registries.ENCHANTMENT, new ResourceLocation("minecraft", "treasure"));

    private EnchantmentEffects() {
    }

    /** 1.21's {@code EnchantmentHelper.has(stack, effect)}: binding keeps armour on, vanishing drops nothing */
    public static boolean has(ItemStack stack, ladder.adapter.component.DataComponentType<?> effect) {
        if (effect == ladder.adapter.provided.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) {
            return EnchantmentHelper.hasBindingCurse(stack);
        }
        if (effect == ladder.adapter.provided.EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP) {
            return EnchantmentHelper.hasVanishingCurse(stack);
        }
        return false;
    }

    /** 1.21's {@code EnchantmentTags.CURSE} (the adapters' data gives the tag below) */
    public static TagKey<Enchantment> curse() {
        return CURSE;
    }

    /** 1.21's {@code EnchantmentTags.TREASURE} */
    public static TagKey<Enchantment> treasure() {
        return TREASURE;
    }
}
