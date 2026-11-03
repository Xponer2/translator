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
package io.github.xponer2.translator.world;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.github.xponer2.translator.registry.ResourceKey;
import io.github.xponer2.translator.runtime.redirect.ApiBridges;

import net.minecraft.server.MinecraftServer;

/**
 * 1.21 {@code LevelStorageSource.LevelStorageAccess} ({@code server.storageSource}): the save folder of the running
 * world. 1.12.2 keeps the session lock itself, so closing does nothing.
 */
public final class LevelStorageAccess implements AutoCloseable {
    private final String levelId;
    private final Path levelPath;

    public LevelStorageAccess(String levelId, Path levelPath) {
        this.levelId = levelId;
        this.levelPath = levelPath;
    }

    /** The save folder of a server's world (a client's integrated server too). */
    public static LevelStorageAccess of(MinecraftServer server) {
        String folder = server.getFolderName();
        File dir;
        var overworld = server.worlds != null && server.worlds.length > 0 ? server.getWorld(0) : null;
        if (overworld != null) {
            dir = overworld.getSaveHandler().getWorldDirectory();
        } else {
            dir = server.getActiveAnvilConverter().getFile(folder, "");
        }
        return new LevelStorageAccess(folder, dir.toPath());
    }

    public String getLevelId() {
        return this.levelId;
    }

    public Path getLevelPath(LevelResource resource) {
        return this.levelPath.resolve(resource.getId()).normalize();
    }

    /** 1.12.2 dimension folders: the overworld in the save folder, the others in DIM&lt;id&gt;. */
    public Path getDimensionPath(ResourceKey<?> dimension) {
        int id = ApiBridges.dimensionId(dimension);
        return id == 0 ? this.levelPath : this.levelPath.resolve("DIM" + id);
    }

    public Optional<Path> getIconFile() {
        Path icon = this.getLevelPath(LevelResource.ICON_FILE);
        return Files.isRegularFile(icon) ? Optional.of(icon) : Optional.empty();
    }

    public boolean hasWorldData() {
        return Files.isRegularFile(this.getLevelPath(LevelResource.LEVEL_DATA_FILE));
    }

    @Override
    public void close() {
    }

    public void safeClose() {
    }

    @Override
    public String toString() {
        return "LevelStorageAccess[" + this.levelId + "]";
    }
}
