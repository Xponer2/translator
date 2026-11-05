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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.world.biome.Biome;

/** What the data says a biome or a sapling grows: for the self-test to compare with what grew. */
public final class TreeInfo {
	private TreeInfo() {
	}

	/** Whether a 1.21.1 mod's biome lists features in its data. */
	public static boolean hasFeatures(Biome biome) {
		return biome instanceof DataBiome data && !data.features().isEmpty();
	}

	/** The logs and leaves of a mod biome's trees ({log, leaves} each), empty for other biomes. */
	public static List<Block[]> trees(Biome biome) {
		var out = new ArrayList<Block[]>();
		if (biome instanceof DataBiome data) {
			for (var tree : data.trees()) {
				if (tree instanceof DataTree dataTree) {
					out.add(new Block[] {dataTree.log().getBlock(), dataTree.leaves().getBlock()});
				}
			}
		}
		return out;
	}

	/** The log and leaves of the tree a sapling's 1.21 tree grower names (its data), null when it has none here. */
	@Nullable
	public static Block[] saplingTree(@Nullable Object grower) {
		var tree = TreeGrowing.treeOf(grower, new Random(0));
		return tree instanceof DataTree dataTree ? new Block[] {dataTree.log().getBlock(), dataTree.leaves().getBlock()} : null;
	}
}
