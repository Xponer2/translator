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
package io.github.xponer2.translator.entity;

import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;

/**
 * 1.21 {@code SynchedEntityData.Builder}: what an entity defines in {@code defineSynchedData} goes straight into the
 * 1.12.2 data manager of the entity.
 */
public class SynchedDataBuilder {
	private final EntityDataManager manager;

	public SynchedDataBuilder(EntityDataManager manager) {
		this.manager = manager;
	}

	/** The builder of an entity's data (the converter's entityInit bridge of a mod's mob calls it). */
	public static SynchedDataBuilder of(net.minecraft.entity.Entity entity) {
		return new SynchedDataBuilder(entity.getDataManager());
	}

	public <T> SynchedDataBuilder define(DataParameter<T> accessor, T value) {
		this.manager.register(accessor, value);
		return this;
	}

	public EntityDataManager build() {
		return this.manager;
	}
}
