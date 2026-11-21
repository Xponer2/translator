/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.event;

import java.util.Map;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * NeoForge 21's {@code BuildCreativeModeTabContentsEvent.getParentEntries()} / {@code getSearchEntries()} on 20.6, whose
 * event keeps one map of the stacks and where they show: the stacks of the tab (or of the search), in order; a stack
 * removed from the set is removed from that place in the event too.
 */
public final class TabEntries {
    static final Hash.Strategy<ItemStack> SAME = new Hash.Strategy<>() {
        @Override
        public int hashCode(ItemStack stack) {
            return ItemStack.hashItemAndComponents(stack);
        }

        @Override
        public boolean equals(ItemStack a, ItemStack b) {
            return a == b || a != null && b != null && ItemStack.isSameItemSameComponents(a, b);
        }
    };

    private TabEntries() {
    }

    public static ObjectSortedSet<ItemStack> getParentEntries(BuildCreativeModeTabContentsEvent event) {
        return view(event, true);
    }

    public static ObjectSortedSet<ItemStack> getSearchEntries(BuildCreativeModeTabContentsEvent event) {
        return view(event, false);
    }

    private static boolean in(CreativeModeTab.TabVisibility visibility, boolean parent) {
        return visibility == CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                || visibility == (parent ? CreativeModeTab.TabVisibility.PARENT_TAB_ONLY : CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
    }

    private static ObjectSortedSet<ItemStack> view(BuildCreativeModeTabContentsEvent event, boolean parent) {
        ObjectLinkedOpenCustomHashSet<ItemStack> set = new ObjectLinkedOpenCustomHashSet<>(SAME) {
            @Override
            public boolean remove(Object k) {
                boolean removed = super.remove(k);
                if (removed && k instanceof ItemStack stack) {
                    CreativeModeTab.TabVisibility visibility = event.getEntries().get(stack);
                    if (visibility == CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS) {
                        event.getEntries().put(stack, parent ? CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY : CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                    } else if (visibility != null) {
                        event.getEntries().remove(stack);
                    }
                }
                return removed;
            }
        };
        for (Map.Entry<ItemStack, CreativeModeTab.TabVisibility> entry : event.getEntries()) {
            if (in(entry.getValue(), parent)) {
                set.add(entry.getKey());
            }
        }
        return set;
    }
}
