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

import net.minecraft.block.SoundType;

/**
 * The 1.21 {@code SoundType} constants 1.12.2 doesn't have: the nearest 1.12.2 sound set (a 1.21 block keeps its
 * feel: plants rustle, metal clanks, ...). A GETSTATIC of a missing constant reads here.
 */
public final class SoundTypeStatics {
	public static final SoundType EMPTY = SoundType.STONE;

	// plants
	public static final SoundType LILY_PAD = SoundType.PLANT;
	public static final SoundType WET_GRASS = SoundType.PLANT;
	public static final SoundType BAMBOO_SAPLING = SoundType.PLANT;
	public static final SoundType SWEET_BERRY_BUSH = SoundType.PLANT;
	public static final SoundType CROP = SoundType.PLANT;
	public static final SoundType HARD_CROP = SoundType.PLANT;
	public static final SoundType VINE = SoundType.PLANT;
	public static final SoundType NETHER_WART = SoundType.PLANT;
	public static final SoundType STEM = SoundType.WOOD;
	public static final SoundType FUNGUS = SoundType.PLANT;
	public static final SoundType ROOTS = SoundType.PLANT;
	public static final SoundType WEEPING_VINES = SoundType.PLANT;
	public static final SoundType TWISTING_VINES = SoundType.PLANT;
	public static final SoundType NETHER_SPROUTS = SoundType.PLANT;
	public static final SoundType CAVE_VINES = SoundType.PLANT;
	public static final SoundType SPORE_BLOSSOM = SoundType.PLANT;
	public static final SoundType AZALEA = SoundType.PLANT;
	public static final SoundType FLOWERING_AZALEA = SoundType.PLANT;
	public static final SoundType MOSS_CARPET = SoundType.PLANT;
	public static final SoundType PINK_PETALS = SoundType.PLANT;
	public static final SoundType MOSS = SoundType.PLANT;
	public static final SoundType BIG_DRIPLEAF = SoundType.PLANT;
	public static final SoundType SMALL_DRIPLEAF = SoundType.PLANT;
	public static final SoundType HANGING_ROOTS = SoundType.PLANT;
	public static final SoundType AZALEA_LEAVES = SoundType.PLANT;
	public static final SoundType GLOW_LICHEN = SoundType.PLANT;
	public static final SoundType CHERRY_SAPLING = SoundType.PLANT;
	public static final SoundType CHERRY_LEAVES = SoundType.PLANT;
	public static final SoundType MANGROVE_ROOTS = SoundType.WOOD;
	public static final SoundType SCULK_VEIN = SoundType.PLANT;
	public static final SoundType FROGSPAWN = SoundType.SLIME;
	public static final SoundType COBWEB = SoundType.CLOTH;

	// earth
	public static final SoundType ROOTED_DIRT = SoundType.GROUND;
	public static final SoundType MUD = SoundType.GROUND;
	public static final SoundType MUDDY_MANGROVE_ROOTS = SoundType.GROUND;
	public static final SoundType PACKED_MUD = SoundType.GROUND;
	public static final SoundType SOUL_SOIL = SoundType.SAND;
	public static final SoundType SOUL_SAND = SoundType.SAND;
	public static final SoundType SUSPICIOUS_SAND = SoundType.SAND;
	public static final SoundType SUSPICIOUS_GRAVEL = SoundType.GROUND;
	public static final SoundType POWDER_SNOW = SoundType.SNOW;

