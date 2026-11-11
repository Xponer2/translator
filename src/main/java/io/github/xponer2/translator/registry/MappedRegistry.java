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

import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A registry of a 1.21 object type that 1.12.2 has no registry for (block entity types, menu types, recipe types,
 * data component types, ...). Filled by {@link DeferredRegister}, in registration order.
 */
public final class MappedRegistry<T> implements Registry<T> {
    private static final ResourceLocation ENTITY_DATA_SERIALIZERS = new ResourceLocation("neoforge", "entity_data_serializers");

    private final ResourceKey<? extends Registry<T>> key;
    private final Map<ResourceLocation, T> byLocation = new LinkedHashMap<>();
    private final Map<T, ResourceLocation> byValue = new IdentityHashMap<>();

    public MappedRegistry(ResourceKey<? extends Registry<T>> key) {
        this.key = key;
        this.fromData = io.github.xponer2.translator.world.DataFeatures.isDataRegistry(key.location());
    }

    /** Whether the id is registered already (a data registry isn't asked to read it). */
    public boolean hasEntry(ResourceLocation location) {
        return this.byLocation.containsKey(location);
    }

    /** The registered value (a data registry doesn't read a file for it). */
    @Nullable
    public T direct(ResourceLocation location) {
        return this.byLocation.get(location);
    }

    public synchronized T register(ResourceLocation location, T value) {
        if (this.byLocation.containsKey(location)) {
            throw new IllegalStateException("Duplicate id " + location + " in " + this.key.location());
        }
        if (this.byValue.containsKey(value)) {
            throw new IllegalStateException("Object registered twice in " + this.key.location() + ": " + location + " and " + this.byValue.get(value));
        }
        this.byLocation.put(location, value);
        this.byValue.put(value, location);
        if (ENTITY_DATA_SERIALIZERS.equals(this.key.location())) {
            // a mod's entity data serializer: 1.12.2 refuses data of one it doesn't know; numbered in registration order,
            // the same on both sides
            io.github.xponer2.translator.entity.DataSerializerDefaults.register(value);
        }
        if (value instanceof RegistrationCallback callback) {
            this.pending.put(location, callback);
        }
        return value;
    }

    /**
     * The callbacks of entries registered straight into the registry or in a RegisterEvent (Refurbished's block entity
     * types, whose classes 1.12.2 then couldn't save), run once registration is done (RegistryCallbacks.bakeAll): the
     * mod's static fields are set then. A DeferredRegister takes its own and runs them once its holder is bound.
     */
    private final java.util.Map<ResourceLocation, RegistrationCallback> pending = new java.util.LinkedHashMap<>();

    @Nullable
    synchronized RegistrationCallback takePending(ResourceLocation location) {
        return this.pending.remove(location);
    }

    void runPendingCallbacks() {
        java.util.List<java.util.Map.Entry<ResourceLocation, RegistrationCallback>> todo;
        synchronized (this) {
            todo = new java.util.ArrayList<>(this.pending.entrySet());
            this.pending.clear();
        }
        for (var entry : todo) {
            runCallback(entry.getKey(), entry.getValue());
        }
    }

    static void runCallback(ResourceLocation location, RegistrationCallback callback) {
        try {
            callback.onRegistered(location);
        } catch (RuntimeException | LinkageError e) {
            if (!io.github.xponer2.translator.runtime.RuntimeMods.isRuntimeNamespace(location.getNamespace())) {
                throw e;
            }
            // a block entity type whose block entity can't be made yet, a tab whose list fails: skipped, the game goes on
            io.github.xponer2.translator.runtime.RuntimeMods.skipped(location, e);
        }
    }

    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return this.key;
    }

    @Nullable
    @Override
    public ResourceLocation getKey(T value) {
        ResourceLocation key = this.byValue.get(value);
        if (key == null && value instanceof net.minecraft.world.biome.Biome biome) {
            // a 1.12.2 biome in the registry of the mods' data biomes (JourneyMap names the biome the player is in):
            // its own Forge name, as 1.21's registry has every vanilla biome
            return biome.getRegistryName();
        }
        return key;
    }

    @Nullable
    @Override
    public T get(@Nullable ResourceLocation location) {
        if (location == null) {
            return null;
        }
        T value = this.byLocation.get(location);
        return value != null || !this.fromData ? value : this.fromData(location);
    }

    @Override
    public boolean containsKey(ResourceLocation location) {
        return this.byLocation.containsKey(location) || this.fromData && this.get(location) != null;
    }

    /**
     * Whether the entries of this registry are the mods' data files, read on first use (configured and placed features:
     * {@link io.github.xponer2.translator.world.DataFeatures}). Those not asked for yet aren't in {@link #keySet}.
     */
    private final boolean fromData;
    private final Set<ResourceLocation> loading = new java.util.HashSet<>();
    private final Set<ResourceLocation> missing = new java.util.HashSet<>();

    @Nullable
    @SuppressWarnings("unchecked")
    private synchronized T fromData(ResourceLocation location) {
        T value = this.byLocation.get(location);
        if (value != null || this.missing.contains(location) || !this.loading.add(location)) {
            return value;
        }
        try {
            value = (T) io.github.xponer2.translator.world.DataFeatures.decode(this.key.location(), location);
        } finally {
            this.loading.remove(location);
        }
        if (value == null) {
            this.missing.add(location);
        } else if (!this.byLocation.containsKey(location)) {
            this.register(location, value);
        }
        return this.byLocation.get(location);
    }

    @Override
    public Set<ResourceLocation> keySet() {
        return Collections.unmodifiableSet(this.byLocation.keySet());
    }

    @Override
    public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        var entries = new LinkedHashSet<Map.Entry<ResourceKey<T>, T>>();
        this.byLocation.forEach((location, value) -> entries.add(new AbstractMap.SimpleImmutableEntry<>(ResourceKey.create(this.key, location), value)));
        return entries;
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableCollection(this.byLocation.values()).iterator();
    }

    @Override
    public String toString() {
        return "MappedRegistry[" + this.key.location() + "]";
    }
}
