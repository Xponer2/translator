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
package io.github.xponer2.translator.selftest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import io.github.xponer2.translator.runtime.RuntimeMods;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.Loader;

/**
 * What the self-test checks of the world of the 1.21.1 mods, beside their blocks and items: whether their biomes are
 * generated anywhere, whether their dimensions exist on 1.12.2.
 */
final class WorldChecks {
	private static final Pattern DIMENSION_FILE = Pattern.compile("([a-z0-9_.-]+)/dimension/([a-z0-9_./-]+)\\.json");
	private static Set<Biome> generated;

	private WorldChecks() {
	}

	/** The dimensions the 1.21.1 mods define in their data ({@code data/<namespace>/dimension/<name>.json}). */
	@SuppressWarnings("deprecation")
	static List<String> dimensionsOfMods(@Nullable String filter) {
		var found = new LinkedHashSet<String>();
		for (var mod : Loader.instance().getActiveModList()) {
			if (!RuntimeMods.isRuntimeNamespace(mod.getModId())) {
				continue;
			}
			CraftingHelper.findFiles(mod, "data", null, (root, file) -> {
				var m = DIMENSION_FILE.matcher(root.relativize(file).toString().replace('\\', '/'));
				if (m.matches() && SelfTest.wanted(filter, m.group(1))) {
					found.add(m.group(1) + ":" + m.group(2));
				}
				return true;
			}, true, true);
		}
		return new ArrayList<>(found);
	}

	/** The 1.12.2 dimension of a 1.21 dimension id, by its name; null when 1.12.2 has none. */
	@Nullable
	static DimensionType dimensionType(String id) {
		String path = id.substring(id.indexOf(':') + 1);
		for (DimensionType type : DimensionType.values()) {
			if (type.getName().equals(id) || type.getName().equals(path)) {
				return type;
			}
		}
		return null;
	}

	/**
	 * The biomes some world generates: the overworld's generator for a default world (a 64 by 64 grid of samples 256
	 * blocks apart) and every loaded world's own (the dimensions of the mods, once they exist). Sampled once per test.
	 */
	static synchronized Set<Biome> generatedBiomes(WorldServer testWorld) {
		if (generated != null) {
			return generated;
		}
		var biomes = new LinkedHashSet<Biome>();
		var overworld = new WorldInfo(new WorldSettings(testWorld.getSeed(), GameType.SURVIVAL, true, false, WorldType.DEFAULT), "biomes");
		sample(new BiomeProvider(overworld), biomes);
		for (WorldServer world : DimensionManager.getWorlds()) {
			if (world != testWorld && world.provider.getDimension() != 0) {
				sample(world.getBiomeProvider(), biomes);
			}
		}
		generated = Collections.unmodifiableSet(biomes);
		return generated;
	}

	private static void sample(BiomeProvider provider, Set<Biome> into) {
		for (int x = -32; x < 32; x++) {
			for (int z = -32; z < 32; z++) {
				into.add(provider.getBiome(new BlockPos(x * 256, 64, z * 256)));
			}
		}
	}

	/** A new test may sample again (worlds loaded since). */
	static synchronized void reset() {
		generated = null;
	}
}
