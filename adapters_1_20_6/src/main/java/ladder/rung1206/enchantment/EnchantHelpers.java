/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.gson.JsonObject;

import ladder.carried_1_20_6.net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * 1.21's EnchantmentHelper of effect components on 1.20.6's enchantments (each vanilla effect is the enchantment 1.20.6
 * has for it) and on the mods' data enchantments (their files' effects, DataEnchantments.EFFECTS): the X rows' adapters.
 * What 1.20.6 computes itself (a fishing rod's lure, a trident's loyalty) is asked of 1.20.6.
 */
public final class EnchantHelpers {
    private EnchantHelpers() {
    }

    /** the effect component type's 1.21 id (the adapters' registry of 1.21's types), or null */
    static ResourceLocation effectId(DataComponentType<?> type) {
        if (type == REPAIR_WITH_XP_STANDIN) {
            return REPAIR_WITH_XP;
        }
        var registry = EnchantRegistries.enchantmentEffectComponentType();
        return registry == null || type == null ? null : registry.getKey(type);
    }

    private static final ResourceLocation REPAIR_WITH_XP = new ResourceLocation("minecraft", "repair_with_xp");
    /** mending's effect when the pack's carried code registers no effect types (no mod named them) */
    // (1.20.6's builder wants a codec for the network: a unit one, never sent)
    private static final DataComponentType<?> REPAIR_WITH_XP_STANDIN = DataComponentType.<net.minecraft.util.Unit>builder()
            .networkSynchronized(net.minecraft.network.codec.StreamCodec.unit(net.minecraft.util.Unit.INSTANCE)).build();

    /** 1.21's {@code EnchantmentEffectComponents.REPAIR_WITH_XP}: the registered type, else a stand-in of the same id */
    public static DataComponentType<?> repairWithXp() {
        var registry = EnchantRegistries.enchantmentEffectComponentType();
        Object type = registry == null ? null : registry.get(REPAIR_WITH_XP);
        return type instanceof DataComponentType<?> found ? found : REPAIR_WITH_XP_STANDIN;
    }

    /** 1.21's getRandomItemWith test of one enchantment of a stack in a slot: it has the effect and acts in that slot */
    public static boolean randomSearchMatches(Holder<Enchantment> holder, DataComponentType<?> type, EquipmentSlot slot, LivingEntity entity) {
        Enchantment enchantment = holder.value();
        return hasEffect(enchantment, effectId(type)) && enchantment.getSlotItems(entity).containsKey(slot);
    }

    /** 1.21's {@code new EnchantedItemInUse(stack, slot, entity)} (an Object: the game's classes name no carried type) */
    public static Object inUse(ItemStack stack, EquipmentSlot slot, LivingEntity entity) {
        return new EnchantedItemInUse(stack, slot, entity);
    }

    /** the stack of a find of getRandomItemWith */
    public static ItemStack stackOf(Object inUse) {
        return ((EnchantedItemInUse) inUse).itemStack();
    }

    /** the 1.20.6 enchantment that has the vanilla effect of that id (null: none, or several) */
    private static Enchantment vanilla(String path) {
        return switch (path) {
            case "repair_with_xp" -> Enchantments.MENDING;
            case "prevent_armor_change" -> Enchantments.BINDING_CURSE;
            case "prevent_equipment_drop" -> Enchantments.VANISHING_CURSE;
            case "trident_return_acceleration" -> Enchantments.LOYALTY;
            case "trident_spin_attack_strength", "trident_sound" -> Enchantments.RIPTIDE;
            case "projectile_count", "projectile_spread" -> Enchantments.MULTISHOT;
            case "crossbow_charge_time", "crossbow_charging_sounds" -> Enchantments.QUICK_CHARGE;
            case "fishing_time_reduction" -> Enchantments.LURE;
            case "fishing_luck_bonus" -> Enchantments.LUCK_OF_THE_SEA;
            case "equipment_drops" -> Enchantments.LOOTING;
            case "projectile_piercing" -> Enchantments.PIERCING;
            case "smash_damage_per_fallen_block" -> null;
            default -> null;
        };
    }

    /** whether an enchantment has the effect (a vanilla one by its enchantment, a mod's data one by its file) */
    static boolean hasEffect(Enchantment enchantment, ResourceLocation id) {
        if (id == null) {
            return false;
        }
        if (id.getNamespace().equals("minecraft") && vanilla(id.getPath()) == enchantment) {
            return true;
        }
        JsonObject effects = DataEnchantments.EFFECTS.get(enchantment);
        return effects != null && (effects.has(id.toString()) || id.getNamespace().equals("minecraft") && effects.has(id.getPath()));
    }

    static boolean stackHas(ItemStack stack, ResourceLocation id) {
        for (var entry : stack.getEnchantments().entrySet()) {
            if (hasEffect(entry.getKey().value(), id)) {
                return true;
            }
        }
        return false;
    }

