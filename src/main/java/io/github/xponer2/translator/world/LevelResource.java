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

/**
 * 1.21 {@code LevelResource}: a file or folder of a world save, relative to the save folder. The 1.12.2 save has the
 * same layout for these (playerdata, stats, advancements, level.dat, ...).
 */
public final class LevelResource {
    public static final LevelResource PLAYER_ADVANCEMENTS_DIR = new LevelResource("advancements");
    public static final LevelResource PLAYER_STATS_DIR = new LevelResource("stats");
    public static final LevelResource PLAYER_DATA_DIR = new LevelResource("playerdata");
    public static final LevelResource PLAYER_OLD_DATA_DIR = new LevelResource("players");
    public static final LevelResource LEVEL_DATA_FILE = new LevelResource("level.dat");
    public static final LevelResource OLD_LEVEL_DATA_FILE = new LevelResource("level.dat_old");
    public static final LevelResource ICON_FILE = new LevelResource("icon.png");
    public static final LevelResource LOCK_FILE = new LevelResource("session.lock");
    public static final LevelResource GENERATED_DIR = new LevelResource("generated");
    public static final LevelResource DATAPACK_DIR = new LevelResource("datapacks");
    public static final LevelResource MAP_RESOURCE_FILE = new LevelResource("resources.zip");
    public static final LevelResource ROOT = new LevelResource(".");

    private final String id;

    public LevelResource(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return "/" + this.id;
    }
}
