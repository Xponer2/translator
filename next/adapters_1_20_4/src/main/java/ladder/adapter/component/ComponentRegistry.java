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
package ladder.adapter.component;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * 1.20.5's registry of component types ({@code Registries.DATA_COMPONENT_TYPE}, {@code BuiltInRegistries.
 * DATA_COMPONENT_TYPE}) on 1.20.4: a NeoForge registry of the same name, synced to clients, made when the game makes
 * its registries; the game's own components (DataComponents) are registered in it, mods register theirs with their
 * DeferredRegisters as on 1.21.
 */
public final class ComponentRegistry {
    public static final ResourceKey<Registry<DataComponentType<?>>> KEY =
            ResourceKey.createRegistryKey(new ResourceLocation("minecraft", "data_component_type"));
    private static Registry<DataComponentType<?>> registry;

    private ComponentRegistry() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(NewRegistryEvent.class, event -> registry = event.create(new RegistryBuilder<>(KEY).sync(true)));
        modBus.addListener(RegisterEvent.class, event -> event.register(KEY, helper -> {
            for (DataComponentType<?> type : DataComponents.ALL) {
                helper.register(new ResourceLocation("minecraft", ((DataComponents.Game<?>) type).name), type);
            }
        }));
    }

    /** 1.20.5's {@code Registries.DATA_COMPONENT_TYPE} */
    public static ResourceKey<Registry<DataComponentType<?>>> key() {
        return KEY;
    }

    /** 1.20.5's {@code BuiltInRegistries.DATA_COMPONENT_TYPE} */
    public static Registry<DataComponentType<?>> registry() {
        if (registry == null) {
            throw new IllegalStateException("the component type registry is made with the game's registries; it was asked for earlier");
        }
        return registry;
    }

    /** a type's registered name (for messages and the stack's data) */
    public static String name(DataComponentType<?> type) {
        ResourceLocation id = registry == null ? null : registry.getKey(type);
        return id == null ? "unregistered component " + System.identityHashCode(type) : id.toString();
    }

    /** 1.20.5's {@code Item.components()}: the game's own values of the item, and its own components */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DataComponentMap components(Item item) {
        ItemStack fresh = new ItemStack(item);
        java.util.Map<DataComponentType<?>, Object> values = new java.util.LinkedHashMap<>();
        for (DataComponentType type : DataComponents.ALL) {
            Object value = type.vanilla().prototype(fresh);
            if (value != null) {
                values.put(type, value);
            }
        }
        ItemDefaults.of(item).forEach(c -> values.put(c.type(), c.value()));
        return new DataComponentMap.Snapshot(values);
    }
}
