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
