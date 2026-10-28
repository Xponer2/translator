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
package io.github.xponer2.translator.content;

import java.util.Random;

import io.github.xponer2.translator.Translator;
import io.github.xponer2.translator.util.ItemIds;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

/**
 * The 1.21 vanilla copper 1.12.2 doesn't have: {@code translator:copper_ingot} and {@code translator:copper_ore} (the
 * ids {@code minecraft:copper_ingot} / {@code minecraft:copper_ore} of the ported mods' data). Only used when no other
 * mod adds copper (an {@code ingotCopper} in the OreDictionary after init): then the Translator's copper hides itself
 * (no creative tab, hidden in JEI, no ore generation) and the 1.21 ids are that mod's copper. The items stay
 * registered either way, so worlds keep them when a copper mod is added or removed.
 */
public final class Copper {
	public static final ResourceLocation INGOT_ID = new ResourceLocation(Translator.MOD_ID, "copper_ingot");
	public static final ResourceLocation ORE_ID = new ResourceLocation(Translator.MOD_ID, "copper_ore");

	public static Item ingot;
	public static Block ore;
	private static boolean active;
	private static boolean generateOre = true;

	private Copper() { }

	/** True if the Translator's copper is in use (no other mod adds copper); known after {@link #setup}. */
	public static boolean isActive() {
		return active;
	}

	public static void readConfig(Configuration config) {
		generateOre = config.getBoolean("generateCopperOre", "copper", true,
				"Generate copper ore in the overworld when no other mod adds copper (vanilla 1.12.2 has none).");
		if (config.hasChanged()) {
			config.save();
		}
	}

	public static final class Registrar {
		@SubscribeEvent
		public void onBlocks(RegistryEvent.Register<Block> event) {
			ore = new OreBlock().setTranslationKey(Translator.MOD_ID + ".copper_ore").setRegistryName(ORE_ID);
			event.getRegistry().register(ore);
		}

		@SubscribeEvent
		public void onItems(RegistryEvent.Register<Item> event) {
			ingot = new Item().setTranslationKey(Translator.MOD_ID + ".copper_ingot").setRegistryName(INGOT_ID);
			event.getRegistry().register(ingot);
			event.getRegistry().register(new ItemBlock(ore).setRegistryName(ORE_ID));
		}
	}

	// like the 1.21 copper ore: a stone pickaxe mines it; drops itself like the 1.12.2 iron ore
	private static final class OreBlock extends Block {
		OreBlock() {
			super(Material.ROCK);
			this.setHardness(3.0F);
			this.setResistance(3.0F);
			this.setSoundType(SoundType.STONE);
			this.setHarvestLevel("pickaxe", 1);
		}
	}

	/**
	 * Decides whose copper the 1.21 ids are, once all mods have registered their OreDictionary entries (post init,
	 * before the recipes and tags of the ported mods load).
	 */
	public static void setup() {
		var other = OreDictionary.getOres("ingotCopper", false).stream().filter(s -> s.getItem() != ingot).findFirst();
		active = !other.isPresent();
		if (active) {
			ingot.setCreativeTab(CreativeTabs.MATERIALS);
			ore.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
			OreDictionary.registerOre("ingotCopper", ingot);
			OreDictionary.registerOre("oreCopper", ore);
			GameRegistry.addSmelting(ore, new ItemStack(ingot), 0.7F);
			ItemIds.registerFlattened(new ResourceLocation("minecraft", "copper_ingot"), ingot, 0);
			ItemIds.registerFlattened(new ResourceLocation("minecraft", "copper_ore"), Item.getItemFromBlock(ore), 0);
			GameRegistry.registerWorldGenerator(new OreGenerator(), 0);
			Translator.LOGGER.info("No other mod adds copper: the Translator's copper is used");
		} else {
			var copper = other.get();
			// ore already in a world still smelts into copper
			GameRegistry.addSmelting(ore, new ItemStack(copper.getItem(), 1, meta(copper)), 0.7F);
			ItemIds.registerFlattened(new ResourceLocation("minecraft", "copper_ingot"), copper.getItem(), meta(copper));
			OreDictionary.getOres("oreCopper", false).stream().findFirst().ifPresent(o ->
					ItemIds.registerFlattened(new ResourceLocation("minecraft", "copper_ore"), o.getItem(), meta(o)));
			Translator.LOGGER.info("Copper comes from {}: the Translator's copper is hidden", copper.getItem().getRegistryName());
		}
	}

	private static int meta(ItemStack stack) {
		return stack.getMetadata() == OreDictionary.WILDCARD_VALUE ? 0 : stack.getMetadata();
	}

	/**
	 * Copper ore like the 1.21 one scaled to the 1.12.2 height: veins of up to 10 blocks, 16 tries per chunk, y 0..96
	 * peaking at y 48, in the stone of the overworld.
	 */
	private static final class OreGenerator implements IWorldGenerator {
		private static final int VEIN_SIZE = 10;
		private static final int TRIES_PER_CHUNK = 16;
		private static final int PEAK_Y = 48;

		private WorldGenMinable generator;

		@Override
		public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
			if (!generateOre || world.provider.getDimensionType() != DimensionType.OVERWORLD) {
				return;
			}
			if (this.generator == null) {
				this.generator = new WorldGenMinable(ore.getDefaultState(), VEIN_SIZE);
			}
			for (int i = 0; i < TRIES_PER_CHUNK; i++) {
				// from the chunk corner like the vanilla ores: WorldGenMinable adds the offset of 8 itself
				int x = (chunkX << 4) + random.nextInt(16);
				int y = random.nextInt(PEAK_Y + 1) + random.nextInt(PEAK_Y + 1);
				int z = (chunkZ << 4) + random.nextInt(16);
				this.generator.generate(world, random, new BlockPos(x, y, z));
			}
		}
	}
}
