/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.item;

import java.util.UUID;

import ladder.adapter.component.DataComponents;
import ladder.adapter.component.ItemDefaults;
import ladder.carried.net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.Block;

/**
 * 1.20.5's swords and tools take their attack damage and speed as the properties' attribute modifiers (made by their
 * createAttributes); 1.20.4's take the two numbers in the constructor and make the same modifiers (the same ids, the
 * tier's damage bonus added). The K rows give 1.20.4's constructor the numbers back out of the properties' modifiers.
 * Item's base attack modifier ids are protected in 1.20.4's Item, public in 1.20.5's: the getters give them.
 */
public final class ToolAdapters {
    private static final UUID BASE_ATTACK_DAMAGE = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID BASE_ATTACK_SPEED = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    private ToolAdapters() {
    }

    public static UUID baseAttackDamage() {
        return BASE_ATTACK_DAMAGE;
    }

    public static UUID baseAttackSpeed() {
        return BASE_ATTACK_SPEED;
    }

    /** the amount of the properties' base modifier of an attribute (createAttributes's), or 0 */
    static double base(Item.Properties properties, Attribute attribute, UUID id) {
        if (ItemDefaults.pending(properties, DataComponents.ATTRIBUTE_MODIFIERS) instanceof ItemAttributeModifiers modifiers) {
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                if (entry.attribute().value() == attribute && entry.modifier().getId().equals(id)) {
                    return entry.modifier().getAmount();
                }
            }
        }
        return 0.0;
    }

    /** the damage a 1.20.4 tool constructor takes: the modifier's amount without the tier's bonus it adds back */
    static float damage(Tier tier, Item.Properties properties) {
        return (float) base(properties, Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE) - tier.getAttackDamageBonus();
    }

    static float speed(Item.Properties properties) {
        return (float) base(properties, Attributes.ATTACK_SPEED, BASE_ATTACK_SPEED);
    }

    public static int swordDamage(Tier tier, Item.Properties properties) {
        return Math.round(damage(tier, properties));
    }

    public static float swordSpeed(Tier tier, Item.Properties properties) {
        return speed(properties);
    }

    public static int pickaxeDamage(Tier tier, Item.Properties properties) {
        return Math.round(damage(tier, properties));
    }

    public static float pickaxeSpeed(Tier tier, Item.Properties properties) {
        return speed(properties);
    }

    public static int hoeDamage(Tier tier, Item.Properties properties) {
        return Math.round(damage(tier, properties));
    }

    public static float hoeSpeed(Tier tier, Item.Properties properties) {
        return speed(properties);
    }

    public static float axeDamage(Tier tier, Item.Properties properties) {
        return damage(tier, properties);
    }

    public static float axeSpeed(Tier tier, Item.Properties properties) {
        return speed(properties);
    }

    public static float shovelDamage(Tier tier, Item.Properties properties) {
        return damage(tier, properties);
    }

    public static float shovelSpeed(Tier tier, Item.Properties properties) {
        return speed(properties);
    }

    public static float diggerDamage(Tier tier, TagKey<Block> blocks, Item.Properties properties) {
        return damage(tier, properties);
    }

    public static float diggerSpeed(Tier tier, TagKey<Block> blocks, Item.Properties properties) {
        return speed(properties);
    }
}
