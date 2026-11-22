/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.misc;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * 1.21's enchantment effects and tags on 1.20.6's enchantment classes: the curses (binding, vanishing), the effects of
 * an attack (1.20.6's post hurt and post damage hooks, the damage bonus) and an enchantment tag on a stack.
 */
public final class EnchantmentEffects {
    private static final TagKey<Enchantment> CURSE = TagKey.create(Registries.ENCHANTMENT, new ResourceLocation("minecraft", "curse"));
    private static final TagKey<Enchantment> TREASURE = TagKey.create(Registries.ENCHANTMENT, new ResourceLocation("minecraft", "treasure"));

    private EnchantmentEffects() {
    }

    /** 1.21's {@code EnchantmentHelper.has(stack, effect)}: binding keeps armour on, vanishing drops nothing */
    public static boolean has(ItemStack stack, net.minecraft.core.component.DataComponentType<?> effect) {
        // the effect types are 1.21's own (carried, in the adapters' registry): by their ids
        net.minecraft.resources.ResourceLocation id = ladder.rung1206.enchantment.EnchantRegistries.enchantmentEffectComponentType() == null ? null
                : ladder.rung1206.enchantment.EnchantRegistries.enchantmentEffectComponentType().getKey(effect);
        if (id == null) {
            return false;
        }
        if (id.getPath().equals("prevent_armor_change") && EnchantmentHelper.hasBindingCurse(stack)) {
            return true;
        }
        if (id.getPath().equals("prevent_equipment_drop") && EnchantmentHelper.hasVanishingCurse(stack)) {
            return true;
        }
        // a mod's data enchantment with that effect
        for (var entry : stack.getEnchantments().entrySet()) {
            com.google.gson.JsonObject effects = ladder.rung1206.enchantment.DataEnchantments.EFFECTS.get(entry.getKey().value());
            if (effects != null && (effects.has(id.toString()) || id.getNamespace().equals("minecraft") && effects.has(id.getPath()))) {
                return true;
            }
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

    /**
     * 1.21's {@code doPostAttackEffects(level, target, source)}: the target's enchantments react to the attacker (thorns),
     * the attacker's to the target (bane of arthropods), as 1.20.6 runs them after a hit
     */
    public static void doPostAttackEffects(ServerLevel level, Entity target, DamageSource source) {
        Entity attacker = source.getEntity();
        if (target instanceof LivingEntity living) {
            EnchantmentHelper.doPostHurtEffects(living, attacker);
        }
        if (attacker instanceof LivingEntity living) {
            EnchantmentHelper.doPostDamageEffects(living, target);
        }
    }

    /** 1.21's {@code doPostAttackEffectsWithItemSource}: a projectile's hit with the enchantments of the item that shot it */
    public static void doPostAttackEffectsWithItemSource(ServerLevel level, Entity target, DamageSource source, ItemStack weapon) {
        Entity attacker = source.getEntity();
        if (target instanceof LivingEntity living) {
            EnchantmentHelper.doPostHurtEffects(living, attacker);
        }
        if (attacker instanceof LivingEntity living && weapon != null) {
            EnchantmentHelper.doPostItemStackHurtEffects(living, target, weapon.getEnchantments());
        }
    }

    /** 1.21's {@code modifyDamage(level, weapon, target, source, damage)}: 1.20.6's damage bonus of the weapon against the target */
    public static float modifyDamage(ServerLevel level, ItemStack weapon, Entity target, DamageSource source, float damage) {
        return damage + EnchantmentHelper.getDamageBonus(weapon, target.getType());
    }

    /** 1.21's {@code hasTag(stack, tag)}: an enchantment of the stack in the tag */
    public static boolean hasTag(ItemStack stack, TagKey<Enchantment> tag) {
        for (Holder<Enchantment> enchantment : stack.getEnchantments().keySet()) {
            if (enchantment.is(tag)) {
                return true;
            }
        }
        return false;
    }
}