	// stone
	public static final SoundType CORAL_BLOCK = SoundType.STONE;
	public static final SoundType NYLIUM = SoundType.STONE;
	public static final SoundType BASALT = SoundType.STONE;
	public static final SoundType NETHERRACK = SoundType.STONE;
	public static final SoundType NETHER_BRICKS = SoundType.STONE;
	public static final SoundType NETHER_ORE = SoundType.STONE;
	public static final SoundType NETHER_GOLD_ORE = SoundType.STONE;
	public static final SoundType BONE_BLOCK = SoundType.STONE;
	public static final SoundType ANCIENT_DEBRIS = SoundType.STONE;
	public static final SoundType GILDED_BLACKSTONE = SoundType.STONE;
	public static final SoundType TUFF = SoundType.STONE;
	public static final SoundType TUFF_BRICKS = SoundType.STONE;
	public static final SoundType POLISHED_TUFF = SoundType.STONE;
	public static final SoundType CALCITE = SoundType.STONE;
	public static final SoundType DRIPSTONE_BLOCK = SoundType.STONE;
	public static final SoundType POINTED_DRIPSTONE = SoundType.STONE;
	public static final SoundType DEEPSLATE = SoundType.STONE;
	public static final SoundType DEEPSLATE_BRICKS = SoundType.STONE;
	public static final SoundType DEEPSLATE_TILES = SoundType.STONE;
	public static final SoundType POLISHED_DEEPSLATE = SoundType.STONE;
	public static final SoundType MUD_BRICKS = SoundType.STONE;
	public static final SoundType SCULK = SoundType.STONE;
	public static final SoundType SCULK_SENSOR = SoundType.STONE;
	public static final SoundType SCULK_CATALYST = SoundType.STONE;
	public static final SoundType SCULK_SHRIEKER = SoundType.STONE;
	public static final SoundType DECORATED_POT = SoundType.STONE;
	public static final SoundType DECORATED_POT_CRACKED = SoundType.STONE;

	// metal
	public static final SoundType LANTERN = SoundType.METAL;
	public static final SoundType NETHERITE_BLOCK = SoundType.METAL;
	public static final SoundType LODESTONE = SoundType.METAL;
	public static final SoundType CHAIN = SoundType.METAL;
	public static final SoundType COPPER = SoundType.METAL;
	public static final SoundType COPPER_BULB = SoundType.METAL;
	public static final SoundType COPPER_GRATE = SoundType.METAL;
	public static final SoundType TRIAL_SPAWNER = SoundType.METAL;
	public static final SoundType SPAWNER = SoundType.METAL;
	public static final SoundType VAULT = SoundType.METAL;
	public static final SoundType HEAVY_CORE = SoundType.METAL;

	// glass
	public static final SoundType AMETHYST = SoundType.GLASS;
	public static final SoundType AMETHYST_CLUSTER = SoundType.GLASS;
	public static final SoundType SMALL_AMETHYST_BUD = SoundType.GLASS;
	public static final SoundType MEDIUM_AMETHYST_BUD = SoundType.GLASS;
	public static final SoundType LARGE_AMETHYST_BUD = SoundType.GLASS;
	public static final SoundType FROGLIGHT = SoundType.GLASS;

	// wood
	public static final SoundType BAMBOO = SoundType.WOOD;
	public static final SoundType SCAFFOLDING = SoundType.LADDER;
	public static final SoundType NETHER_WOOD = SoundType.WOOD;
	public static final SoundType BAMBOO_WOOD = SoundType.WOOD;
	public static final SoundType CHERRY_WOOD = SoundType.WOOD;
	public static final SoundType HANGING_SIGN = SoundType.WOOD;
	public static final SoundType NETHER_WOOD_HANGING_SIGN = SoundType.WOOD;
	public static final SoundType BAMBOO_WOOD_HANGING_SIGN = SoundType.WOOD;
	public static final SoundType CHERRY_WOOD_HANGING_SIGN = SoundType.WOOD;
	public static final SoundType CHISELED_BOOKSHELF = SoundType.WOOD;

	// soft
	public static final SoundType CANDLE = SoundType.CLOTH;
	public static final SoundType SPONGE = SoundType.CLOTH;
	public static final SoundType WET_SPONGE = SoundType.CLOTH;
	public static final SoundType SHROOMLIGHT = SoundType.CLOTH;
	public static final SoundType WART_BLOCK = SoundType.CLOTH;
	public static final SoundType HONEY_BLOCK = SoundType.SLIME;

	private SoundTypeStatics() {
	}
}
