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
