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

/**
 * The registries of a world or server (1.21 {@code RegistryAccess}): in 1.12.2 always the same, {@link #INSTANCE}.
 */
public final class RegistryAccess implements HolderLookup.Provider {
    public static final RegistryAccess INSTANCE = new RegistryAccess();
    public static final RegistryAccess EMPTY = INSTANCE;

    private RegistryAccess() { }

    /** A registry by its key: the 1.12.2 Forge registry or the Translator's own (BuiltInRegistries). */
    public <T> Registry<T> registryOrThrow(ResourceKey<? extends Registry<? extends T>> key) {
        return this.lookupOrThrow(key);
    }

    @SuppressWarnings("unchecked")
    public <T> java.util.Optional<Registry<T>> registry(ResourceKey<? extends Registry<? extends T>> key) {
        try {
            return java.util.Optional.ofNullable((Registry<T>) this.lookupOrThrow(key));
        } catch (RuntimeException e) {
            return java.util.Optional.empty();
        }
    }

    public <T> java.util.Optional<Registry<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
        return this.registry(key);
    }

    public RegistryAccess freeze() {
        return this;
    }

    /**
     * 1.21 {@code registries()}: every registry with its key. JEI makes a tag information category for each one's tags (an
     * empty stream left them out).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public java.util.stream.Stream<RegistryEntry<?>> registries() {
        var entries = new java.util.ArrayList<RegistryEntry<?>>();
        for (Registry<?> registry : io.github.xponer2.translator.util.BuiltInRegistries.all()) {
            if (registry.key() != null) {
                entries.add(new RegistryEntry(registry.key(), registry));
            }
        }
        return entries.stream();
    }

    /** 1.21 {@code RegistryAccess.RegistryEntry}: a registry and its key. */
    public static final class RegistryEntry<T> {
        private final ResourceKey<? extends Registry<T>> key;
        private final Registry<T> value;

        public RegistryEntry(ResourceKey<? extends Registry<T>> key, Registry<T> value) {
            this.key = key;
            this.value = value;
        }

        public ResourceKey<? extends Registry<T>> key() {
            return this.key;
        }

        public Registry<T> value() {
            return this.value;
        }
    }
}
