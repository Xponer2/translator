/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.misc;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/** More of 1.21's small members on 1.20.6 (the rows' adapters, receiver first). */
public final class MoreAdapters {
    /** a food builder's "converts to" item until it builds, then the food's */
    private static final Map<FoodProperties.Builder, ItemLike> BUILDING = new WeakHashMap<>();
    private static final Map<FoodProperties, ItemLike> CONVERTS = new WeakHashMap<>();

    private MoreAdapters() {
    }

    /** {@code registry.getAny()}: its first entry */
    public static <T> Optional<Holder.Reference<T>> getAny(Registry<T> registry) {
        return registry.holders().findFirst();
    }

    /** {@code ExplosionInteraction.TRIGGER} (a wind charge's: it triggers blocks, breaks none): 1.20.6's NONE */
    public static Level.ExplosionInteraction trigger() {
        return Level.ExplosionInteraction.NONE;
    }

    /** {@code builder.usingConvertsTo(item)}: what is left after eating it (a bowl), given back when it is eaten */
    public static FoodProperties.Builder usingConvertsTo(FoodProperties.Builder builder, ItemLike item) {
        synchronized (BUILDING) {
            BUILDING.put(builder, item);
        }
        return builder;
    }

    /** the food a builder built (mixin.FoodBuilderMixin) */
    public static void built(FoodProperties.Builder builder, FoodProperties food) {
        ItemLike item;
        synchronized (BUILDING) {
            item = BUILDING.remove(builder);
        }
        if (item != null) {
            synchronized (CONVERTS) {
                CONVERTS.put(food, item);
            }
        }
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((LivingEntityUseItemEvent.Finish event) -> {
            FoodProperties food = event.getItem().getFoodProperties(event.getEntity());
            ItemLike left;
            synchronized (CONVERTS) {
                left = food == null ? null : CONVERTS.get(food);
            }
            if (left == null || event.getEntity().level().isClientSide) {
                return;
            }
            ItemStack rest = new ItemStack(left);
            if (event.getResultStack().isEmpty()) {
                event.setResultStack(rest);
            } else if (!(event.getEntity() instanceof Player player && player.getAbilities().instabuild)) {
                if (!(event.getEntity() instanceof Player player) || !player.getInventory().add(rest)) {
                    event.getEntity().spawnAtLocation(rest);
                }
            }
        });
    }

    /** {@code entity.getWeaponItem()}: the main hand's */
    public static ItemStack getWeaponItem(LivingEntity entity) {
        return entity.getMainHandItem();
    }

    /** {@code Registries.RECIPE} */
    public static ResourceKey<Registry<Recipe<?>>> recipeRegistry() {
        return ResourceKey.createRegistryKey(new ResourceLocation("recipe"));
    }

    /** {@code stack.isPrimaryItemFor(enchantment)}: 20.6's enchanting table check */
    public static boolean isPrimaryItemFor(net.neoforged.neoforge.common.extensions.IItemStackExtension stack, Holder<Enchantment> enchantment) {
        return ((ItemStack) stack).canApplyAtEnchantingTable(enchantment.value());
    }

    public static boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return stack.canApplyAtEnchantingTable(enchantment.value());
    }

    /** {@code LootItemCondition.DIRECT_CODEC} */
    public static Codec<LootItemCondition> lootConditionDirectCodec() {
        return LootItemConditions.DIRECT_CODEC;
    }

    /** {@code ModelBakery.MISSING_MODEL_VARIANT}: 20.6's missing model location */
    public static net.minecraft.client.resources.model.ModelResourceLocation missingModelVariant() {
        return net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_LOCATION;
    }

    // NeoForge 21's LivingShieldBlockEvent is 20.6's ShieldBlockEvent (an R row): blocked = not cancelled

    public static boolean getBlocked(net.neoforged.neoforge.event.entity.living.ShieldBlockEvent event) {
        return !event.isCanceled();
    }

    public static boolean getOriginalBlock(net.neoforged.neoforge.event.entity.living.ShieldBlockEvent event) {
        return true;
    }

    public static void setBlocked(net.neoforged.neoforge.event.entity.living.ShieldBlockEvent event, boolean blocked) {
        event.setCanceled(!blocked);
    }

    /** the shield's own damage: 20.6 damages the shield by the blocked damage, or not at all */
    public static float shieldDamage(net.neoforged.neoforge.event.entity.living.ShieldBlockEvent event) {
        return event.shieldTakesDamage() ? event.getBlockedDamage() : 0.0F;
    }

    public static void setShieldDamage(net.neoforged.neoforge.event.entity.living.ShieldBlockEvent event, float damage) {
        event.setShieldTakesDamage(damage > 0.0F);
    }
}
