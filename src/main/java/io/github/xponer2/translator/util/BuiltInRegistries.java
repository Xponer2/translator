/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.util;

import io.github.xponer2.translator.registry.EntityType;
import io.github.xponer2.translator.registry.EntityTypeRegistry;
import io.github.xponer2.translator.registry.ForgeRegistryWrapper;
import io.github.xponer2.translator.registry.MappedRegistry;
import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.registry.ResourceKey;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The registries (1.21 {@code net.minecraft.core.registries.BuiltInRegistries}): the 1.12.2 Forge registries where
 * 1.12.2 has one, a {@link MappedRegistry} for every other registry key.
 */
public final class BuiltInRegistries {
    private static final Map<ResourceLocation, Registry<?>> REGISTRIES = new ConcurrentHashMap<>();

    /**
     * The registry of registries (1.21 {@code BuiltInRegistries.REGISTRY}): the registries here by their keys. A mod's
     * own registry registered into it (Create's) is found by its key afterwards, like the others.
     */
    public static final Registry<Registry<?>> REGISTRY = new RootRegistry();

    public static final Registry<Item> ITEM = forge(Registries.ITEM, ForgeRegistries.ITEMS);
    public static final Registry<Block> BLOCK = forge(Registries.BLOCK, ForgeRegistries.BLOCKS);
    public static final Registry<Enchantment> ENCHANTMENT = forge(Registries.ENCHANTMENT, ForgeRegistries.ENCHANTMENTS);
    public static final Registry<SoundEvent> SOUND_EVENT = forge(Registries.SOUND_EVENT, ForgeRegistries.SOUND_EVENTS);
    public static final Registry<Potion> MOB_EFFECT = forge(Registries.MOB_EFFECT, ForgeRegistries.POTIONS);
    public static final Registry<EntityType<?>> ENTITY_TYPE = put(Registries.ENTITY_TYPE, new EntityTypeRegistry());
    public static final Registry<io.github.xponer2.translator.menu.MenuType<?>> MENU = byKey(Registries.MENU);
    public static final Registry<io.github.xponer2.translator.recipe.RecipeSerializer<?>> RECIPE_SERIALIZER = byKey(Registries.RECIPE_SERIALIZER);
    public static final Registry<io.github.xponer2.translator.recipe.RecipeType<?>> RECIPE_TYPE = byKey(Registries.RECIPE_TYPE);
    /** Advancement trigger types: kept, 1.12.2 advancements don't use them. */
    public static final Registry<Object> TRIGGER_TYPES = byKey(ResourceKey.createRegistryKey(new ResourceLocation("trigger_type")));

    private BuiltInRegistries() { }

    private static <T> Registry<T> put(ResourceKey<Registry<T>> key, Registry<T> registry) {
        REGISTRIES.put(key.location(), registry);
        return registry;
    }

    private static <T extends IForgeRegistryEntry<T>> Registry<T> forge(ResourceKey<Registry<T>> key, IForgeRegistry<T> registry) {
        var wrapper = new ForgeRegistryWrapper<>(key, registry);
        REGISTRIES.put(key.location(), wrapper);
        return wrapper;
    }

    /**
     * The 1.21 key of a 1.12.2 Forge registry (items -> minecraft:item), null when no 1.21 registry wraps it.
     */
    @javax.annotation.Nullable
    public static ResourceKey<? extends Registry<?>> keyOf(net.minecraftforge.registries.IForgeRegistry<?> forge) {
        for (Registry<?> registry : REGISTRIES.values()) {
            if (registry instanceof io.github.xponer2.translator.registry.ForgeRegistryWrapper<?> wrapper && wrapper.forge() == forge) {
                return registry.key();
            }
        }
        return null;
    }

    /** The registries the Translator keeps (1.12.2 has no registry for them), in no particular order. */
    public static java.util.List<MappedRegistry<?>> mappedRegistries() {
        var list = new java.util.ArrayList<MappedRegistry<?>>();
        for (Registry<?> registry : REGISTRIES.values()) {
            if (registry instanceof MappedRegistry<?> mapped) {
                list.add(mapped);
            }
        }
        return list;
    }

    /**
     * The registry of a key; registries 1.12.2 doesn't have are created on first use.
     */
    @SuppressWarnings("unchecked")
    public static <T> Registry<T> byKey(ResourceKey<? extends Registry<T>> key) {
        return (Registry<T>) REGISTRIES.computeIfAbsent(key.location(), location -> new MappedRegistry<>(key));
    }

    private static final class RootRegistry implements Registry<Registry<?>> {
        private static final ResourceKey<Registry<Registry<?>>> KEY = ResourceKey.createRegistryKey(new ResourceLocation("root"));

        @Override
        public ResourceKey<? extends Registry<Registry<?>>> key() {
            return KEY;
        }

        @Override
        public ResourceLocation getKey(Registry<?> value) {
            return value == null ? null : value.key().location();
        }

        @Override
        public Registry<?> get(@javax.annotation.Nullable ResourceLocation location) {
            return location == null ? null : REGISTRIES.get(location);
        }

        @Override
        public boolean containsKey(ResourceLocation location) {
            return REGISTRIES.containsKey(location);
        }

        @Override
        public java.util.Set<ResourceLocation> keySet() {
            return java.util.Collections.unmodifiableSet(REGISTRIES.keySet());
        }

        @Override
        public java.util.Set<Map.Entry<ResourceKey<Registry<?>>, Registry<?>>> entrySet() {
            var entries = new java.util.LinkedHashSet<Map.Entry<ResourceKey<Registry<?>>, Registry<?>>>();
            REGISTRIES.forEach((location, registry) -> entries.add(new java.util.AbstractMap.SimpleImmutableEntry<>(ResourceKey.create(KEY, location), registry)));
            return entries;
        }

        @Override
        public java.util.Iterator<Registry<?>> iterator() {
            return java.util.Collections.unmodifiableCollection(REGISTRIES.values()).iterator();
        }

        @Override
        public Registry.Reference<Registry<?>> register(ResourceKey<Registry<?>> key, Registry<?> value, io.github.xponer2.translator.registry.RegistrationInfo info) {
            REGISTRIES.put(key.location(), value);
            return new Registry.Reference<>(key, value);
        }
    }
}
