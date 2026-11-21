/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
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

    // 1.20.5's block entity loading and saving entries (registries passed; 1.20.4's load and save take none)

    /**
     * 1.20.5's {@code loadWithComponents}: 1.20.4's load; the tag's block entity components (1.20.4 has none) are kept
     * in the entity's persistent data, so they are saved with it and come back as they were.
     */
    public static void loadWithComponents(net.minecraft.world.level.block.entity.BlockEntity entity, net.minecraft.nbt.CompoundTag tag,
            HolderLookup.Provider registries) {
        entity.load(tag);
        if (tag.contains("components")) {
            entity.getPersistentData().put(COMPONENTS_KEPT, tag.get("components").copy());
        }
    }

    /** where a block entity keeps the components 1.20.4 can't read (in its persistent data) */
    public static final String COMPONENTS_KEPT = "ladder:components";

    public static void loadCustomOnly(net.minecraft.world.level.block.entity.BlockEntity entity, net.minecraft.nbt.CompoundTag tag,
            HolderLookup.Provider registries) {
        entity.load(tag);
    }

    public static net.minecraft.nbt.CompoundTag saveCustomOnly(net.minecraft.world.level.block.entity.BlockEntity entity, HolderLookup.Provider registries) {
        return entity.saveWithoutMetadata();
    }

    public static net.minecraft.nbt.CompoundTag saveCustomAndMetadata(net.minecraft.world.level.block.entity.BlockEntity entity,
            HolderLookup.Provider registries) {
        return entity.saveWithFullMetadata();
    }

    /** 1.20.5's {@code HolderLookup.Provider.createSerializationContext(ops)}: 1.20.4's RegistryOps over the same registries */
    public static net.minecraft.resources.RegistryOps<?> createSerializationContext(HolderLookup.Provider registries,
            com.mojang.serialization.DynamicOps<?> ops) {
        return net.minecraft.resources.RegistryOps.create(ops, registries);
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

    /** FastColor.ARGB32.color(r, g, b): the opaque colour (1.20.5's short form of color(255, r, g, b)). */
    public static int opaqueColor(int r, int g, int b) {
        return FastColor.ARGB32.color(255, r, g, b);
    }

    /** 1.20.5's {@code LivingEntity.hasInfiniteMaterials()}: a player in creative (instabuild), no other entity */
    public static boolean hasInfiniteMaterials(net.minecraft.world.entity.LivingEntity entity) {
        return entity instanceof Player player && player.getAbilities().instabuild;
    }

    /** {@code Player.hasInfiniteMaterials()} */
    public static boolean hasInfiniteMaterials(Player player) {
        return player.getAbilities().instabuild;
    }

    /**
     * 1.20.5's {@code StructureManager.checkStructurePresence(chunk, structure, placement, skipReferenced)}: the placement
     * is 1.20.5's quick check before the chunk's own; 1.20.4 checks the chunk the same way without it
     */
    public static net.minecraft.world.level.levelgen.structure.StructureCheckResult checkStructurePresence(
            net.minecraft.world.level.StructureManager manager, net.minecraft.world.level.ChunkPos chunk,
            net.minecraft.world.level.levelgen.structure.Structure structure,
            net.minecraft.world.level.levelgen.structure.placement.StructurePlacement placement, boolean skipReferenced) {
        return manager.checkStructurePresence(chunk, structure, skipReferenced);
    }

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> SKULLS = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, new net.minecraft.resources.ResourceLocation("skulls"));

    /** 1.20.5's {@code ItemTags.SKULLS} (the tag the adapters' data gives: data/minecraft/tags/items/skulls.json) */
    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> skulls() {
        return SKULLS;
    }

    /**
     * BlockEntity.isValidBlockState(state) of 1.21.1, called: a converted mod block entity's own override, else its
     * type's check (1.20.4 makes and sets block entities unchecked)
     */
    public static boolean isValidBlockState(net.minecraft.world.level.block.entity.BlockEntity blockEntity, net.minecraft.world.level.block.state.BlockState state) {
        if (blockEntity instanceof ladder.adapter.block.ValidStateApi api
                && ladder.adapter.Dispatch.declares(blockEntity.getClass(), "isValidBlockState", net.minecraft.world.level.block.state.BlockState.class)) {
            return api.isValidBlockState(state);
        }
        return blockEntity.getType().isValid(state);
    }

    /** a mod block entity's {@code super.isValidBlockState(state)}: its type's check */
    public static boolean blockEntityValidBlockState(net.minecraft.world.level.block.entity.BlockEntity blockEntity, net.minecraft.world.level.block.state.BlockState state) {
        return blockEntity.getType().isValid(state);
    }

    /** NeoForge 21's {@code BlockToolModificationEvent.getItemAbility()}: the ability, a ToolAction below */
    public static net.neoforged.neoforge.common.ToolAction getItemAbility(net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent event) {
        return event.getToolAction();
    }

    /** 1.21's {@code Entity.igniteForSeconds(seconds)}: on fire for that long, unless it burns longer already */
    public static void igniteForSeconds(net.minecraft.world.entity.Entity entity, float seconds) {
        int ticks = net.minecraft.util.Mth.floor(seconds * 20.0F);
        if (entity.getRemainingFireTicks() < ticks) {
            entity.setRemainingFireTicks(ticks);
        }
    }

    /** 1.21's {@code DamageSource.isDirect()}: the cause is what hit */
    public static boolean isDirect(net.minecraft.world.damagesource.DamageSource source) {
        return source.getEntity() == source.getDirectEntity();
    }

    /** 1.21's {@code Level.playSound(player, x, y, z, sound holder, source, volume, pitch)} */
    public static void playSound(net.minecraft.world.level.Level level, @javax.annotation.Nullable net.minecraft.world.entity.player.Player player, double x,
            double y, double z, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, net.minecraft.sounds.SoundSource source, float volume,
            float pitch) {
        level.playSound(player, x, y, z, sound.value(), source, volume, pitch);
    }

    /** NeoForge 21's {@code BuildCreativeModeTabContentsEvent.insertAfter(existing, stack, visibility)} */
    public static void insertAfter(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event, net.minecraft.world.item.ItemStack existing,
            net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.CreativeModeTab.TabVisibility visibility) {
        event.getEntries().putAfter(entry(event, existing), stack, visibility);
    }

    /** NeoForge 21's {@code insertBefore(existing, stack, visibility)} */
    public static void insertBefore(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event, net.minecraft.world.item.ItemStack existing,
            net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.CreativeModeTab.TabVisibility visibility) {
        event.getEntries().putBefore(entry(event, existing), stack, visibility);
    }

    /** NeoForge 21's {@code insertFirst(stack, visibility)} */
    public static void insertFirst(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event, net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.item.CreativeModeTab.TabVisibility visibility) {
        event.getEntries().putFirst(stack, visibility);
    }

    /** NeoForge 21's {@code remove(existing, visibility)}: the entry leaves the tabs the visibility names */
    public static void remove(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event, net.minecraft.world.item.ItemStack existing,
            net.minecraft.world.item.CreativeModeTab.TabVisibility visibility) {
        net.minecraft.world.item.ItemStack key = entry(event, existing);
        net.minecraft.world.item.CreativeModeTab.TabVisibility now = event.getEntries().get(key);
        if (now == null) {
            return;
        }
        net.minecraft.world.item.CreativeModeTab.TabVisibility both = net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        if (visibility == both || now == visibility) {
            event.getEntries().remove(key);
        } else if (now == both) {
            // one of the two tabs only: the entry stays in the other
            event.getEntries().put(key, visibility == net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_TAB_ONLY
                    ? net.minecraft.world.item.CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY : net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
    }

    /** the tab's own entry that is the stack (the same item and components), else the stack */
    private static net.minecraft.world.item.ItemStack entry(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event, net.minecraft.world.item.ItemStack stack) {
        for (var e : event.getEntries()) {
            if (net.minecraft.world.item.ItemStack.isSameItemSameTags(e.getKey(), stack)) {
                return e.getKey();
            }
        }
        return stack;
    }

    /** 1.21's {@code Enchantment.description()}: its translated name */
    public static net.minecraft.network.chat.Component description(net.minecraft.world.item.enchantment.Enchantment enchantment) {
        return net.minecraft.network.chat.Component.translatable(enchantment.getDescriptionId());
    }

    /** 1.21's {@code Enchantment.areCompatible(a, b)} */
    public static boolean areCompatible(net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> a,
            net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> b) {
        return !a.equals(b) && a.value().isCompatibleWith(b.value());
    }

    /** 1.21's {@code CrossbowItem.getChargeDuration(stack, entity)} */
    public static int getChargeDuration(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return net.minecraft.world.item.CrossbowItem.getChargeDuration(stack);
    }

    /** 1.21's {@code WorldBorder.clampToBounds(pos)} */
    public static net.minecraft.core.BlockPos clampToBounds(net.minecraft.world.level.border.WorldBorder border, net.minecraft.core.BlockPos pos) {
        return border.clampToBounds(pos.getX(), pos.getY(), pos.getZ());
    }

    /** NeoForge 21's {@code ModConfigSpec.Builder.gameRestart()}: a value the game must restart for (shown nowhere below) */
    public static net.neoforged.neoforge.common.ModConfigSpec.Builder gameRestart(net.neoforged.neoforge.common.ModConfigSpec.Builder builder) {
        return builder.worldRestart();
    }

    /** 1.21's {@code AttributeMap.removeAttributeModifiers(modifiers)}: by attribute holder, each modifier by its id */
    public static void removeAttributeModifiers(net.minecraft.world.entity.ai.attributes.AttributeMap map,
            com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier> modifiers) {
        modifiers.forEach((attribute, modifier) -> {
            net.minecraft.world.entity.ai.attributes.AttributeInstance instance = map.getInstance(attribute.value());
            if (instance != null) {
                instance.removeModifier(modifier.getId());
            }
        });
    }

    /** 1.21's {@code AttributeMap.addTransientAttributeModifiers(modifiers)}: a modifier of the same id replaced */
    public static void addTransientAttributeModifiers(net.minecraft.world.entity.ai.attributes.AttributeMap map,
            com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier> modifiers) {
        modifiers.forEach((attribute, modifier) -> {
            net.minecraft.world.entity.ai.attributes.AttributeInstance instance = map.getInstance(attribute.value());
            if (instance != null) {
                instance.removeModifier(modifier.getId());
                instance.addTransientModifier(modifier);
            }
        });
    }
}
