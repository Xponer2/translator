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

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A registry of objects by id (1.21 {@code net.minecraft.core.Registry}). Implemented by {@link ForgeRegistryWrapper}
 * for the registries 1.12.2 has and by {@link MappedRegistry} for the 1.21 ones it doesn't have.
 */
public interface Registry<T> extends Iterable<T> {
    /**
     * 1.21 {@code Registry.register}: into a registry the Translator keeps (MappedRegistry) at once; a 1.12.2 Forge
     * registry (items, blocks) takes the entry, named, while it is open, else the entry stays unregistered (logged).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static <V, E extends V> E register(Registry<V> registry, ResourceLocation location, E value) {
        if (registry instanceof MappedRegistry) {
            ((MappedRegistry<V>) registry).register(location, value);
        } else if (registry instanceof ForgeRegistryWrapper && value instanceof net.minecraftforge.registries.IForgeRegistryEntry) {
            var entry = (net.minecraftforge.registries.IForgeRegistryEntry) value;
            try {
                if (entry.getRegistryName() == null) {
                    entry.setRegistryName(location);
                }
                ((net.minecraftforge.registries.IForgeRegistry) ((ForgeRegistryWrapper) registry).forge()).register(entry);
            } catch (RuntimeException e) {
                org.apache.logging.log4j.LogManager.getLogger("Translator").warn("Can't register {} in {}: {}", location, registry.key().location(), e.toString());
            }
        }
        return value;
    }

    static <V, E extends V> E register(Registry<V> registry, String name, E value) {
        return register(registry, new ResourceLocation(name), value);
    }

    static <V, E extends V> E register(Registry<V> registry, ResourceKey<V> key, E value) {
        return register(registry, key.location(), value);
    }

    static <V> Reference<V> registerForHolder(Registry<V> registry, ResourceLocation location, V value) {
        register(registry, location, value);
        return new Reference<>(ResourceKey.create(registry.key(), location), value);
    }

    static <V> Reference<V> registerForHolder(Registry<V> registry, ResourceKey<V> key, V value) {
        register(registry, key.location(), value);
        return new Reference<>(key, value);
    }

    /** NeoForge {@code addCallback}: a bake callback runs once registration is done (RegistryCallbacks). */
    default void addCallback(Object callback) {
        RegistryCallbacks.add(this, callback);
    }

    default void addCallback(Class<?> type, Object callback) {
        RegistryCallbacks.add(this, callback);
    }

    /** 1.21 {@code WritableRegistry.register(key, value, info)} (WritableRegistry is this interface here). */
    default Reference<T> register(ResourceKey<T> key, T value, RegistrationInfo info) {
        return registerForHolder(this, key, value);
    }

    ResourceKey<? extends Registry<T>> key();

    @Nullable
    ResourceLocation getKey(T value);

    @Nullable
    T get(@Nullable ResourceLocation location);

    boolean containsKey(ResourceLocation location);

    Set<ResourceLocation> keySet();

    Set<Map.Entry<ResourceKey<T>, T>> entrySet();

    @Nullable
    default T get(@Nullable ResourceKey<T> key) {
        return key == null ? null : this.get(key.location());
    }

    /** 1.21 {@code HolderGetter.getOrThrow}: the holder of a registered object (a runtime mod's registry lookup is this registry). */
    default Reference<T> getOrThrow(ResourceKey<T> key) {
        var value = this.get(key.location());
        if (value == null) {
            throw new IllegalStateException("Missing element " + key);
        }
        return new Reference<>(key, value);
    }

    default Optional<T> getOptional(@Nullable ResourceLocation location) {
        return Optional.ofNullable(this.get(location));
    }

    default Optional<ResourceKey<T>> getResourceKey(T value) {
        var location = this.getKey(value);
        return location == null ? Optional.empty() : Optional.of(ResourceKey.create(this.key(), location));
    }

    default boolean containsValue(T value) {
        return this.getKey(value) != null;
    }

    default Optional<Holder<T>> getHolder(ResourceLocation location) {
        var value = this.get(location);
        return value == null ? Optional.empty() : Optional.of(new Reference<>(ResourceKey.create(this.key(), location), value));
    }

    default Holder<T> wrapAsHolder(T value) {
        var location = this.getKey(value);
        return location == null ? Holder.direct(value) : new Reference<>(ResourceKey.create(this.key(), location), value);
    }

