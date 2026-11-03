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

import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * NeoForge {@code NewRegistryEvent} (neoforge-1.21.1-runtime.tsv): posted once every 1.21.1 mod is constructed, before
 * the registry events, as NeoForge does. A mod makes its own registries here (AE2's key types, without which no ME
 * storage works); they are found by their keys afterwards.
 */
public class NewRegistryEvent extends Event {
    public <T> Registry<T> create(RegistryBuilder<T> builder) {
        return builder.create();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void register(Registry<?> registry) {
        io.github.xponer2.translator.util.BuiltInRegistries.REGISTRY.register((ResourceKey) registry.key(), registry, RegistrationInfo.BUILT_IN);
    }
}
