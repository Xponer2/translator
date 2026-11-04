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
package io.github.xponer2.translator.runtime.mc;

import io.github.xponer2.translator.entity.SynchedDataBuilder;
import io.github.xponer2.translator.registry.EntityType;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * 1.21 {@code Entity} for the entities of 1.21.1 mods that extend it directly (the seats of furniture, a mod's own
 * projectiles and markers): made with their EntityType; their {@code defineSynchedData}, saved data and tick are
 * 1.12.2's entityInit, entity NBT and onUpdate. Without it their constructor couldn't run (1.12.2's Entity takes the
 * world only) and the abstract 1.12.2 methods were missing.
 */
public abstract class Entity extends net.minecraft.entity.Entity {
	private final EntityType<?> type;

	protected Entity(EntityType<?> type, World world) {
		super(world);
		this.type = type;
	}

	public EntityType<?> getType() {
		return this.type;
	}

	/** 1.21 defineSynchedData: the entity's data, defined with a builder over the 1.12.2 data manager. */
	protected void defineSynchedData(SynchedDataBuilder builder) {
	}

	@Override
	protected void entityInit() {
		this.defineSynchedData(new SynchedDataBuilder(this.dataManager));
	}

	protected void readAdditionalSaveData(NBTTagCompound tag) {
	}

	protected void addAdditionalSaveData(NBTTagCompound tag) {
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		this.readAdditionalSaveData(tag);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		this.addAdditionalSaveData(tag);
	}

	/** 1.12.2's update is 1.21's tick; 1.21's base tick (super.tick()) is 1.12.2's. */
	@Override
	public void onUpdate() {
		this.tick();
	}

	public void tick() {
		super.onUpdate();
	}
}
