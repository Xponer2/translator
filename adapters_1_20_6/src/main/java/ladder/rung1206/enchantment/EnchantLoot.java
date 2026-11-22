/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.enchantment;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * The loot context parameters and parameter sets 1.21's enchantment effects check their conditions with, and the codec
 * of a component map of other types (1.21's DataComponentMap.makeCodec): the F and X rows' adapters.
 */
public final class EnchantLoot {
    public static final LootContextParam<Integer> ENCHANTMENT_LEVEL = new LootContextParam<>(new ResourceLocation("enchantment_level"));
    public static final LootContextParam<Boolean> ENCHANTMENT_ACTIVE = new LootContextParam<>(new ResourceLocation("enchantment_active"));

    public static final LootContextParamSet ENCHANTED_DAMAGE = LootContextParamSet.builder().required(LootContextParams.THIS_ENTITY)
            .required(ENCHANTMENT_LEVEL).required(LootContextParams.ORIGIN).required(LootContextParams.DAMAGE_SOURCE)
            .optional(LootContextParams.DIRECT_KILLER_ENTITY).optional(LootContextParams.KILLER_ENTITY).build();
    public static final LootContextParamSet ENCHANTED_ITEM = LootContextParamSet.builder().required(LootContextParams.TOOL).required(ENCHANTMENT_LEVEL)
            .build();
    public static final LootContextParamSet ENCHANTED_LOCATION = LootContextParamSet.builder().required(LootContextParams.THIS_ENTITY)
            .required(ENCHANTMENT_LEVEL).required(LootContextParams.ORIGIN).required(ENCHANTMENT_ACTIVE).build();
    public static final LootContextParamSet ENCHANTED_ENTITY = LootContextParamSet.builder().required(LootContextParams.THIS_ENTITY)
            .required(ENCHANTMENT_LEVEL).required(LootContextParams.ORIGIN).build();
    public static final LootContextParamSet HIT_BLOCK = LootContextParamSet.builder().required(LootContextParams.THIS_ENTITY)
            .required(ENCHANTMENT_LEVEL).required(LootContextParams.ORIGIN).required(LootContextParams.BLOCK_STATE).build();

    private EnchantLoot() {
    }

    public static LootContextParam<Integer> enchantmentLevel() {
        return ENCHANTMENT_LEVEL;
    }

    public static LootContextParam<Boolean> enchantmentActive() {
        return ENCHANTMENT_ACTIVE;
    }

    public static LootContextParamSet enchantedDamage() {
        return ENCHANTED_DAMAGE;
    }

    public static LootContextParamSet enchantedItem() {
        return ENCHANTED_ITEM;
    }

    public static LootContextParamSet enchantedLocation() {
        return ENCHANTED_LOCATION;
    }

    public static LootContextParamSet enchantedEntity() {
        return ENCHANTED_ENTITY;
    }

    public static LootContextParamSet hitBlock() {
        return HIT_BLOCK;
    }

    /** 1.21's {@code DataComponentMap.makeCodec(typeCodec)}: a map of components of the types the codec names */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Codec<DataComponentMap> makeCodec(Codec<DataComponentType<?>> typeCodec) {
        Codec<Map<DataComponentType<?>, Object>> map = Codec.dispatchedMap(typeCodec, type -> (Codec<Object>) type.codecOrThrow());
        return map.xmap(m -> {
            DataComponentMap.Builder builder = DataComponentMap.builder();
            m.forEach((type, value) -> builder.set((DataComponentType) type, value));
            return builder.build();
        }, components -> {
            Map<DataComponentType<?>, Object> out = new HashMap<>();
            components.forEach(typed -> out.put(typed.type(), typed.value()));
            return out;
        });
    }

    // 1.21's loot builders of an enchantment (1.20.6's count looting only, and take plain numbers)

    /** {@code EnchantedCountIncreaseFunction.lootingMultiplier(registries, count)} */
    public static net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction.Builder lootingMultiplier(
            net.minecraft.core.HolderLookup.Provider registries, net.minecraft.world.level.storage.loot.providers.number.NumberProvider count) {
        return net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction.lootingMultiplier(count);
    }

    /** {@code new EnchantedCountIncreaseFunction.Builder(enchantment, count)} */
    public static net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction.Builder countIncrease(
            net.minecraft.core.Holder<?> enchantment, net.minecraft.world.level.storage.loot.providers.number.NumberProvider count) {
        return new net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction.Builder(count);
    }

    /** {@code LootItemRandomChanceCondition.randomChance(chance)}: a constant one's value (another: its average, a half) */
    public static net.minecraft.world.level.storage.loot.predicates.LootItemCondition.Builder randomChance(
            net.minecraft.world.level.storage.loot.providers.number.NumberProvider chance) {
        float value = chance instanceof net.minecraft.world.level.storage.loot.providers.number.ConstantValue constant ? constant.value() : 0.5F;
        return net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition.randomChance(value);
    }

    /** {@code EnchantRandomlyFunction.Builder.withOneOf(set)}: each enchantment of the set */
    public static net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction.Builder withOneOf(
            net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction.Builder builder,
            net.minecraft.core.HolderSet<net.minecraft.world.item.enchantment.Enchantment> set) {
        for (var holder : set) {
            builder.withEnchantment(holder.value());
        }
        return builder;
    }

    /** {@code new LootItemRandomChanceWithEnchantedBonusCondition(chance, bonus, enchantment)}: 1.20.6's looting one */
    public static net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithLootingCondition chanceWithBonus(float unenchanted,
            ladder.carried_1_20_6.net.minecraft.world.item.enchantment.LevelBasedValue bonus, net.minecraft.core.Holder<?> enchantment) {
        float one = bonus.calculate(1);
        float two = bonus.calculate(2);
        return new net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithLootingCondition(one, two - one);
    }
}
