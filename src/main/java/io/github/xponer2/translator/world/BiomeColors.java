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

	/** A biome that isn't there (a mod's that failed to be made) is asked as plains. */
	private static Biome known(@Nullable Biome biome) {
		return biome != null ? biome : net.minecraft.init.Biomes.PLAINS;
	}

	/**
	 * 1.21 {@code new Biome(climate, effects, generation, spawns)} (what 1.21's {@code Biome.BiomeBuilder.build()}
	 * does): a 1.12.2 biome of that climate and water color, not registered, whose 1.21 effects are the mod's own. It
	 * was null: JourneyMap's "void" biome, made in its class initializer, failed the whole class and no biome was mapped.
	 * The parameters are carried 1.21 classes, read by their 1.21 accessors.
	 */
	public static Biome newBiome(@Nullable Object climate, @Nullable Object effects, @Nullable Object generation, @Nullable Object spawns) {
		float temperature = number(climate, "temperature", 0.5F);
		if (temperature > 0.1F && temperature < 0.2F) {
			// 1.12.2 refuses these (snow would flicker there)
			temperature = 0.2F;
		}
		var properties = new Biome.BiomeProperties("Made by a 1.21.1 mod").setTemperature(temperature).setRainfall(number(climate, "downfall", 0.5F));
		if (Boolean.FALSE.equals(value(climate, "hasPrecipitation"))) {
			properties.setRainDisabled();
		}
		if (value(effects, "getWaterColor") instanceof Integer water) {
			properties.setWaterColor(water);
		}
		Biome biome = new MadeBiome(properties);
		EFFECTS.put(biome, effects);
		return biome;
	}

	private static float number(@Nullable Object of, String accessor, float otherwise) {
		return value(of, accessor) instanceof Float number ? number : otherwise;
	}

	@Nullable
	private static Object value(@Nullable Object of, String accessor) {
		if (of == null) {
			return null;
		}
		try {
			Method method = of.getClass().getMethod(accessor);
			method.setAccessible(true);
			return method.invoke(of);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	/** A biome a 1.21.1 mod made itself (1.12.2's constructor is protected to its subclasses). */
	private static final class MadeBiome extends Biome {
		MadeBiome(Biome.BiomeProperties properties) {
			super(properties);
		}
	}

	/** 1.21 {@code biome.getFoliageColor()}. */
	public static int getFoliageColor(@Nullable Biome biome) {
		return known(biome).getFoliageColorAtPos(SEA_LEVEL);
	}

	/** 1.21 {@code biome.getGrassColor(x, z)}. */
	public static int getGrassColor(@Nullable Biome biome, double x, double z) {
		return known(biome).getGrassColorAtPos(new BlockPos(x, 64, z));
	}

	/** 1.21 {@code biome.getSkyColor()}. */
	public static int getSkyColor(@Nullable Biome biome) {
		Biome known = known(biome);
		return known.getSkyColorByTemp(known.getDefaultTemperature());
	}

	/** 1.21 {@code biome.getWaterColor()} (1.12.2's is final, on a biome that may be null here). */
	public static int getWaterColor(@Nullable Biome biome) {
		return known(biome).getWaterColor();
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
	public static Object getSpecialEffects(@Nullable Biome given) {
		Biome biome = known(given);
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
