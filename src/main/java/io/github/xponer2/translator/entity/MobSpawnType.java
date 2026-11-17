/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.entity;

/**
 * 1.21 {@code MobSpawnType}: why a mob was spawned, which a mod's {@code finalizeSpawn} reads. 1.12.2 has none; it was a
 * carried 1.21 enum, so a mod's finalizeSpawn couldn't take 1.12.2's place (SpawnFinalizing).
 */
public enum MobSpawnType {
	NATURAL,
	CHUNK_GENERATION,
	SPAWNER,
	STRUCTURE,
	BREEDING,
	MOB_SUMMONED,
	JOCKEY,
	EVENT,
	CONVERSION,
	REINFORCEMENT,
	TRIGGERED,
	BUCKET,
	SPAWN_EGG,
	COMMAND,
	DISPENSER,
	PATROL,
	TRIAL_SPAWNER;

	public static boolean isSpawner(MobSpawnType type) {
		return type == SPAWNER || type == TRIAL_SPAWNER;
	}

	public static boolean ignoresLightRequirements(MobSpawnType type) {
		return type == TRIAL_SPAWNER;
	}
}
