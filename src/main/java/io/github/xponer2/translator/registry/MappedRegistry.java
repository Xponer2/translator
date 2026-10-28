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
    private final ResourceKey<? extends Registry<T>> key;
    private final Map<ResourceLocation, T> byLocation = new LinkedHashMap<>();
    private final Map<T, ResourceLocation> byValue = new IdentityHashMap<>();

    public MappedRegistry(ResourceKey<? extends Registry<T>> key) {
        this.key = key;
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
        return value;
    }

    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return this.key;
    }

    @Nullable
    @Override
    public ResourceLocation getKey(T value) {
        return this.byValue.get(value);
    }

    @Nullable
    @Override
    public T get(@Nullable ResourceLocation location) {
        return location == null ? null : this.byLocation.get(location);
    }

    @Override
    public boolean containsKey(ResourceLocation location) {
        return this.byLocation.containsKey(location);
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
