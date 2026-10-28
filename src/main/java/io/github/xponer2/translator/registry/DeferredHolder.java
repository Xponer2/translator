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

import java.util.Optional;
import java.util.function.Supplier;

/**
 * An object of a {@link DeferredRegister} (1.21 NeoForge {@code DeferredHolder}): known by id from the start, the
 * object itself exists once the registry was filled.
 *
 * @param <R> the registry type
 * @param <T> the type of this object
 */
public class DeferredHolder<R, T extends R> implements Holder<R>, Supplier<T> {
    private final ResourceKey<R> key;
    private T value;
    // registers the object on first use (registries without a Forge registry event, see DeferredRegister)
    private Runnable resolver;
    // the resolvers by key: holders made by create() for an object of a DeferredRegister (e.g. in an API) use them too
    private static final java.util.Map<ResourceKey<?>, Runnable> RESOLVERS = new java.util.concurrent.ConcurrentHashMap<>();

    protected DeferredHolder(ResourceKey<R> key) {
        this.key = key;
    }

    void setResolver(Runnable resolver) {
        this.resolver = resolver;
        RESOLVERS.put(this.key, resolver);
    }

    public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<? extends Registry<R>> registry, ResourceLocation location) {
        return new DeferredHolder<>(ResourceKey.create(registry, location));
    }

    void bind(T value) {
        if (this.value != null && this.value != value) {
            throw new IllegalStateException("Already bound: " + this.key);
        }
        this.value = value;
    }

    @Override
    public T get() {
        if (this.value == null && this.resolver != null) {
            this.resolver.run();
        }
        if (this.value == null) {
            // a holder made by create(): looked up in its registry like NeoForge does (registered objects only, the
            // block and item registries return air for unknown ids)
            this.lookup(true);
        }
        if (this.value == null) {
            throw new NullPointerException("Trying to access unbound value: " + this.key);
        }
        return this.value;
    }

    @SuppressWarnings("unchecked")
    private void lookup(boolean resolve) {
        Registry<R> registry = io.github.xponer2.translator.util.BuiltInRegistries.byKey(ResourceKey.createRegistryKey(this.key.registry()));
        if (resolve && !registry.containsKey(this.key.location()) && RESOLVERS.get(this.key) != null) {
            RESOLVERS.get(this.key).run();
        }
        if (registry.containsKey(this.key.location())) {
            this.value = (T) registry.get(this.key.location());
        }
    }

    @Override
    public R value() {
        return this.get();
    }

    @Override
    public boolean isBound() {
        if (this.value == null) {
            this.lookup(false);
        }
        return this.value != null;
    }

    public ResourceLocation getId() {
        return this.key.location();
    }

    public ResourceKey<R> getKey() {
        return this.key;
    }

    @Override
    public Optional<ResourceKey<R>> unwrapKey() {
        return Optional.of(this.key);
    }

    public Optional<T> asOptional() {
        return Optional.ofNullable(this.value);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof DeferredHolder<?, ?> other && this.key == other.key;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public String toString() {
        return "DeferredHolder{" + this.key + "}";
    }
}
