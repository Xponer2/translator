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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

/**
 * 1.21 colors of a biome on a 1.12.2 one. 1.21 keeps a biome's colors in its {@code BiomeSpecialEffects} and asks
 * them without a position; 1.12.2 works its grass and leaf colors out from the climate at a block. A mod that colors
 * a map (JourneyMap) or the world (leaf and grass tints) asks these.
 */
public final class BiomeColors {
	private static final Map<Biome, Object> EFFECTS = Collections.synchronizedMap(new IdentityHashMap<>());
	/** A block at sea level: the climate there is the biome's own (1.12.2 cools it with height above 64). */
	private static final BlockPos SEA_LEVEL = new BlockPos(0, 64, 0);

	private BiomeColors() {
	}

	/** 1.21 {@code biome.getFoliageColor()}. */
	public static int getFoliageColor(Biome biome) {
		return biome.getFoliageColorAtPos(SEA_LEVEL);
	}

	/** 1.21 {@code biome.getGrassColor(x, z)}. */
	public static int getGrassColor(Biome biome, double x, double z) {
		return biome.getGrassColorAtPos(new BlockPos(x, 64, z));
	}

	/** 1.21 {@code biome.getSkyColor()}. */
	public static int getSkyColor(Biome biome) {
		return biome.getSkyColorByTemp(biome.getDefaultTemperature());
	}

	/** 1.21 {@code biome.getFogColor()}: 1.12.2's fog is the world's (the overworld's day fog). */
	public static int getFogColor(Biome biome) {
		return 0xC0D8FF;
	}

	public static int getWaterFogColor(Biome biome) {
		return 0x050533;
	}

	/**
	 * 1.21 {@code biome.getSpecialEffects()}: a 1.21 BiomeSpecialEffects (the carried class, made through its builder)
	 * with this biome's colors; its grass and leaf colors are left to the climate, as for a 1.21 biome without overrides.
	 * Null only when the 1.21 class can't be made.
	 */
	@Nullable
	public static Object getSpecialEffects(Biome biome) {
		Object effects = EFFECTS.get(biome);
		if (effects == null && !EFFECTS.containsKey(biome)) {
			effects = make(biome);
			EFFECTS.put(biome, effects);
		}
		return effects;
	}

	@Nullable
	private static Object make(Biome biome) {
		try {
			Class<?> type = Class.forName("net.minecraft.world.level.biome.BiomeSpecialEffects$Builder", true, Launch.classLoader);
			Object builder = type.getConstructor().newInstance();
			set(type, builder, "fogColor", getFogColor(biome));
			set(type, builder, "waterColor", biome.getWaterColor() & 0xFFFFFF);
			set(type, builder, "waterFogColor", getWaterFogColor(biome));
			set(type, builder, "skyColor", getSkyColor(biome) & 0xFFFFFF);
			return type.getMethod("build").invoke(builder);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	private static void set(Class<?> type, Object builder, String name, int color) throws ReflectiveOperationException {
		Method method = type.getMethod(name, int.class);
		method.invoke(builder, color);
	}
}
