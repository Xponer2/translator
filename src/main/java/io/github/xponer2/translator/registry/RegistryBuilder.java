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
import net.minecraft.util.ResourceLocation;

/**
 * NeoForge {@code RegistryBuilder}: a new registry (a mod's own, NeoForge's fluid types, ...) is the registry of its key
 * in {@link BuiltInRegistries}, made on first use, so a DeferredRegister for the key finds the same one. Syncing,
 * callbacks and id limits have no 1.12.2 counterpart and are kept only as settings. The callback parameters are
 * Object: they take the NeoForge callback interfaces of the mods (carried from 1.21.1).
 */
public class RegistryBuilder<T> {
    private final ResourceKey<? extends Registry<T>> key;
    private final java.util.List<Object> callbacks = new java.util.ArrayList<>();
    private ResourceLocation defaultKey;

    public RegistryBuilder(ResourceKey<? extends Registry<T>> key) {
        this.key = key;
    }

    public RegistryBuilder<T> defaultKey(ResourceLocation key) {
        this.defaultKey = key;
        return this;
    }

    public RegistryBuilder<T> defaultKey(ResourceKey<T> key) {
        this.defaultKey = key.location();
        return this;
    }

    public RegistryBuilder<T> callback(Object callback) {
        this.callbacks.add(callback);
        return this;
    }

    public RegistryBuilder<T> onAdd(Object callback) {
        this.callbacks.add(callback);
        return this;
    }

    public RegistryBuilder<T> onBake(Object callback) {
        this.callbacks.add(callback);
        return this;
    }

    public RegistryBuilder<T> onClear(Object callback) {
        this.callbacks.add(callback);
        return this;
    }

    /** Holders made with the entries (1.21 intrusive holders): the Translator makes its holders when asked. */
    public RegistryBuilder<T> withIntrusiveHolders() {
        return this;
    }

    public RegistryBuilder<T> maxId(int maxId) {
        return this;
    }

    public RegistryBuilder<T> disableRegistrationCheck() {
        return this;
    }

    public RegistryBuilder<T> sync(boolean sync) {
        return this;
    }

    public Registry<T> create() {
        Registry<T> registry = BuiltInRegistries.byKey(this.key);
        for (Object callback : this.callbacks) {
            registry.addCallback(callback);
        }
        return registry;
    }
}
