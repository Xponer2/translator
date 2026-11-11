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

import javax.annotation.Nullable;

import net.minecraft.entity.IEntityLivingData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

/**
 * A mob with 1.21's {@code finalizeSpawn(level, difficulty, spawnType, groupData)}: the Translator's living facades.
 * Their onInitialSpawn (what 1.12.2 calls when a mob spawns) runs it, so a mod's override sets its mob up (equipment,
 * variants, a boss's state); before, it was never called. Its default is 1.12.2's onInitialSpawn.
 */
public interface SpawnFinalizing {
	@Nullable
	IEntityLivingData finalizeSpawn(World level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable IEntityLivingData data);

}
