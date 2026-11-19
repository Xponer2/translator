/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.entity;

import net.minecraft.world.entity.Entity;

/** The game's own side of 1.20.5's entity hooks, for a converted mod's super calls. */
public final class EntityDefaults {
    private EntityDefaults() {
    }

    /** super.defineSynchedData(builder) into the game's class: 1.20.4 has defined the game's data already */
    public static void defineSynchedData(Entity entity, SynchedDataBuilder builder) {
    }
}
