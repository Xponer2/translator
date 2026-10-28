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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The id of an object in a registry (1.21 {@code net.minecraft.resources.ResourceKey}): the registry's id plus the
 * object's id. Keys are interned, so they can be compared with {@code ==}.
 */
public final class ResourceKey<T> {
    private static final ResourceLocation ROOT = new ResourceLocation("minecraft", "root");
    private static final Map<String, ResourceKey<?>> KEYS = new ConcurrentHashMap<>();

    private final ResourceLocation registryName;
    private final ResourceLocation location;

    private ResourceKey(ResourceLocation registryName, ResourceLocation location) {
        this.registryName = registryName;
        this.location = location;
    }

    @SuppressWarnings("unchecked")
    private static <T> ResourceKey<T> intern(ResourceLocation registryName, ResourceLocation location) {
        return (ResourceKey<T>) KEYS.computeIfAbsent(registryName + " " + location, k -> new ResourceKey<>(registryName, location));
    }

    public static <T> ResourceKey<T> create(ResourceKey<? extends Registry<T>> registry, ResourceLocation location) {
        return intern(registry.location, location);
    }

    public static <T> ResourceKey<Registry<T>> createRegistryKey(ResourceLocation location) {
        return intern(ROOT, location);
    }

    public ResourceLocation location() {
        return this.location;
    }

    public ResourceLocation registry() {
        return this.registryName;
    }

    public boolean isFor(ResourceKey<? extends Registry<?>> registry) {
        return this.registryName.equals(registry.location());
    }

    @SuppressWarnings("unchecked")
    public <E> java.util.Optional<ResourceKey<E>> cast(ResourceKey<? extends Registry<E>> registry) {
        return this.isFor(registry) ? java.util.Optional.of((ResourceKey<E>) this) : java.util.Optional.empty();
    }

    @Override
    public String toString() {
        return "ResourceKey[" + this.registryName + " / " + this.location + "]";
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ResourceKey<?> other && this.registryName.equals(other.registryName) && this.location.equals(other.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.registryName, this.location);
    }
}
