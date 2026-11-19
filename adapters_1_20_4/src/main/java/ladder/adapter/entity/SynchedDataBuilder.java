/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;

/**
 * 1.20.5's {@code SynchedEntityData.Builder} over 1.20.4's SynchedEntityData: 1.20.4 makes an entity's data first and
 * defines each value on it, 1.20.5 gathers the values in a builder and makes the data of them. Each define goes
 * straight to the entity's data (1.20.4 checks the same: an id defined twice, or out of range, fails).
 */
public class SynchedDataBuilder {
    private final SynchedEntityData data;

    public SynchedDataBuilder(Entity entity) {
        this.data = entity.getEntityData();
    }

    public <T> SynchedDataBuilder define(EntityDataAccessor<T> accessor, T value) {
        this.data.define(accessor, value);
        return this;
    }

    public SynchedEntityData build() {
        return this.data;
    }
}
