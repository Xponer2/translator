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

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.21 {@code BaseSpawner} for the mods' spawner logic (1.12.2's MobSpawnerBaseLogic): 1.21 passes the world and
 * position to each call, NeoForge tells the owner ({@code getOwner()}: the block entity or entity), and 1.12.2's logic
 * asks for them (getSpawnerWorld, getSpawnerPosition: abstract there, nothing a 1.21 spawner has; the converter made them
 * give null, and 1.12.2's own calls of the logic threw: Twilight Forest's sinister spawners). Its events go to the 1.21
 * broadcastEvent the mod implements.
 */
public abstract class BaseSpawner extends MobSpawnerBaseLogic {
	/** NeoForge {@code getOwner()}: the block entity or entity the spawner is in (the mods' spawners say). */
	@Nullable
	public com.mojang.datafixers.util.Either<TileEntity, Entity> getOwner() {
		return null;
	}

	@Override
	@Nullable
	public World getSpawnerWorld() {
		var owner = this.getOwner();
		return owner == null ? null : owner.map(TileEntity::getWorld, entity -> entity.world);
	}

	@Override
	public BlockPos getSpawnerPosition() {
		var owner = this.getOwner();
		return owner == null ? BlockPos.ORIGIN : owner.map(TileEntity::getPos, Entity::getPosition);
	}

	/** 1.12.2's event (a delay reset, 1): the 1.21 one with the owner's world and position. */
	@Override
	public void broadcastEvent(int id) {
		World world = this.getSpawnerWorld();
		if (world != null) {
			this.broadcastEvent(world, this.getSpawnerPosition(), id);
		}
	}

	/** 1.21 {@code broadcastEvent(level, pos, id)}: what the mod's spawner implements. */
	public void broadcastEvent(World level, BlockPos pos, int id) {
	}
}
