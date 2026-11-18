/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.item.Item;

/**
 * The items' own component values (1.20.5's {@code Item.Properties.component(type, value)}, an item's prototype): the
 * properties gather them, the item takes them when it is made (mixin.ItemMixin).
 */
public final class ItemDefaults {
    private static final Map<Item.Properties, Map<DataComponentType<?>, Object>> BY_PROPERTIES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Item, DataComponentMap> BY_ITEM = Collections.synchronizedMap(new WeakHashMap<>());

    private ItemDefaults() {
    }

    /** 1.20.5's {@code Item.Properties.component(type, value)} */
    public static Item.Properties component(Item.Properties properties, DataComponentType<?> type, Object value) {
        BY_PROPERTIES.computeIfAbsent(properties, p -> new LinkedHashMap<>()).put(type, value);
        return properties;
    }

    /** a component the properties have been given so far (the item isn't made yet), or null */
    public static Object pending(Item.Properties properties, DataComponentType<?> type) {
        Map<DataComponentType<?>, Object> values = BY_PROPERTIES.get(properties);
        return values == null ? null : values.get(type);
    }

    /** an item is made of its properties: their components are its own */
    public static void bind(Item item, Item.Properties properties) {
        Map<DataComponentType<?>, Object> values = BY_PROPERTIES.get(properties);
        if (values != null) {
            BY_ITEM.put(item, new DataComponentMap.Snapshot(Map.copyOf(values)));
        }
    }

    /** an item's own components (1.20.5's {@code Item.components()}; the game's own aren't in it: DataComponents) */
    public static DataComponentMap of(Item item) {
        return BY_ITEM.getOrDefault(item, DataComponentMap.EMPTY);
    }
}
