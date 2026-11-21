/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.misc;

import ladder.rung1206.mixin.ChunkStorageAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

/**
 * The values 1.21 passes to chunk loading that 1.20.6 doesn't (the A row of ChunkSerializer.read): the region info of
 * the level's chunk storage, which 1.21's chunk map passes and 1.20.6's keeps.
 */
public final class ChunkArgs {
    private ChunkArgs() {
    }

    public static RegionStorageInfo storageInfo(ServerLevel level) {
        return ((ChunkStorageAccess) level.getChunkSource().chunkMap).ladder$storageInfo();
    }
}