    default Stream<T> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    default Codec<T> byNameCodec() {
        return Codec.STRING.comapFlatMap(s -> {
            ResourceLocation location;
            try {
                location = io.github.xponer2.translator.util.ResourceLocations.parse(s);
            } catch (RuntimeException e) {
                return DataResult.error("Not a valid resource location: " + s);
            }
            var value = this.get(location);
            return value != null ? DataResult.success(value) : DataResult.error("Unknown registry key in " + this.key().location() + ": " + s);
        }, value -> {
            var location = this.getKey(value);
            if (location == null) {
                throw new IllegalStateException("Unregistered object in " + this.key().location() + ": " + value);
            }
            return location.toString();
        });
    }

    // --- 1.21 lookups (HolderLookup.RegistryLookup, Registry): over the same entries ----------------------------------

    default Reference<T> getHolderOrThrow(ResourceKey<T> key) {
        return this.getOrThrow(key);
    }

    default Optional<Reference<T>> getHolder(ResourceKey<T> key) {
        var value = key == null ? null : this.get(key.location());
        return value == null ? Optional.empty() : Optional.of(new Reference<>(key, value));
    }

    default Optional<T> getOptional(@Nullable ResourceKey<T> key) {
        return Optional.ofNullable(this.get(key));
    }

    /** The registry as its own lookup (1.21 {@code asLookup()}, a HolderLookup.RegistryLookup is a Registry here). */
    default Registry<T> asLookup() {
        return this;
    }

    default Registry<T> filterElements(java.util.function.Predicate<? super T> filter) {
        return this;
    }

    default Codec<Holder<T>> holderByNameCodec() {
        return this.byNameCodec().xmap(this::wrapAsHolder, Holder::value);
    }

    default Stream<Reference<T>> listElements() {
        return this.keySet().stream().map(location -> new Reference<>(ResourceKey.create(this.key(), location), this.get(location)));
    }

    default Stream<Reference<T>> holders() {
        return this.listElements();
    }

    /** The members of an item or block tag (the tag files of the mods), as holders. */
    @SuppressWarnings("unchecked")
    default Iterable<Holder<T>> getTagOrEmpty(io.github.xponer2.translator.util.TagKey<T> tag) {
        var holders = new java.util.ArrayList<Holder<T>>();
        for (Object member : io.github.xponer2.translator.tags.TagManager.conditionContext().getTag(tag)) {
            holders.add(this.wrapAsHolder((T) member));
        }
        return holders;
    }

    default Stream<io.github.xponer2.translator.util.TagKey<T>> getTagNames() {
        return Stream.empty();
    }

    @Nullable
    default ResourceLocation getDefaultKey() {
        return null;
    }

    /** The numeric id: the place in the registry's order (1.12.2 Forge registries: their ids). */
    default int getId(@Nullable T value) {
        int id = 0;
        for (T entry : this) {
            if (entry == value) {
                return id;
            }
            id++;
        }
        return -1;
    }

    @Nullable
    default T byId(int id) {
        int index = 0;
        for (T entry : this) {
            if (index++ == id) {
                return entry;
            }
        }
        return null;
    }

    default Optional<Reference<T>> getRandom(java.util.Random random) {
        var keys = new java.util.ArrayList<>(this.keySet());
        if (keys.isEmpty()) {
            return Optional.empty();
        }
        var location = keys.get(random.nextInt(keys.size()));
        return Optional.of(new Reference<>(ResourceKey.create(this.key(), location), this.get(location)));
    }

    default Reference<T> createIntrusiveHolder(T value) {
        return new Reference<>(null, value, this);
    }

    default void addAlias(ResourceLocation from, ResourceLocation to) {
    }

    /**
     * A holder of a registered object.
     */
    class Reference<T> implements Holder<T> {
        @Nullable
        private ResourceKey<T> key;
        private final T value;
        @Nullable
        private final Registry<T> owner;

        public Reference(ResourceKey<T> key, T value) {
            this(key, value, null);
        }

        /** A holder whose key is looked up when asked (an object holds its holder before it is registered). */
        Reference(@Nullable ResourceKey<T> key, T value, @Nullable Registry<T> owner) {
            this.key = key;
            this.value = value;
            this.owner = owner;
        }

        public ResourceKey<T> key() {
            if (this.key == null && this.owner != null) {
                var location = this.owner.getKey(this.value);
                if (location != null) {
                    this.key = ResourceKey.create(this.owner.key(), location);
                }
            }
            return this.key;
        }

        @Override
        public T value() {
            return this.value;
        }

        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public Optional<ResourceKey<T>> unwrapKey() {
            return Optional.ofNullable(this.key());
        }

        @Override
        public String toString() {
            return "Reference{" + this.key() + "=" + this.value + "}";
        }
    }
}
