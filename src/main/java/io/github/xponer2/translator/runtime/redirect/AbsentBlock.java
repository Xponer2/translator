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

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

/**
 * A 1.21 vanilla block 1.12.2 hasn't (soul soil, the nyliums, ...) as a mod's {@code Blocks.SOUL_SOIL} reads it: a block
 * of its own, never in a world, so a comparison with it is false. It was air, so every check "the block below is soul
 * soil" held for air (Biomes O' Plenty's burning blossom stayed when its ground was gone and could be placed in the air).
 * Its default state is air's: what a mod places as it ({@code SOUL_SOIL.defaultBlockState()}) is air, as before. Not
 * registered.
 */
public final class AbsentBlock extends Block {
	private final String name;

	AbsentBlock(String name) {
		super(Material.AIR);
		this.name = name;
		this.setDefaultState(Blocks.AIR.getDefaultState());
	}

	/** The 1.21 name of the block this stands for. */
	public String name() {
		return this.name;
	}

	private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "mangrove", "cherry", "bamboo", "crimson", "warped",
			"pale_oak"};

	/**
	 * The 1.12.2 block a mod's block copied from this one is most like (its material, hardness, resistance, sound): a
	 * spruce trapdoor is 1.12.2's trapdoor, cherry planks are planks. Null when nothing fits. A mod's
	 * {@code Properties.ofFullCopy(Blocks.SPRUCE_TRAPDOOR)} copied air: its blocks could not be broken and dropped
	 * nothing (Macaw's trapdoors, Farmer's Delight's hanging signs, Handcrafted's furniture).
	 */
	@javax.annotation.Nullable
	public Block standIn() {
		String path = this.name.substring(this.name.indexOf(':') + 1);
		boolean wood = false;
		for (String type : WOODS) {
			wood |= path.startsWith(type + "_") || path.contains("_" + type + "_");
		}
		boolean metal = path.contains("copper") || path.contains("iron");
		if (path.endsWith("_sign")) {
			return Blocks.STANDING_SIGN;
		}
		if (path.endsWith("_trapdoor")) {
			return metal ? Blocks.IRON_TRAPDOOR : Blocks.TRAPDOOR;
		}
		if (path.endsWith("_door")) {
			return metal ? Blocks.IRON_DOOR : Blocks.OAK_DOOR;
		}
		if (path.endsWith("_fence_gate")) {
			return Blocks.OAK_FENCE_GATE;
		}
		if (path.endsWith("_fence")) {
			return wood ? Blocks.OAK_FENCE : Blocks.NETHER_BRICK_FENCE;
		}
		if (path.endsWith("_button")) {
			return wood ? Blocks.WOODEN_BUTTON : Blocks.STONE_BUTTON;
		}
		if (path.endsWith("_pressure_plate")) {
			return wood ? Blocks.WOODEN_PRESSURE_PLATE : metal ? Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE : Blocks.STONE_PRESSURE_PLATE;
		}
		if (path.endsWith("_leaves")) {
			return Blocks.LEAVES;
		}
		if (path.endsWith("_sapling") || path.endsWith("_propagule")) {
			return Blocks.SAPLING;
		}
		if (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_stem") || path.endsWith("_hyphae") || path.equals("bamboo_block")) {
			return Blocks.LOG;
		}
		if (path.endsWith("_slab")) {
			return wood ? Blocks.WOODEN_SLAB : Blocks.STONE_SLAB;
		}
		if (path.endsWith("_stairs")) {
			return wood ? Blocks.OAK_STAIRS : Blocks.STONE_STAIRS;
		}
		if (path.endsWith("_wall")) {
			return Blocks.COBBLESTONE_WALL;
		}
		if (path.endsWith("_carpet")) {
			return Blocks.CARPET;
		}
		if (path.endsWith("_wool")) {
			return Blocks.WOOL;
		}
		if (path.contains("glass")) {
			return path.contains("pane") ? Blocks.GLASS_PANE : Blocks.GLASS;
		}
		if (path.contains("terracotta")) {
			return Blocks.HARDENED_CLAY;
		}
		if (path.contains("concrete")) {
			return Blocks.CONCRETE;
		}
		if (metal || path.endsWith("_block") && (path.contains("netherite") || path.contains("raw_"))) {
			return Blocks.IRON_BLOCK;
		}
		if (wood || path.equals("barrel") || path.contains("bookshelf") || path.endsWith("_planks")) {
			return Blocks.PLANKS;
		}
		if (path.contains("deepslate") || path.contains("tuff") || path.contains("calcite") || path.contains("blackstone") || path.contains("basalt")
				|| path.contains("brick") || path.contains("stone") || path.endsWith("_ore")) {
			return Blocks.STONE;
		}
		if (path.contains("mud") || path.contains("soil") || path.contains("nylium") || path.contains("moss")) {
			return Blocks.DIRT;
		}
		return null;
	}

	@Override
	public String toString() {
		return "AbsentBlock{" + this.name + "}";
	}
}
