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
package io.github.xponer2.translator.world;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;

/**
 * 1.21 {@code WorldOptions}: a world's seed and generation switches, from 1.12.2's WorldInfo. With 1.21's WorldData
 * (the table maps it to WorldInfo), {@code server.getWorldData().worldGenOptions().seed()} gives the world's seed:
 * AE2's meteorites, Twilight Forest's and TerraBlender's generation read it (they got nothing).
 */
public final class WorldOptions {
	private final long seed;
	private final boolean generateStructures;
	private final boolean generateBonusChest;

	public WorldOptions(long seed, boolean generateStructures, boolean generateBonusChest) {
		this.seed = seed;
		this.generateStructures = generateStructures;
		this.generateBonusChest = generateBonusChest;
	}

	public static WorldOptions of(WorldInfo info) {
		return new WorldOptions(info.getSeed(), info.isMapFeaturesEnabled(), false);
	}

	public long seed() {
		return this.seed;
	}

	public boolean generateStructures() {
		return this.generateStructures;
	}

	public boolean generateBonusChest() {
		return this.generateBonusChest;
	}

	public boolean isOldCustomizedWorld() {
		return false;
	}

	public WorldOptions withBonusChest(boolean bonusChest) {
		return new WorldOptions(this.seed, this.generateStructures, bonusChest);
	}

	public WorldOptions withStructures(boolean structures) {
		return new WorldOptions(this.seed, structures, this.generateBonusChest);
	}

	public WorldOptions withSeed(java.util.OptionalLong seed) {
		return new WorldOptions(seed.isPresent() ? seed.getAsLong() : this.seed, this.generateStructures, this.generateBonusChest);
	}

	/** 1.21 {@code server.getWorldData()}: the overworld's WorldInfo (the table's WorldData). */
	@Nullable
	public static WorldInfo getWorldData(MinecraftServer server) {
		World overworld = server.getWorld(0);
		return overworld == null ? null : overworld.getWorldInfo();
	}

	/** 1.21 {@code worldData.worldGenOptions()}. */
	public static WorldOptions worldGenOptions(WorldInfo info) {
		return of(info);
	}

	/** 1.21 {@code worldData.overworldData()}: the same WorldInfo. */
	public static WorldInfo overworldData(WorldInfo info) {
		return info;
	}
}
