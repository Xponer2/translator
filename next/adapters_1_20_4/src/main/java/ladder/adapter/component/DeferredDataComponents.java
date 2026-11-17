/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import java.util.function.UnaryOperator;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** NeoForge 21's {@code DeferredRegister.DataComponents}: a mod's component types, registered as on 1.21. */
public class DeferredDataComponents extends DeferredRegister<DataComponentType<?>> {
    protected DeferredDataComponents(String namespace) {
        super(ComponentRegistry.KEY, namespace);
    }

    /** NeoForge 21's {@code DeferredRegister.createDataComponents(namespace)} */
    public static DeferredDataComponents createDataComponents(String namespace) {
        return new DeferredDataComponents(namespace);
    }

    /** NeoForge 21's {@code createDataComponents(registry key, namespace)} (the key is the component registry's) */
    public static DeferredDataComponents createDataComponents(net.minecraft.resources.ResourceKey<?> key, String namespace) {
        return new DeferredDataComponents(namespace);
    }

    public <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> registerComponentType(String name,
            UnaryOperator<DataComponentType.Builder<D>> builder) {
        return this.register(name, () -> builder.apply(DataComponentType.builder()).build());
    }
}
