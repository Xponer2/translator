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
package io.github.xponer2.translator.runtime.redirect;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * 1.21 {@code BaseSpawner} on 1.12.2's MobSpawnerBaseLogic: the 1.21 methods take the world and position (1.12.2's
 * logic asks its own), and some are private there. Twilight Forest's sinister spawners (its towers' and dungeons')
 * never reset their timer, never saw a player near and never took the mob to spawn.
 */
public final class SpawnerBridges {
	private SpawnerBridges() {
	}

	private static int getInt(MobSpawnerBaseLogic logic, String srg) {
		return ObfuscationReflectionHelper.getPrivateValue(MobSpawnerBaseLogic.class, logic, srg);
	}

	/**
	 * 1.21 {@code delay(level, pos)}: a new random delay and the next mob to spawn. 1.12.2's resetTimer does the same
	 * with the world the logic gives itself, which a converted 1.21 spawner hasn't (it gets the world as an argument):
	 * done here with the world given.
	 */
	@SuppressWarnings("unchecked")
	public static void delay(MobSpawnerBaseLogic logic, World world, BlockPos pos) {
		int min = getInt(logic, "field_98283_g");
		int max = getInt(logic, "field_98293_h");
		int delay = max <= min ? min : min + world.rand.nextInt(max - min);
		ObfuscationReflectionHelper.setPrivateValue(MobSpawnerBaseLogic.class, logic, delay, "field_98286_b");
		java.util.List<WeightedSpawnerEntity> potentials = ObfuscationReflectionHelper.getPrivateValue(MobSpawnerBaseLogic.class, logic, "field_98285_e");
		if (potentials != null && !potentials.isEmpty()) {
			logic.setNextSpawnData(net.minecraft.util.WeightedRandom.getRandomItem(world.rand, potentials));
		}
	}

	/** 1.21 {@code isNearPlayer(level, pos)}: a player within the spawner's activation range of the position. */
	public static boolean isNearPlayer(MobSpawnerBaseLogic logic, World world, BlockPos pos) {
		int range;
		try {
			range = getInt(logic, "field_98289_l");
		} catch (RuntimeException e) {
			range = 16;
		}
		return world.isAnyPlayerWithinRangeAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, range);
	}

	/** 1.21 {@code onEventTriggered(level, id)}: 1.12.2's setDelayToMin. */
	public static boolean onEventTriggered(MobSpawnerBaseLogic logic, World world, int id) {
		return logic.setDelayToMin(id);
	}

	/** 1.21 {@code load(level, pos, tag)}: 1.12.2's readFromNBT. */
	public static void load(MobSpawnerBaseLogic logic, @Nullable World world, BlockPos pos, NBTTagCompound tag) {
		logic.readFromNBT(tag);
	}

	/** 1.21 {@code setEntityId(type, level, random, pos)}: the type's id (1.12.2 takes the id). */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void setEntityId(MobSpawnerBaseLogic logic, io.github.xponer2.translator.registry.EntityType<?> type, @Nullable World world,
			@Nullable java.util.Random random, @Nullable BlockPos pos) {
		net.minecraft.util.ResourceLocation id = ((io.github.xponer2.translator.registry.Registry) io.github.xponer2.translator.util.BuiltInRegistries
				.byKey(io.github.xponer2.translator.util.Registries.ENTITY_TYPE)).getKey(type);
		if (id != null) {
			logic.setEntityId(id);
		}
	}

	/** 1.21 {@code setNextSpawnData(level, pos, spawnData)}: the carried SpawnData's entity tag as 1.12.2's next entity. */
	public static void setNextSpawnData(MobSpawnerBaseLogic logic, @Nullable World world, @Nullable BlockPos pos, Object spawnData) {
		NBTTagCompound tag = null;
		try {
			Object entity = spawnData.getClass().getMethod("entityToSpawn").invoke(spawnData);
			if (entity instanceof NBTTagCompound compound) {
				tag = compound;
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			// not a SpawnData: nothing to set
		}
		if (tag != null) {
			logic.setNextSpawnData(new WeightedSpawnerEntity(1, tag.copy()));
		}
	}
}
