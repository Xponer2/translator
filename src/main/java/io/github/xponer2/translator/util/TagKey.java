package io.github.xponer2.translator.util;

import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.registry.ResourceKey;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The id of a tag of a registry (1.21 {@code net.minecraft.tags.TagKey}). Keys are interned.
 */
public final class TagKey<T> {
    private static final Map<String, TagKey<?>> KEYS = new ConcurrentHashMap<>();

    private final ResourceKey<? extends Registry<T>> registry;
    private final ResourceLocation location;

    private TagKey(ResourceKey<? extends Registry<T>> registry, ResourceLocation location) {
        this.registry = registry;
        this.location = location;
    }

    @SuppressWarnings("unchecked")
    public static <T> TagKey<T> create(ResourceKey<? extends Registry<T>> registry, ResourceLocation location) {
        return (TagKey<T>) KEYS.computeIfAbsent(registry.location() + " " + location, k -> new TagKey<>(registry, location));
    }

    public ResourceKey<? extends Registry<T>> registry() {
        return this.registry;
    }

    public ResourceLocation location() {
        return this.location;
    }

    public boolean isFor(ResourceKey<? extends Registry<?>> registry) {
        return this.registry == registry;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TagKey<?> other && this.registry.equals(other.registry) && this.location.equals(other.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.registry, this.location);
    }

    @Override
    public String toString() {
        return "TagKey[" + this.registry.location() + " / " + this.location + "]";
    }
}
