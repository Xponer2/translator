/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import ladder.rung1206.shape.AddedTo;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * 1.21's static members of EnchantmentHelper that 1.20.6's hasn't, in 1.21's shape: an add-static shape patch copies
 * them into the class (shape-patches.tsv), so the mods' mixins find their points in them (each stack's enchantments by
 * getOrDefault, the list of finds, the entity's random); the adapters' calls of them are the class's (AddedTo). No
 * local or parameter of a carried type (the game loads the class without the pack's carried classes).
 */
@AddedTo("net/minecraft/world/item/enchantment/EnchantmentHelper")
public final class AddedEnchantmentHelper {
    private AddedEnchantmentHelper() {
    }

    /** 1.21's {@code getRandomItemWith(effect, entity, filter)}: an equipped stack with that effect, at random */
    public static Optional<Object> getRandomItemWith(DataComponentType<?> componentType, LivingEntity entity, Predicate<ItemStack> filter) {
        List<Object> list = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (filter.test(stack)) {
                ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                    if (EnchantHelpers.randomSearchMatches(entry.getKey(), componentType, slot, entity)) {
                        list.add(EnchantHelpers.inUse(stack, slot, entity));
                    }
                }
            }
        }
        return Util.getRandomSafe(list, entity.getRandom());
    }

    /**
     * 1.21's {@code getAvailableEnchantmentResults(power, stack, possible)}: of the candidates, those the stack may take at
     * the table (its filter: 1.20.6's test, a book's allowed enchantments or the item's own), each at its highest level
     * the power reaches. 1.20.6's own table method calls it with its candidates (EnchantmentTableMixin), so the mods'
     * points (fabric's filter, VanillaBackport's) see the table. No lambda: the method is copied into the game class.
     */
    public static List<net.minecraft.world.item.enchantment.EnchantmentInstance> getAvailableEnchantmentResults(int power, ItemStack stack,
            java.util.stream.Stream<Holder<Enchantment>> possible) {
        List<net.minecraft.world.item.enchantment.EnchantmentInstance> list = new ArrayList<>();
        boolean book = stack.is(net.minecraft.world.item.Items.BOOK);
        possible.filter(new TableCandidate(stack, book)).forEach(new HighestLevel(power, list));
        return list;
    }

    /** 1.20.6's test of a table candidate for a stack */
    public record TableCandidate(ItemStack stack, boolean book) implements Predicate<Holder<Enchantment>> {
        @Override
        public boolean test(Holder<Enchantment> holder) {
            Enchantment enchantment = holder.value();
            return this.book && enchantment.isAllowedOnBooks() || enchantment.canApplyAtEnchantingTable(this.stack) && enchantment.isPrimaryItem(this.stack);
        }
    }

    /** a candidate's highest level the power reaches, into the list */
    public record HighestLevel(int power, List<net.minecraft.world.item.enchantment.EnchantmentInstance> list)
            implements java.util.function.Consumer<Holder<Enchantment>> {
        @Override
        public void accept(Holder<Enchantment> holder) {
            Enchantment enchantment = holder.value();
            for (int level = enchantment.getMaxLevel(); level > enchantment.getMinLevel() - 1; level--) {
                if (this.power >= enchantment.getMinCost(level) && this.power <= enchantment.getMaxCost(level)) {
                    this.list.add(new net.minecraft.world.item.enchantment.EnchantmentInstance(enchantment, level));
                    break;
                }
            }
        }
    }
}
