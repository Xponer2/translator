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

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.21 {@code LevelLightEngine} of a world ({@code level.getLightEngine()}): 1.12.2 relights a block with
 * {@code World.checkLight} and keeps no queue of its own (AE2's cable buses relight when their parts change). Mods
 * extend it for the light of a world of their own (Flywheel's VirtualLightEngine, which Create's block entity renderers
 * use: it couldn't load while this class was final, and Create's schematicannon, shafts and cogwheels crashed the client).
 */
public class LevelLightEngine {
	/** The world, or null for a mod's light engine of no real world (it gives the light itself). */
	@javax.annotation.Nullable
	private final World world;

	public LevelLightEngine(World world) {
		this.world = world;
	}

	/** 1.21 {@code new LevelLightEngine(chunks, blockLight, skyLight)}: the light of the chunks' world (none: of nothing). */
	public LevelLightEngine(net.minecraft.world.level.chunk.LightChunkGetter chunks, boolean blockLight, boolean skyLight) {
		net.minecraft.world.IBlockAccess level = chunks == null ? null : chunks.getLevel();
		this.world = level instanceof World ? (World) level : null;
	}

	public void checkBlock(BlockPos pos) {
		if (this.world != null) {
			this.world.checkLight(pos);
		}
	}

	public int getRawBrightness(BlockPos pos, int skyDarken) {
		return this.world == null ? 0 : this.world.getLightFromNeighbors(pos);
	}

	/**
	 * 1.21 {@code getLayerListener(layer)}: the light of one kind (block or sky) at a block, 1.12.2's saved light there.
	 */
	public LayerLightListener getLayerListener(net.minecraft.world.EnumSkyBlock layer) {
		return pos -> this.world != null && this.world.isBlockLoaded(pos) ? this.world.getLightFor(layer, pos) : 0;
	}

	public boolean hasLightWork() {
		return false;
	}

	public int runLightUpdates() {
		return 0;
	}
}
