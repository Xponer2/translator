/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package ladder.adapter.misc;

import java.util.List;

import javax.annotation.Nullable;

import ladder.adapter.mixin.EntityAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/** Small members of the newer versions, on 1.20.4 (receiver first): each in 1.20.4's own terms. */
public final class MiscAdapters {
    private MiscAdapters() {
    }

    /** 1.20.5's {@code Entity.getRandom()}: the entity's own random (1.20.4 has it, protected). */
    public static RandomSource getRandom(Entity entity) {
        return ((EntityAccess) entity).ladder$random();
    }

    /** 1.20.5's {@code Entity.registryAccess()}: its level's. */
    public static RegistryAccess registryAccess(Entity entity) {
        return entity.level().registryAccess();
    }

    /** 1.20.5's {@code hurtAndBreak(amount, entity, slot)}: 1.20.4's with the break event of that slot. */
    public static void hurtAndBreak(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot) {
        stack.hurtAndBreak(amount, entity, e -> e.broadcastBreakEvent(slot));
    }

    /** 1.20.5's {@code DyeColor.getTextureDiffuseColor()}: the ARGB of 1.20.4's three floats. */
    public static int getTextureDiffuseColor(DyeColor color) {
        float[] rgb = color.getTextureDiffuseColors();
        return FastColor.ARGB32.color(255, Math.round(rgb[0] * 255.0F), Math.round(rgb[1] * 255.0F), Math.round(rgb[2] * 255.0F));
    }

    /** 1.20.5's {@code ItemStack.getTooltipLines(context, player, flag)}: 1.20.4's has no context (it reads the level itself) */
    public static List<Component> getTooltipLines(ItemStack stack, ladder.adapter.provided.TooltipContext context, @Nullable Player player, TooltipFlag flag) {
        return stack.getTooltipLines(player, flag);
    }

    /** 1.20.5's {@code BlockState.isPathfindable(type)}: 1.20.4's asks for a level and a position its blocks rarely read */
    public static boolean isPathfindable(BlockBehaviour.BlockStateBase state, PathComputationType type) {
        return state.isPathfindable(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, type);
    }

    /** 1.20.5's {@code BlockState.useWithoutItem(level, player, hit)}: 1.20.4's use with the main hand */
    public static InteractionResult useWithoutItem(BlockBehaviour.BlockStateBase state, Level level, Player player, BlockHitResult hit) {
        return state.use(level, player, InteractionHand.MAIN_HAND, hit);
    }

    /** 1.20.5's {@code Mob.finalizeSpawn} without the spawn's NBT (1.20.4 takes it, null when none) */
    @Nullable
    public static SpawnGroupData finalizeSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type,
            @Nullable SpawnGroupData data) {
        return mob.finalizeSpawn(level, difficulty, type, data, null);
    }

    /** 1.20.5's {@code Holder.getRegisteredName()} */
    public static String getRegisteredName(net.minecraft.core.Holder<?> holder) {
        return holder.unwrapKey().map(key -> key.location().toString()).orElse("[unregistered]");
    }

    /** 1.20.5's {@code Holder.is(other)}: the same registry entry (by key), or the same value */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean is(net.minecraft.core.Holder holder, net.minecraft.core.Holder other) {
        if (holder == other) {
            return true;
        }
        java.util.Optional<net.minecraft.resources.ResourceKey> key = holder.unwrapKey();
        return key.isPresent() ? other.is(key.get()) : holder.value() == other.value();
    }

    /** 1.20.5's {@code Registry.getHolder(location)} */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static java.util.Optional<?> getHolder(net.minecraft.core.Registry registry, net.minecraft.resources.ResourceLocation location) {
        return registry.getHolder(net.minecraft.resources.ResourceKey.create(registry.key(), location));
    }

    /** 1.20.5's food record: {@code nutrition()} */
    public static int nutrition(net.minecraft.world.food.FoodProperties food) {
        return food.getNutrition();
    }

    /** 1.20.5's {@code saturation()}: the saturation itself, which 1.20.4 keeps as a modifier (nutrition * modifier * 2) */
    public static float saturation(net.minecraft.world.food.FoodProperties food) {
        return food.getNutrition() * food.getSaturationModifier() * 2.0F;
    }

    /** 1.20.5's {@code eatSeconds()}: 1.20.4's fast food is eaten in 16 ticks, the rest in 32 */
    public static float eatSeconds(net.minecraft.world.food.FoodProperties food) {
        return food.isFastFood() ? 0.8F : 1.6F;
    }

    // 1.21's Component.Serializer takes the registries; 1.20.4's needs none

    public static String toJson(Component component, HolderLookup.Provider registries) {
        return Component.Serializer.toJson(component);
    }

    @Nullable
    public static MutableComponent fromJson(String json, HolderLookup.Provider registries) {
        return Component.Serializer.fromJson(json);
    }

    @Nullable
    public static MutableComponent fromJson(com.google.gson.JsonElement json, HolderLookup.Provider registries) {
        return Component.Serializer.fromJson(json);
    }

    @Nullable
    public static MutableComponent fromJsonLenient(String json, HolderLookup.Provider registries) {
        return Component.Serializer.fromJsonLenient(json);
    }
}
