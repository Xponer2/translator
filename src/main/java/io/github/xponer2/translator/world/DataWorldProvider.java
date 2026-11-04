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

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.IChunkGenerator;

/**
 * The world of a 1.21.1 mod's dimension ({@link DataDimensions}): one class for all of them, each finds its data by
 * its dimension id (set before {@link #init()}).
 */
public class DataWorldProvider extends WorldProvider {
	@Override
	protected void init() {
		DataDimensions.Data data = DataDimensions.data(this.getDimension());
		this.hasSkyLight = data == null || data.skylight;
		this.doesWaterVaporize = data != null && data.ultrawarm;
		this.nether = data != null && data.ceiling;
		this.biomeProvider = data != null && !data.biomes.isEmpty() ? new DataBiomeProvider(this.world.getSeed(), data.biomes)
				: new BiomeProvider(this.world.getWorldInfo());
	}

	@Override
	public DimensionType getDimensionType() {
		return DimensionType.getById(this.getDimension());
	}

	@Override
	public IChunkGenerator createChunkGenerator() {
		return new DataChunkGenerator(this.world, DataDimensions.data(this.getDimension()));
	}

	/** A dimension with a fixed time (Twilight Forest's dusk) keeps its sky there. */
	@Override
	public float calculateCelestialAngle(long worldTime, float partialTicks) {
		DataDimensions.Data data = DataDimensions.data(this.getDimension());
		return data != null && data.fixedTime != null ? super.calculateCelestialAngle(data.fixedTime, 0.0F) : super.calculateCelestialAngle(worldTime, partialTicks);
	}

	@Override
	public boolean canRespawnHere() {
		DataDimensions.Data data = DataDimensions.data(this.getDimension());
		return data != null && data.beds;
	}

	@Override
	public boolean isSurfaceWorld() {
		DataDimensions.Data data = DataDimensions.data(this.getDimension());
		return data == null || !data.ceiling && data.terrain != DataDimensions.Data.Terrain.VOID;
	}

	@Override
	public int getAverageGroundLevel() {
		DataDimensions.Data data = DataDimensions.data(this.getDimension());
		return data != null && data.terrain == DataDimensions.Data.Terrain.ISLANDS ? 100 : super.getAverageGroundLevel();
	}
}
