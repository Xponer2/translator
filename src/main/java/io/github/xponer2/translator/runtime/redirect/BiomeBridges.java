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

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

/**
 * Biome calls of the 1.21.1 mods and the carried 1.21 code that 1.12.2's Biome lacks (runtime, a missing call on Biome
 * comes here, the biome first).
 */
public final class BiomeBridges {
	private BiomeBridges() {
	}

	/** 1.21 {@code getGenerationSettings()}: the biome's features ({@link io.github.xponer2.translator.world.DataFeatures#generationSettings}). */
	@Nullable
	public static Object getGenerationSettings(Biome biome) {
		return io.github.xponer2.translator.world.DataFeatures.generationSettings(biome);
	}

	/** 1.21 {@code shouldFreeze(level, pos)}: water there turns to ice (cold here, at an edge of the water, little light). */
	/** 1.21 {@code biome.hasPrecipitation()}: rain or snow falls there (Twilight Forest's weather renderer). */
	/**
	 * 1.21 {@code biome.getPrecipitationAt(pos)}: none where it doesn't rain or snow, snow where it is cold enough
	 * there, else rain (the carried Biome.Precipitation; 12 of 300 popular mods ask it: Ambient Sounds, Cobblemon).
	 */
	@javax.annotation.Nullable
	public static Object getPrecipitationAt(Biome biome, net.minecraft.util.math.BlockPos pos) {
		String kind = !biome.canRain() && !biome.getEnableSnow() ? "NONE" : biome.getTemperature(pos) < 0.15F ? "SNOW" : "RAIN";
		return CarriedBridges.constant("net.minecraft.world.level.biome.Biome$Precipitation", kind);
	}

	public static boolean hasPrecipitation(Biome biome) {
		return biome.canRain() || biome.getEnableSnow();
	}

	/** 1.21 {@code biome.warmEnoughToRain(pos)}: rain rather than snow at that height (1.12.2's 0.15 threshold). */
	public static boolean warmEnoughToRain(Biome biome, BlockPos pos) {
		return biome.getTemperature(pos) >= 0.15F;
	}

	public static boolean shouldFreeze(Biome biome, World world, BlockPos pos) {
		return world.canBlockFreeze(pos, true);
	}

	/** 1.21 {@code shouldFreeze(level, pos, mustBeAtEdge)}. */
	public static boolean shouldFreeze(Biome biome, World world, BlockPos pos, boolean mustBeAtEdge) {
		return world.canBlockFreeze(pos, mustBeAtEdge);
	}

	/** 1.21 {@code shouldSnow(level, pos)}: snow can lie there (cold, air, little light, ground under it). */
	public static boolean shouldSnow(Biome biome, World world, BlockPos pos) {
		return world.canSnowAt(pos, true);
	}
}
