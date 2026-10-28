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
package io.github.xponer2.translator.registry;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A 1.21 registry view of a 1.12.2 Forge registry (items, blocks, enchantments, sounds, ...).
 */
public final class ForgeRegistryWrapper<T extends IForgeRegistryEntry<T>> implements Registry<T> {
    private final ResourceKey<? extends Registry<T>> key;
    private final IForgeRegistry<T> registry;

    public ForgeRegistryWrapper(ResourceKey<? extends Registry<T>> key, IForgeRegistry<T> registry) {
        this.key = key;
        this.registry = registry;
    }

    public IForgeRegistry<T> forge() {
        return this.registry;
    }

    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return this.key;
    }

    @Nullable
    @Override
    public ResourceLocation getKey(T value) {
        var location = value.getRegistryName();
        // a registry name is set before registration, only a registered object has a key
        return location != null && this.registry.getValue(location) == value ? location : null;
    }

    @Nullable
    @Override
    public T get(@Nullable ResourceLocation location) {
        if (location != null && !this.registry.containsKey(location)) {
            var alias = Aliases.get(this.key.location(), location);
            if (alias != null) {
                location = alias;
            }
        }
        return location == null || !this.registry.containsKey(location) ? this.defaultValue() : this.registry.getValue(location);
    }

    // 1.21 BuiltInRegistries.BLOCK and ITEM are defaulted registries: air for unknown ids (the 1.12.2 item registry
    // has no default, the block one returns air too)
    @Nullable
    @SuppressWarnings("unchecked")
    private T defaultValue() {
        var type = this.registry.getRegistrySuperType();
        if (type == net.minecraft.item.Item.class) {
            return (T) net.minecraft.init.Items.AIR;
        }
        if (type == net.minecraft.block.Block.class) {
            return (T) net.minecraft.init.Blocks.AIR;
        }
        return null;
    }

    @Override
    public boolean containsKey(ResourceLocation location) {
        return this.registry.containsKey(location);
    }

    @Override
    public Set<ResourceLocation> keySet() {
        return this.registry.getKeys();
    }

    @Override
    public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        var entries = new LinkedHashSet<Map.Entry<ResourceKey<T>, T>>();
        for (var entry : this.registry.getEntries()) {
            entries.add(new AbstractMap.SimpleImmutableEntry<>(ResourceKey.create(this.key, entry.getKey()), entry.getValue()));
        }
        return entries;
    }

    @Override
    public Iterator<T> iterator() {
        return this.registry.iterator();
    }

    @Override
    public String toString() {
        return "ForgeRegistryWrapper[" + this.key.location() + "]";
    }
}
