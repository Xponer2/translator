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

    /** 1.21's {@code Enchantment.STREAM_CODEC}: a holder of the enchantment registry */
    public static net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Holder<Enchantment>> streamCodec() {
        return net.minecraft.network.codec.ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.ENCHANTMENT);
    }

    /** 1.21's {@code menu.getEnchantmentList(registries, stack, slot, cost)}: 1.20.6's of the enabled features (opened by the AT) */
    public static java.util.List<net.minecraft.world.item.enchantment.EnchantmentInstance> getEnchantmentList(net.minecraft.world.inventory.EnchantmentMenu menu,
            net.minecraft.core.RegistryAccess registries, net.minecraft.world.item.ItemStack stack, int slot, int cost) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return menu.getEnchantmentList(server == null ? net.minecraft.world.flag.FeatureFlags.VANILLA_SET : server.getWorldData().enabledFeatures(), stack, slot, cost);
    }

    // 1.21's vanilla enchantment providers (a data registry there; their keys, 1.20.6 enchants its mobs in code)
    private static net.minecraft.resources.ResourceKey<Object> provider(String path) {
        return net.minecraft.resources.ResourceKey.create(EnchantRegistries.ENCHANTMENT_PROVIDER_KEY, new net.minecraft.resources.ResourceLocation(path));
    }

    public static net.minecraft.resources.ResourceKey<Object> mobSpawnEquipment() {
        return provider("mob_spawn_equipment");
    }

    public static net.minecraft.resources.ResourceKey<Object> pillagerSpawnCrossbow() {
        return provider("pillager_spawn_crossbow");
    }

    public static net.minecraft.resources.ResourceKey<Object> raidPillagerPostWave3() {
        return provider("raid/pillager_post_wave_3");
    }

    public static net.minecraft.resources.ResourceKey<Object> raidPillagerPostWave5() {
        return provider("raid/pillager_post_wave_5");
    }

    public static net.minecraft.resources.ResourceKey<Object> raidVindicatorPostWave5() {
        return provider("raid/vindicator_post_wave_5");
    }

    public static net.minecraft.resources.ResourceKey<Object> endermanLootDrop() {
        return provider("enderman_loot_drop");
    }

    /** 1.21's arrow hit block effects: 1.20.6's enchantments act on a hit block in their own code */
    public static void hitBlockEnchantmentEffects(net.minecraft.world.entity.projectile.AbstractArrow arrow, net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.phys.BlockHitResult hit, net.minecraft.world.item.ItemStack weapon) {
    }
}
