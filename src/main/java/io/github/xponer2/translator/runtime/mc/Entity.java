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
		try {
			this.tick();
		} catch (RuntimeException | LinkageError e) {
			// a part of the 1.21 game the Translator lacks, in a mod entity's tick: logged once, where it stopped the server
			// (Iron's Spells' projectiles hitting the player); the entity goes, so it doesn't fail every tick
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".tick", e);
			this.setDead();
		}
	}

	public void tick() {
		super.onUpdate();
	}

	/** 1.21: in a bubble column (1.12.2 has none): pulled down or pushed up as there. */
	public void onInsideBubbleColumn(boolean down) {
		this.motionY = down ? Math.max(-0.3D, this.motionY - 0.03D) : Math.min(0.7D, this.motionY + 0.06D);
		this.fallDistance = 0.0F;
	}

	/** 1.21: above a bubble column. */
	public void onAboveBubbleCol(boolean down) {
		this.motionY = down ? Math.max(-0.9D, this.motionY - 0.03D) : Math.min(1.8D, this.motionY + 0.1D);
	}
}
