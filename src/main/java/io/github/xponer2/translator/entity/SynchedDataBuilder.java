/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
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
