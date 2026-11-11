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
package io.github.xponer2.translator.registry;

import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.tileentity.TileEntityStructure;

/**
 * The 1.21 vanilla block entity types the mods read ({@code BlockEntityType.CHEST}: the converter takes a static field
 * BlockEntityType lacks from here, the table's R line), over 1.12.2's tile entities. They were null: Twilight Forest's
 * registration of its chests and their renderer, AE2's test plots, Supplementaries' and Create's checks failed on them.
 * A class of its own: made when a mod first reads one, after the game's blocks exist.
 */
public final class VanillaBlockEntityTypes {
	public static final BlockEntityType<?> CHEST = BlockEntityType.vanilla("chest", TileEntityChest.class, Blocks.CHEST);
	public static final BlockEntityType<?> TRAPPED_CHEST = BlockEntityType.vanilla("trapped_chest", TileEntityChest.class, Blocks.TRAPPED_CHEST);
	public static final BlockEntityType<?> DISPENSER = BlockEntityType.vanilla("dispenser", TileEntityDispenser.class, Blocks.DISPENSER);
	public static final BlockEntityType<?> HOPPER = BlockEntityType.vanilla("hopper", TileEntityHopper.class, Blocks.HOPPER);
	public static final BlockEntityType<?> BED = BlockEntityType.vanilla("bed", TileEntityBed.class, Blocks.BED);
	public static final BlockEntityType<?> SKULL = BlockEntityType.vanilla("skull", TileEntitySkull.class, Blocks.SKULL);
	public static final BlockEntityType<?> STRUCTURE_BLOCK = BlockEntityType.vanilla("structure_block", TileEntityStructure.class, Blocks.STRUCTURE_BLOCK);
	public static final BlockEntityType<?> ENCHANTING_TABLE = BlockEntityType.vanilla("enchanting_table", TileEntityEnchantmentTable.class, Blocks.ENCHANTING_TABLE);

	private VanillaBlockEntityTypes() {
	}
}
