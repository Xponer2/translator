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

    public static final Registry<Item> ITEM = forge(Registries.ITEM, ForgeRegistries.ITEMS);
    public static final Registry<Block> BLOCK = forge(Registries.BLOCK, ForgeRegistries.BLOCKS);
    public static final Registry<Enchantment> ENCHANTMENT = forge(Registries.ENCHANTMENT, ForgeRegistries.ENCHANTMENTS);
    public static final Registry<SoundEvent> SOUND_EVENT = forge(Registries.SOUND_EVENT, ForgeRegistries.SOUND_EVENTS);
    public static final Registry<Potion> MOB_EFFECT = forge(Registries.MOB_EFFECT, ForgeRegistries.POTIONS);
    public static final Registry<EntityType<?>> ENTITY_TYPE = put(Registries.ENTITY_TYPE, new EntityTypeRegistry());
    public static final Registry<io.github.xponer2.translator.menu.MenuType<?>> MENU = byKey(Registries.MENU);
    public static final Registry<io.github.xponer2.translator.recipe.RecipeSerializer<?>> RECIPE_SERIALIZER = byKey(Registries.RECIPE_SERIALIZER);
    public static final Registry<io.github.xponer2.translator.recipe.RecipeType<?>> RECIPE_TYPE = byKey(Registries.RECIPE_TYPE);

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
     * The registry of a key; registries 1.12.2 doesn't have are created on first use.
     */
    @SuppressWarnings("unchecked")
    public static <T> Registry<T> byKey(ResourceKey<? extends Registry<T>> key) {
        return (Registry<T>) REGISTRIES.computeIfAbsent(key.location(), location -> new MappedRegistry<>(key));
    }
}
