/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.runtime.redirect;

import io.github.xponer2.translator.world.LevelStorageAccess;

import net.minecraft.server.MinecraftServer;

/**
 * World save access of 1.21 servers (R line on MinecraftServer, so the integrated server too): the
 * {@code storageSource} field is a {@link LevelStorageAccess} over the 1.12.2 save folder.
 */
public final class StorageBridges {
    private StorageBridges() {
    }

    /** {@code server.storageSource} (map mods name their data folders after the save: JourneyMap, Xaero). */
    public static LevelStorageAccess storageSource(MinecraftServer server) {
        return LevelStorageAccess.of(server);
    }
}
