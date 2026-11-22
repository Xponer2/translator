/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.misc;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import ladder.rung1206.mixin.EntityAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** Small members of 1.21 on 1.20.6 (receiver first). */
public final class MiscAdapters {
    private MiscAdapters() {
    }

    /** 1.21's {@code Entity.getRandom()}: the entity's own random (1.20.6 has it, protected). */
    public static RandomSource getRandom(Entity entity) {
        return ((EntityAccess) entity).ladder$random();
    }

    /** 1.21's {@code DyeColor.getTextureDiffuseColor()}: the ARGB of 1.20.6's three floats. */
    public static int getTextureDiffuseColor(DyeColor color) {
        float[] rgb = color.getTextureDiffuseColors();
        return FastColor.ARGB32.color(255, Math.round(rgb[0] * 255.0F), Math.round(rgb[1] * 255.0F), Math.round(rgb[2] * 255.0F));
    }

    /**
     * 1.21's {@code hurtAndBreak(amount, level, entity, onBreak)}: 1.20.6's with the level's random; a broken stack
     * shrinks and the callback gets its item, as in 1.21.
     */
    public static void hurtAndBreak(ItemStack stack, int amount, ServerLevel level, @Nullable LivingEntity entity, Consumer<Item> onBreak) {
        if (entity instanceof net.minecraft.world.entity.player.Player player && player.hasInfiniteMaterials()) {
            // 1.21: a creative player's items don't wear
            return;
        }
        Runnable broken = () -> {
            Item item = stack.getItem();
            stack.shrink(1);
            onBreak.accept(item);
        };
        if (entity == null) {
            stack.hurtAndBreak(amount, level.getRandom(), (net.minecraft.server.level.ServerPlayer) null, broken);
        } else {
            stack.hurtAndBreak(amount, level.getRandom(), entity, broken);
        }
    }

    /** the same with a server player */
    public static void hurtAndBreak(ItemStack stack, int amount, ServerLevel level, @Nullable net.minecraft.server.level.ServerPlayer player, Consumer<Item> onBreak) {
        hurtAndBreak(stack, amount, level, (LivingEntity) player, onBreak);
    }

    /** 1.21's {@code transmuteCopy(item)}: the stack's own count */
    public static ItemStack transmuteCopy(ItemStack stack, ItemLike item) {
        return stack.transmuteCopy(item, stack.getCount());
    }

    /** NeoForge 21's {@code BlockToolModificationEvent.getItemAbility()}: the ability, a ToolAction below */
    public static net.neoforged.neoforge.common.ToolAction getItemAbility(net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent event) {
        return event.getToolAction();
    }

    /** 1.21's {@code Entity.igniteForSeconds(seconds)}: on fire for that long, unless it burns longer already */
    public static void igniteForSeconds(net.minecraft.world.entity.Entity entity, float seconds) {
        entity.igniteForTicks(net.minecraft.util.Mth.floor(seconds * 20.0F));
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
            if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(e.getKey(), stack)) {
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
            net.minecraft.world.entity.ai.attributes.AttributeInstance instance = map.getInstance(attribute);
            if (instance != null) {
                instance.removeModifier(modifier.id());
            }
        });
    }

    /** 1.21's {@code AttributeMap.addTransientAttributeModifiers(modifiers)}: a modifier of the same id replaced */
    public static void addTransientAttributeModifiers(net.minecraft.world.entity.ai.attributes.AttributeMap map,
            com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier> modifiers) {
        modifiers.forEach((attribute, modifier) -> {
            net.minecraft.world.entity.ai.attributes.AttributeInstance instance = map.getInstance(attribute);
            if (instance != null) {
                instance.removeModifier(modifier.id());
                instance.addTransientModifier(modifier);
            }
        });
    }
}
