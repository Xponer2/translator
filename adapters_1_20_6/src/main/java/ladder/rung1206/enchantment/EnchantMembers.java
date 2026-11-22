/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.enchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * 1.21's members of an enchantment (a data record there) on 1.20.6's enchantment classes: the X and F rows' adapters.
 * The definition is 1.20.6's own (the access transformer opens its field).
 */
public final class EnchantMembers {
    private EnchantMembers() {
    }

    /** 1.21's {@code Enchantment.CODEC}: by name, 1.20.6's enchantments being a built-in registry */
    public static Codec<Enchantment> codec() {
        return BuiltInRegistries.ENCHANTMENT.byNameCodec();
    }

    public static Enchantment.EnchantmentDefinition definition(Enchantment enchantment) {
        return enchantment.definition;
    }

    /** {@code exclusiveSet()}: the enchantments it can't be with (1.20.6's compatibility check) */
    public static HolderSet<Enchantment> exclusiveSet(Enchantment enchantment) {
        List<Holder<Enchantment>> out = new ArrayList<>();
        for (Enchantment other : BuiltInRegistries.ENCHANTMENT) {
            if (other != enchantment && !enchantment.isCompatibleWith(other)) {
                out.add(BuiltInRegistries.ENCHANTMENT.wrapAsHolder(other));
            }
        }
        return HolderSet.direct(out);
    }

    public static boolean matchingSlot(Enchantment enchantment, EquipmentSlot slot) {
        return Arrays.asList(enchantment.definition.slots()).contains(slot);
    }

    /** {@code getSupportedItems()}: the items of 1.20.6's supported item tag */
    public static HolderSet<Item> getSupportedItems(Enchantment enchantment) {
        return BuiltInRegistries.ITEM.getOrCreateTag(enchantment.getSupportedItems());
    }

    /** {@code runLocationChangedEffects}: 1.20.6's enchantments act on a move in their own code (frost walker, soul speed) */
    public static void runLocationChangedEffects(Enchantment enchantment, ServerLevel level, int enchantmentLevel,
            ladder.carried_1_20_6.net.minecraft.world.item.enchantment.EnchantedItemInUse item, LivingEntity entity) {
    }

    /** {@code modifyUnfilteredValue}: 1.20.6's enchantments change their values in their own code */
    public static void modifyUnfilteredValue(Enchantment enchantment, DataComponentType<?> type, RandomSource random, int level,
            org.apache.commons.lang3.mutable.MutableFloat value) {
    }

    /** 1.21's {@code EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT} (the adapters' data gives 1.20.6 the tag) */
    public static net.minecraft.tags.TagKey<Enchantment> onMobSpawnEquipment() {
        return net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,
                new net.minecraft.resources.ResourceLocation("minecraft", "on_mob_spawn_equipment"));
    }
}
