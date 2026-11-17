/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.loot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The 1.21 loot table of every block of a 1.21.1 mod, whatever 1.12.2 block class it ends up on. The Translator's own
 * block bases read the table in their getDrops; a mod block on a 1.12.2 class (a stained glass, a bush, a tall flower,
 * leaves) dropped as 1.12.2 drops: Macaw's mosaic glass and Biomes O' Plenty's tall flowers dropped nothing, Farmer's
 * Delight's wild crops themselves instead of their produce, Twilight Forest's leaves themselves without shears.
 */
public final class HarvestLoot {
	private static final Map<Class<?>, Boolean> OWN_LOOT = new ConcurrentHashMap<>();

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onHarvest(BlockEvent.HarvestDropsEvent event) {
		NonNullList<ItemStack> drops = drops(event.getState().getBlock(), event.getWorld(), event.getPos(), event.getState(), event.getFortuneLevel(),
				event.getHarvester(), event.getWorld().rand);
		if (drops != null) {
			event.getDrops().clear();
			event.getDrops().addAll(drops);
			event.setDropChance(1.0F);
		}
	}

	/**
	 * The drops of the block's 1.21 table when the 1.12.2 drops didn't come from it: null for a block of 1.12.2 or of
	 * a converted mod, one whose class reads the table already, or one without a table.
	 */
	@javax.annotation.Nullable
	public static NonNullList<ItemStack> drops(Block block, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos,
			net.minecraft.block.state.IBlockState state, int fortune, @javax.annotation.Nullable net.minecraft.entity.player.EntityPlayer player,
			java.util.Random random) {
		ResourceLocation id = block.getRegistryName();
		if (id == null || !io.github.xponer2.translator.runtime.RuntimeMods.isRuntimeNamespace(id.getNamespace()) || ownLoot(block.getClass())) {
			return null;
		}
		NonNullList<ItemStack> drops = NonNullList.create();
		return BlockLoot.drop(BlockLoot.tableOf(block, null), drops, world, pos, state, fortune, player, random) ? drops : null;
	}

	/** Whether a Translator class between the block's class and 1.12.2's reads the table in its getDrops. */
	private static boolean ownLoot(Class<?> type) {
		return OWN_LOOT.computeIfAbsent(type, start -> {
			for (Class<?> c = start; c != null && c != Block.class; c = c.getSuperclass()) {
				if (!c.getName().startsWith("io.github.xponer2.translator.")) {
					continue;
				}
				try {
					c.getDeclaredMethod("getDrops", NonNullList.class, net.minecraft.world.IBlockAccess.class, net.minecraft.util.math.BlockPos.class,
							net.minecraft.block.state.IBlockState.class, int.class);
					return true;
				} catch (NoSuchMethodException e) {
					// its parent
				}
			}
			return false;
		});
	}
}
