/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ladder.carried.net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * 1.20.5's stack patch API over the store (ComponentStore): the patch is what the stack has that its item hasn't (a
 * value other than the item's, or the item's value removed), made from the stack's data each time.
 */
public final class ComponentPatches {
    private ComponentPatches() {
    }

    /** 1.20.5's {@code ItemStack.getComponentsPatch()} */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DataComponentPatch getComponentsPatch(ItemStack stack) {
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        if (stack.isEmpty()) {
            return builder.build();
        }
        java.util.Set<DataComponentType<?>> types = new java.util.LinkedHashSet<>(DataComponents.ALL);
        types.addAll(ItemDefaults.of(stack.getItem()).keySet());
        types.addAll(ComponentStore.getComponents(stack).keySet());
        for (DataComponentType type : types) {
            Object value = ComponentStore.read(stack, type);
            Object prototype = ComponentStore.prototype(stack, type);
            if (value == null) {
                if (prototype != null) {
                    builder.remove(type);
                }
            } else if (!Objects.equals(value, prototype)) {
                builder.set(type, value);
            }
        }
        return builder.build();
    }

    /** 1.20.5's {@code ItemStack.isComponentsPatchEmpty()} */
    public static boolean isComponentsPatchEmpty(ItemStack stack) {
        return getComponentsPatch(stack).isEmpty();
    }

    /** 1.20.5's {@code ItemStack.applyComponents(patch)}: each value set, each removal removed */
    public static void applyComponents(ItemStack stack, DataComponentPatch patch) {
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
            if (entry.getValue().isPresent()) {
                ComponentStore.set(stack, entry.getKey(), entry.getValue().get());
            } else {
                ComponentStore.remove(stack, entry.getKey());
            }
        }
    }

    /** 1.20.5's {@code ItemStack.applyComponents(map)}: each value set */
    public static void applyComponents(ItemStack stack, DataComponentMap components) {
        for (TypedDataComponent<?> component : components) {
            ComponentStore.set(stack, component.type(), component.value());
        }
    }

    /** 1.20.5's {@code ItemStack.applyComponentsAndValidate(patch)}: 1.20.4 has no stack validation to run */
    public static void applyComponentsAndValidate(ItemStack stack, DataComponentPatch patch) {
        applyComponents(stack, patch);
    }

    /** new ItemStack(item, count, patch) */
    public static ItemStack create(Holder<Item> item, int count, DataComponentPatch patch) {
        ItemStack stack = new ItemStack(item.value(), count);
        applyComponents(stack, patch);
        return stack;
    }

    /** 1.20.5's {@code ItemStack.transmuteCopy(item, count)}: another item with this stack's data */
    public static ItemStack transmuteCopy(ItemStack stack, ItemLike item, int count) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = new ItemStack(item, count);
        applyComponents(copy, getComponentsPatch(stack));
        return copy;
    }

    /** 1.20.5's {@code ItemStack.transmuteCopy(item)}: the same count */
    public static ItemStack transmuteCopy(ItemStack stack, ItemLike item) {
        return transmuteCopy(stack, item, stack.getCount());
    }
}
