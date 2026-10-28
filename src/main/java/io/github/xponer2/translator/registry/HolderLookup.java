package io.github.xponer2.translator.registry;

import io.github.xponer2.translator.util.BuiltInRegistries;

/**
 * Access to the registries (1.21 {@code HolderLookup}). 1.12.2 has no per-world registries: every provider is
 * {@link RegistryAccess#INSTANCE}, backed by {@link BuiltInRegistries}.
 */
public interface HolderLookup {
    interface Provider {
        default <T> Registry<T> lookupOrThrow(ResourceKey<? extends Registry<? extends T>> key) {
            @SuppressWarnings("unchecked")
            var registry = (Registry<T>) BuiltInRegistries.byKey((ResourceKey<Registry<T>>) (ResourceKey<?>) key);
            return registry;
        }

        default <T> Holder<T> holderOrThrow(ResourceKey<T> key) {
            var registry = BuiltInRegistries.byKey(ResourceKey.<T>createRegistryKey(key.registry()));
            return registry.getHolder(key.location()).orElseThrow(() -> new IllegalStateException("Missing element " + key));
        }
    }
}
