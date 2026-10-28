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

    /**
     * A holder of a registered object.
     */
    final class Reference<T> implements Holder<T> {
        private final ResourceKey<T> key;
        private final T value;

        public Reference(ResourceKey<T> key, T value) {
            this.key = key;
            this.value = value;
        }

        public ResourceKey<T> key() {
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
            return Optional.of(this.key);
        }

        @Override
        public String toString() {
            return "Reference{" + this.key + "=" + this.value + "}";
        }
    }
}
