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
}
