/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge 21.1's {@code AttributeUtil} on 1.21: an item's attribute lines made the 21.1 way (ItemStackMixin calls it
 * where 1.21 makes its own), with GatherSkippedAttributeTooltipsEvent before and AddAttributeTooltipsEvent after; its
 * text comes from the attributes (AttributeText: a mod attribute's overrides, else 21.1's defaults). Merged lines
 * only when a mod asks for them (enableMergedAttributeTooltips, as 21.1's NeoForgeMod).
 */
public class AttributeUtil {
    public static final ResourceLocation BASE_ATTACK_DAMAGE_ID = Item.BASE_ATTACK_DAMAGE_ID;
    public static final ResourceLocation BASE_ATTACK_SPEED_ID = Item.BASE_ATTACK_SPEED_ID;
    public static final ResourceLocation BASE_ENTITY_REACH_ID = ResourceLocation.withDefaultNamespace("base_entity_reach");
    public static final ResourceLocation FAKE_MERGED_ID = ResourceLocation.fromNamespaceAndPath("neoforge", "fake_merged_modifier");
    /** by operation, the strongest first, then by id */
    public static final Comparator<AttributeModifier> ATTRIBUTE_MODIFIER_COMPARATOR = Comparator.comparing(AttributeModifier::operation)
            .thenComparingDouble(m -> -Math.abs(m.amount())).thenComparing(AttributeModifier::id);
    private static boolean merged;

    /** NeoForgeMod.enableMergedAttributeTooltips() of 21.1 */
    public static void enableMergedAttributeTooltips() {
        merged = true;
    }

    /** NeoForgeMod.shouldMergeAttributeTooltips() of 21.1 */
    public static boolean shouldMergeAttributeTooltips() {
        return merged;
    }

    public static void addAttributeTooltips(ItemStack stack, Consumer<Component> tooltip, AttributeTooltipContext ctx) {
        if (stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).showInTooltip()) {
            applyModifierTooltips(stack, tooltip, ctx);
        }
        NeoForge.EVENT_BUS.post(new AddAttributeTooltipsEvent(stack, tooltip, ctx));
    }

    /** the lines of every slot group (an empty line, the group's name, its modifiers), less what listeners skip */
    public static void applyModifierTooltips(ItemStack stack, Consumer<Component> tooltip, AttributeTooltipContext ctx) {
        GatherSkippedAttributeTooltipsEvent skipped = NeoForge.EVENT_BUS.post(new GatherSkippedAttributeTooltipsEvent(stack, ctx));
        if (skipped.isSkippingAll()) {
            return;
        }
        for (EquipmentSlotGroup group : EquipmentSlotGroup.values()) {
            if (skipped.isSkipped(group)) {
                continue;
            }
            Multimap<Holder<Attribute>, AttributeModifier> modifiers = getSortedModifiers(stack, group);
            modifiers.values().removeIf(m -> skipped.isSkipped(m.id()));
            if (modifiers.isEmpty()) {
                continue;
            }
            tooltip.accept(Component.empty());
            tooltip.accept(Component.translatable("item.modifiers." + group.getSerializedName()).withStyle(ChatFormatting.GRAY));
            applyTextFor(stack, tooltip, modifiers, ctx);
        }
    }

    /** the lines of these modifiers: base modifiers first (green, or merged gold), then the others */
    public static void applyTextFor(ItemStack stack, Consumer<Component> tooltip, Multimap<Holder<Attribute>, AttributeModifier> modifierMap,
            AttributeTooltipContext ctx) {
        if (modifierMap.isEmpty()) {
            return;
        }
        Map<Holder<Attribute>, AttributeModifier> bases = new LinkedHashMap<>();
        Map<Holder<Attribute>, List<AttributeModifier>> children = new LinkedHashMap<>();
        for (Iterator<Map.Entry<Holder<Attribute>, AttributeModifier>> it = modifierMap.entries().iterator(); it.hasNext();) {
            Map.Entry<Holder<Attribute>, AttributeModifier> entry = it.next();
            if (entry.getValue().id().equals(AttributeText.getBaseId(entry.getKey().value()))) {
                bases.put(entry.getKey(), entry.getValue());
                children.put(entry.getKey(), new ArrayList<>());
                it.remove();
            }
        }
        for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : modifierMap.entries()) {
            List<AttributeModifier> of = children.get(entry.getKey());
            if (of != null) {
                of.add(entry.getValue());
            }
        }
        TooltipFlag flag = ctx.flag();
        for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : bases.entrySet()) {
            Attribute attribute = entry.getKey().value();
            List<AttributeModifier> more = children.get(entry.getKey());
            double entityBase = ctx.player() == null ? 0 : ctx.player().getAttributeBaseValue(entry.getKey());
            double base = entry.getValue().amount() + entityBase;
            double rawBase = base;
            double total = base;
            if (merged) {
                for (AttributeModifier m : more) {
                    switch (m.operation()) {
                        case ADD_VALUE -> base = total = total + m.amount();
                        case ADD_MULTIPLIED_BASE -> total += m.amount() * base;
                        case ADD_MULTIPLIED_TOTAL -> total *= 1 + m.amount();
                    }
                }
            }
            boolean isMerged = merged && !more.isEmpty();
            MutableComponent text = AttributeText.toBaseComponent(attribute, total, entityBase, isMerged, flag);
            tooltip.accept(Component.literal(" ").append(text).withStyle(isMerged ? ChatFormatting.GOLD : ChatFormatting.DARK_GREEN));
            if (flag.hasShiftDown() && isMerged) {
                tooltip.accept(listHeader().append(AttributeText.toBaseComponent(attribute, rawBase, entityBase, false, flag).withStyle(ChatFormatting.DARK_GREEN)));
                for (AttributeModifier m : more) {
                    tooltip.accept(listHeader().append(AttributeText.toComponent(attribute, m, flag)));
                }
            }
        }
        for (Holder<Attribute> holder : modifierMap.keySet()) {
            if (merged && bases.containsKey(holder)) {
                continue;
            }
            Attribute attribute = holder.value();
            Collection<AttributeModifier> modifiers = modifierMap.get(holder);
            if (merged && modifiers.size() > 1) {
                Map<AttributeModifier.Operation, Merge> byOperation = new EnumMap<>(AttributeModifier.Operation.class);
                for (AttributeModifier m : modifiers) {
                    if (m.amount() == 0) {
                        continue;
                    }
                    Merge merge = byOperation.computeIfAbsent(m.operation(), op -> new Merge());
                    merge.merged |= merge.sum != 0;
                    merge.sum += m.amount();
                    merge.children.add(m);
                }
                for (AttributeModifier.Operation op : AttributeModifier.Operation.values()) {
                    Merge merge = byOperation.get(op);
                    if (merge == null || merge.sum == 0) {
                        continue;
                    }
                    MutableComponent line = AttributeText.toComponent(attribute, new AttributeModifier(FAKE_MERGED_ID, merge.sum, op), flag);
                    if (merge.merged) {
                        TextColor color = AttributeText.getMergedStyle(attribute, merge.sum > 0);
                        tooltip.accept(line.withStyle(line.getStyle().withColor(color)));
                        if (flag.hasShiftDown()) {
                            merge.children.forEach(m -> tooltip.accept(listHeader().append(AttributeText.toComponent(attribute, m, flag))));
                        }
                    } else {
                        tooltip.accept(line);
                    }
                }
            } else {
                for (AttributeModifier m : modifiers) {
                    if (m.amount() != 0) {
                        tooltip.accept(AttributeText.toComponent(attribute, m, flag));
                    }
                }
            }
        }
    }

    /** a potion's attribute lines (the client's tooltip flag) */
    public static void addPotionTooltip(List<Pair<Holder<Attribute>, AttributeModifier>> list, Consumer<Component> tooltips) {
        TooltipFlag flag = FMLEnvironment.dist.isClient() ? ladder.rung121.ClientAccess.tooltipFlag() : TooltipFlag.NORMAL;
        for (Pair<Holder<Attribute>, AttributeModifier> pair : list) {
            tooltips.accept(AttributeText.toComponent(pair.getFirst().value(), pair.getSecond(), flag));
        }
    }

    /** a map sorted by attribute key, then by ATTRIBUTE_MODIFIER_COMPARATOR (a stable order of lines) */
    public static Multimap<Holder<Attribute>, AttributeModifier> sortedMap() {
        return TreeMultimap.create(Comparator.comparing((Holder<Attribute> h) -> h.unwrapKey().map(k -> k.location().toString()).orElse("")),
                ATTRIBUTE_MODIFIER_COMPARATOR);
    }

    /** every modifier of the stack for the group (components or the item's defaults, enchantments, the event) */
    public static Multimap<Holder<Attribute>, AttributeModifier> getSortedModifiers(ItemStack stack, EquipmentSlotGroup slot) {
        Multimap<Holder<Attribute>, AttributeModifier> map = LinkedListMultimap.create();
        stack.forEachModifier(slot, (attribute, modifier) -> {
            if (attribute != null && modifier != null) {
                map.put(attribute, modifier);
            }
        });
        return map;
    }

    private static MutableComponent listHeader() {
        return Component.literal(" ┇ ").withStyle(ChatFormatting.GRAY);
    }

    private static final class Merge {
        double sum;
        boolean merged;
        final List<AttributeModifier> children = new ArrayList<>();
    }
}
