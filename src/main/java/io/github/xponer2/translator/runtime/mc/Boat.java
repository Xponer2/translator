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

import net.minecraft.entity.item.EntityBoat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * 1.21 {@code Boat} for the boats of 1.21.1 mods (their woods, and the carried chest boat): made with their EntityType
 * (1.12.2's boat takes the world only: no mod's boat could be made), their synched data and saved data on 1.12.2's
 * boat's. The boat item they drop is their getDropItem (1.12.2's getItemBoat by the table).
 */
public class Boat extends EntityBoat {
	private final EntityType<?> type;

	public Boat(EntityType<?> type, World world) {
		super(world);
		this.type = type;
	}

	/** 1.21 {@code Boat(level, x, y, z)}: the vanilla boat's type. */
	public Boat(World world, double x, double y, double z) {
		super(world, x, y, z);
		this.type = null;
	}

	public EntityType<?> getType() {
		return this.type;
	}

	/** 1.21 defineSynchedData: the mod's data after 1.12.2's boat's. */
	protected void defineSynchedData(SynchedDataBuilder builder) {
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.defineSynchedData(new SynchedDataBuilder(this.dataManager));
	}

	/** 1.21's boat saves its type: 1.12.2's boat's saved data. */
	protected void readAdditionalSaveData(NBTTagCompound tag) {
		super.readEntityFromNBT(tag);
	}

	protected void addAdditionalSaveData(NBTTagCompound tag) {
		super.writeEntityToNBT(tag);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		this.readAdditionalSaveData(tag);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		this.addAdditionalSaveData(tag);
	}
}