    /** the highest level on the stack of an enchantment with the effect (0: none) */
    static int level(ItemStack stack, ResourceLocation id) {
        int best = 0;
        for (var entry : stack.getEnchantments().entrySet()) {
            if (hasEffect(entry.getKey().value(), id)) {
                best = Math.max(best, entry.getIntValue());
            }
        }
        return best;
    }

    /** 1.21's {@code getRandomItemWith(effect, entity, filter)}: an equipped stack with that effect, at random */
    public static Optional<EnchantedItemInUse> getRandomItemWith(DataComponentType<?> type, LivingEntity entity, Predicate<ItemStack> filter) {
        ResourceLocation id = effectId(type);
        List<EnchantedItemInUse> found = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty() && filter.test(stack) && slotHas(stack, slot, entity, id)) {
                found.add(new EnchantedItemInUse(stack, slot, entity));
            }
        }
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(entity.getRandom().nextInt(found.size())));
    }

    private static boolean slotHas(ItemStack stack, EquipmentSlot slot, LivingEntity entity, ResourceLocation id) {
        for (var entry : stack.getEnchantments().entrySet()) {
            Enchantment enchantment = entry.getKey().value();
            if (hasEffect(enchantment, id) && enchantment.getSlotItems(entity).containsKey(slot)) {
                return true;
            }
        }
        return false;
    }

    /** 1.21's {@code runIterationOnEquipment(entity, visitor)}: each enchantment of each equipped stack in a slot it acts in */
    public static void runIterationOnEquipment(LivingEntity entity,
            ladder.carried_1_20_6.net.minecraft.world.item.enchantment.EnchantmentHelper$EnchantmentInSlotVisitor visitor) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            runIterationOnItem(entity.getItemBySlot(slot), slot, entity, visitor);
        }
    }

    /** 1.21's {@code runIterationOnItem(stack, slot, entity, visitor)} */
    public static void runIterationOnItem(ItemStack stack, EquipmentSlot slot, LivingEntity entity,
            ladder.carried_1_20_6.net.minecraft.world.item.enchantment.EnchantmentHelper$EnchantmentInSlotVisitor visitor) {
        if (stack.isEmpty()) {
            return;
        }
        for (var entry : stack.getEnchantments().entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            if (enchantment.value().getSlotItems(entity).containsKey(slot)) {
                visitor.accept(enchantment, entry.getIntValue(), new EnchantedItemInUse(stack, slot, entity));
            }
        }
    }

    /** 1.21's {@code runIterationOnItem(stack, visitor)}: each enchantment of the stack (1.20.6's visitor takes it plain) */
    public static void runIterationOnItem(ItemStack stack, EnchantmentHelper.EnchantmentVisitor visitor) {
        for (var entry : stack.getEnchantments().entrySet()) {
            visitor.accept(entry.getKey().value(), entry.getIntValue());
        }
    }

    /** 1.21's {@code getHighestLevel(stack, effect)}: the enchantment of the stack with that effect at its highest level */
    public static com.mojang.datafixers.util.Pair<?, Integer> getHighestLevel(ItemStack stack, DataComponentType<?> type) {
        ResourceLocation id = effectId(type);
        Enchantment best = null;
        int level = 0;
        for (var entry : stack.getEnchantments().entrySet()) {
            if (hasEffect(entry.getKey().value(), id) && entry.getIntValue() > level) {
                best = entry.getKey().value();
                level = entry.getIntValue();
            }
        }
        return best == null ? null : com.mojang.datafixers.util.Pair.of(EnchantEffects.effects(best).get(type), level);
    }

    /** 1.21's {@code pickHighestLevel(stack, effect)}: that effect's value (none: empty) */
    public static Optional<?> pickHighestLevel(ItemStack stack, DataComponentType<?> type) {
        var pair = getHighestLevel(stack, type);
        return pair == null ? Optional.empty() : Optional.ofNullable(pair.getFirst());
    }

    // values of 1.21's vanilla effects, as 1.20.6 computes them

    /** {@code getFishingTimeReduction}: seconds, five a level of lure */
    public static float getFishingTimeReduction(ServerLevel level, ItemStack rod, Entity fisher) {
        return 5.0F * EnchantmentHelper.getFishingSpeedBonus(rod);
    }

    public static int getFishingLuckBonus(ServerLevel level, ItemStack rod, Entity fisher) {
        return EnchantmentHelper.getFishingLuckBonus(rod);
    }

    public static int getTridentReturnToOwnerAcceleration(ServerLevel level, ItemStack trident, Entity owner) {
        return EnchantmentHelper.getLoyalty(trident);
    }

    /** {@code getTridentSpinAttackStrength}: riptide's 1.5 at level one, 0.75 more a level (0: none) */
    public static float getTridentSpinAttackStrength(ItemStack trident, LivingEntity user) {
        int riptide = EnchantmentHelper.getRiptide(trident);
        return riptide <= 0 ? 0.0F : 0.75F * (1 + riptide);
    }

    /** {@code modifyCrossbowChargingTime}: quick charge takes a quarter of a second a level */
    public static float modifyCrossbowChargingTime(ItemStack crossbow, LivingEntity user, float seconds) {
        return Math.max(0.0F, seconds - 0.25F * EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, crossbow));
    }

    /** {@code processProjectileCount}: multishot shoots two more */
    public static int processProjectileCount(ServerLevel level, ItemStack weapon, Entity shooter, int count) {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, weapon) > 0 ? count + 2 : count;
    }

    /** {@code processProjectileSpread}: multishot's ten degrees a level */
    public static float processProjectileSpread(ServerLevel level, ItemStack weapon, Entity shooter, float spread) {
        return spread + 10.0F * EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, weapon);
    }

    /** {@code modifyDurabilityToRepairFromXp}: mending repairs two points an experience point */
    public static int modifyDurabilityToRepairFromXp(ServerLevel level, ItemStack stack, int xp) {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, stack) > 0 ? xp * 2 : xp;
    }

    /** {@code processBlockExperience}: no vanilla enchantment changes it (1.20.6's fortune and silk touch act in the block) */
    public static int processBlockExperience(ServerLevel level, ItemStack tool, int xp) {
        return xp;
    }

    /** {@code modifyKnockback}: 1.20.6 adds knockback's levels in the attack itself */
    public static float modifyKnockback(ServerLevel level, ItemStack weapon, Entity target, net.minecraft.world.damagesource.DamageSource source, float knockback) {
        return knockback;
    }

    /** {@code processEquipmentDropChance}: looting's one percent a level */
    public static float processEquipmentDropChance(ServerLevel level, LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float chance) {
        return source.getEntity() instanceof LivingEntity killer ? chance + 0.01F * EnchantmentHelper.getMobLooting(killer) : chance;
    }

    /** {@code tickEffects}: 1.20.6's enchantments tick in their own code (frost walker, soul speed) */
    public static void tickEffects(ServerLevel level, LivingEntity entity) {
    }

    /** {@code onHitBlock}: 1.20.6's enchantments act on a hit block in their own code (channeling) */
    public static void onHitBlock(ServerLevel level, ItemStack weapon, LivingEntity owner, Entity projectile, EquipmentSlot slot,
            net.minecraft.world.phys.Vec3 position, net.minecraft.world.level.block.state.BlockState state, java.util.function.Consumer<?> onBreak) {
    }

    /** {@code forEachModifier(stack, slot, action)}: 1.20.6's enchantments give no attribute modifiers */
    public static void forEachModifier(ItemStack stack, EquipmentSlot slot, java.util.function.BiConsumer<?, ?> action) {
    }

    // enchanting

    private static FeatureFlagSet features() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? FeatureFlags.VANILLA_SET : server.getWorldData().enabledFeatures();
    }

    /** 1.21's {@code enchantItem(random, stack, level, registries, options)}: 1.20.6's of the enabled enchantments */
    public static ItemStack enchantItem(RandomSource random, ItemStack stack, int level, RegistryAccess registries, Optional<?> options) {
        return EnchantmentHelper.enchantItem(features(), random, stack, level, false);
    }

    /** 1.21's {@code getAvailableEnchantmentResults(power, stack, candidates)}: each candidate at its highest level in reach */
    public static List<EnchantmentInstance> getAvailableEnchantmentResults(int power, ItemStack stack, Stream<Holder<Enchantment>> candidates) {
        List<EnchantmentInstance> results = new ArrayList<>();
        boolean book = stack.is(Items.BOOK);
        candidates.forEach(holder -> {
            Enchantment enchantment = holder.value();
            if (!book && !enchantment.isPrimaryItem(stack)) {
                return;
            }
            for (int level = enchantment.getMaxLevel(); level >= enchantment.getMinLevel(); level--) {
                if (power >= enchantment.getMinCost(level) && power <= enchantment.getMaxCost(level)) {
                    results.add(new EnchantmentInstance(enchantment, level));
                    break;
                }
            }
        });
        return results;
    }

    /** a stack's enchantments by their holders (1.21's {@code stack.getEnchantments()} as a map) */
    static Map<Holder<Enchantment>, Integer> levels(ItemStack stack) {
        Map<Holder<Enchantment>, Integer> out = new java.util.LinkedHashMap<>();
        for (var entry : stack.getEnchantments().entrySet()) {
            out.put(entry.getKey(), entry.getIntValue());
        }
        return out;
    }
}
