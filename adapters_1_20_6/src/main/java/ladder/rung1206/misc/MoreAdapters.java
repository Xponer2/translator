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

    /** {@code food.usingConvertsTo()}: the item left after eating, as its builder was given it */
    public static Optional<ItemStack> usingConvertsTo(FoodProperties food) {
        ItemLike left;
        synchronized (CONVERTS) {
            left = CONVERTS.get(food);
        }
        return Optional.ofNullable(left).map(ItemStack::new);
    }

    /** {@code tesselator.clear()}: 1.20.6's tesselator keeps one builder of its own, nothing to free */
    public static void clear(com.mojang.blaze3d.vertex.Tesselator tesselator) {
    }

    /**
     * 1.21's {@code behavior.consumeWithRemainder(source, stack, remainder)}: one of the stack used, the remainder back
     * into the dispenser (or out of it, when it is full)
     */
    public static ItemStack consumeWithRemainder(net.minecraft.core.dispenser.DefaultDispenseItemBehavior behavior, net.minecraft.core.dispenser.BlockSource source,
            ItemStack stack, ItemStack remainder) {
        stack.shrink(1);
        if (stack.isEmpty()) {
            return remainder;
        }
        if (source.blockEntity().addItem(remainder.copy()) < 0) {
            net.minecraft.core.Direction facing = source.state().getValue(net.minecraft.world.level.block.DispenserBlock.FACING);
            net.minecraft.core.dispenser.DefaultDispenseItemBehavior.spawnItem(source.level(), remainder, 6, facing,
                    net.minecraft.world.level.block.DispenserBlock.getDispensePosition(source));
        }
        return stack;
    }

    /** 1.21's {@code arrow.getWeaponItem()}: the weapon a 1.21 constructor gave it (ArrowKnockbackMixin), else none */
    public static ItemStack getWeaponItem(net.minecraft.world.entity.projectile.AbstractArrow arrow) {
        ItemStack weapon = ((ladder.rung1206.entity.ArrowWeapon) arrow).ladder$weapon();
        return weapon == null ? ItemStack.EMPTY : weapon;
    }

    /** the K row's resolver of 1.21's {@code new ValidationContext(reporter, params)}: none (1.21's is empty too) */
    public static net.minecraft.core.HolderGetter.Provider noResolver(net.minecraft.util.ProblemReporter reporter,
            net.minecraft.world.level.storage.loot.parameters.LootContextParamSet params) {
        return new net.minecraft.core.HolderGetter.Provider() {
            @Override
            public <T> Optional<net.minecraft.core.HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
                return Optional.empty();
            }
        };
    }

    /** {@code entity.onEquippedItemBroken(item, slot)}: 1.20.6's break event of the slot */
    public static void onEquippedItemBroken(LivingEntity entity, net.minecraft.world.item.Item item, net.minecraft.world.entity.EquipmentSlot slot) {
        entity.broadcastBreakEvent(slot);
    }

    /** {@code DamageType.CODEC}: a damage type of the data registry, by id */
    public static Codec<Holder<net.minecraft.world.damagesource.DamageType>> damageTypeCodec() {
        return net.minecraft.resources.RegistryFileCodec.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                net.minecraft.world.damagesource.DamageType.DIRECT_CODEC);
    }

    /** {@code Attribute.CODEC}: an attribute by id */
    public static Codec<Holder<net.minecraft.world.entity.ai.attributes.Attribute>> attributeCodec() {
        return net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.holderByNameCodec();
    }

    /** {@code GameEvent.CODEC}: a game event by id */
    public static Codec<Holder<net.minecraft.world.level.gameevent.GameEvent>> gameEventCodec() {
        return net.minecraft.core.registries.BuiltInRegistries.GAME_EVENT.holderByNameCodec();
    }

    /** {@code entity.getKnownMovement()}: its movement (1.21 keeps a player's last one sent by the client) */
    public static net.minecraft.world.phys.Vec3 getKnownMovement(net.minecraft.world.entity.Entity entity) {
        return entity.getDeltaMovement();
    }

    /** {@code Level.ExplosionInteraction.CODEC}: by 1.21's lower-case names (trigger: 1.20.6's none) */
    public static Codec<Level.ExplosionInteraction> explosionInteractionCodec() {
        return Codec.STRING.xmap(name -> name.equals("trigger") ? Level.ExplosionInteraction.NONE
                : Level.ExplosionInteraction.valueOf(name.toUpperCase(java.util.Locale.ROOT)), mode -> mode.name().toLowerCase(java.util.Locale.ROOT));
    }
}
