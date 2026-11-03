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

import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * 1.21 block breaking by code (a mining tool that breaks blocks itself): the drops for a tool, the experience, what
 * a block does after it breaks. 1.12.2 does this inside {@code Block.harvestBlock}; here it is the same steps (silk
 * touch, fortune, the Forge harvest drops event) with the drops returned instead of spawned.
 */
public final class BlockDrops {
	/** {@code Block.getSilkTouchDrop} is protected in 1.12.2. */
	private static final Method SILK_TOUCH_DROP = ObfuscationReflectionHelper.findMethod(Block.class, "func_180643_i", ItemStack.class,
			IBlockState.class);

	private BlockDrops() {
	}

	// --- Block (static) -----------------------------------------------------------------------------------------------

	public static List<ItemStack> getDrops(IBlockState state, WorldServer world, BlockPos pos, @Nullable TileEntity tile) {
		return getDrops(state, world, pos, tile, null, ItemStack.EMPTY);
	}

	public static List<ItemStack> getDrops(IBlockState state, WorldServer world, BlockPos pos, @Nullable TileEntity tile, @Nullable Entity entity,
			ItemStack tool) {
		Block block = state.getBlock();
		EntityPlayer player = entity instanceof EntityPlayer p ? p : null;
		int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool);
		boolean silk = silkTouch(state, world, pos, player, tool);
		NonNullList<ItemStack> drops = NonNullList.create();
		if (silk) {
			try {
				ItemStack drop = (ItemStack) SILK_TOUCH_DROP.invoke(block, state);
				if (!drop.isEmpty()) {
					drops.add(drop);
				}
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Can't get the silk touch drop of " + block, e);
			}
		} else {
			block.getDrops(drops, world, pos, state, fortune);
		}
		float chance = ForgeEventFactory.fireBlockHarvesting(drops, world, pos, state, fortune, 1.0F, silk, player);
		if (chance < 1.0F) {
			drops.removeIf(drop -> world.rand.nextFloat() > chance);
		}
		return drops;
	}

	// --- Block --------------------------------------------------------------------------------------------------------

	public static void popExperience(Block block, WorldServer world, BlockPos pos, int amount) {
		block.dropXpOnBlockBreak(world, pos, amount);
	}

	// --- BlockState ---------------------------------------------------------------------------------------------------

	/** No experience with silk touch, as the 1.12.2 block break event does it. */
	public static int getExpDrop(IBlockState state, World world, BlockPos pos, @Nullable TileEntity tile, @Nullable Entity breaker, ItemStack tool) {
		if (silkTouch(state, world, pos, breaker instanceof EntityPlayer p ? p : null, tool)) {
			return 0;
		}
		return state.getBlock().getExpDrop(state, world, pos, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool));
	}

	/** What 1.12.2 does when an infested block drops: a silverfish comes out (not with silk touch). */
	public static void spawnAfterBreak(IBlockState state, WorldServer world, BlockPos pos, ItemStack tool, boolean dropExperience) {
		if (state.getBlock() instanceof BlockSilverfish && world.getGameRules().getBoolean("doTileDrops")
				&& EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
			var silverfish = new EntitySilverfish(world);
			silverfish.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
			world.spawnEntity(silverfish);
			silverfish.spawnExplosionParticle();
		}
	}

	private static boolean silkTouch(IBlockState state, World world, BlockPos pos, @Nullable EntityPlayer player, ItemStack tool) {
		if (EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, tool) <= 0) {
			return false;
		}
		// Forge's canSilkHarvest only looks at the block (no tile entity), the player may be null
		return state.getBlock().canSilkHarvest(world, pos, state, player);
	}
}
