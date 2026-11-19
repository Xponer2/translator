/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.entity;

/**
 * 1.20.5's {@code Entity.defineSynchedData(SynchedEntityData.Builder)}: a converted mod's entity that overrides it gets
 * this interface; 1.20.4's Entity constructor calls it right after its own defineSynchedData()
 * ({@link ladder.adapter.mixin.EntityMixin}), so the mod's data is defined on the entity's SynchedEntityData then.
 * The game's own data is defined by 1.20.4's no-argument method already: a super call into the game's class does
 * nothing more (EntityDefaults).
 */
public interface SynchedDataApi {
    default void defineSynchedData(SynchedDataBuilder builder) {
    }
}
