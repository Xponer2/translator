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
package io.github.xponer2.translator.util;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.terraingen.TerrainGen;

/**
 * NeoForge EventHooks (event firing helpers) on the 1.12.2 Forge event factory.
 */
public final class EventHooks {
	private EventHooks() {
	}

	/** 1.21 BonemealEvent: canceled = another mod handled the bonemeal, successful = it did grow something. */
	public static BonemealResult fireBonemealEvent(@Nullable EntityPlayer player, World level, BlockPos pos, IBlockState state, ItemStack stack) {
		// 1.12.2 needs a player; dispensers use the fake player like vanilla does
		var user = player != null ? player : level instanceof WorldServer server ? FakePlayerFactory.getMinecraft(server) : null;
		if (user == null) {
			return new BonemealResult(false, false);
		}

		int result = ForgeEventFactory.onApplyBonemeal(user, level, pos, state, stack, EnumHand.MAIN_HAND);
		return new BonemealResult(result != 0, result > 0);
	}

	/** 1.21 BlockGrowFeatureEvent for saplings: false if a mod stopped the tree from growing. */
	/**
	 * NeoForge {@code getItemBurnTime(stack, burnTime, recipeType)}: Forge 1.12.2's fuel event (its constructor takes no
	 * recipe type: the carried NeoForge one couldn't be made, and Iron Furnaces' furnaces threw at every fuel).
	 */
	public static int getItemBurnTime(ItemStack stack, int burnTime, @Nullable io.github.xponer2.translator.recipe.RecipeType<?> recipeType) {
		var event = new net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent(stack, burnTime);
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
		return event.getBurnTime();
	}

	public static boolean saplingGrowTree(World level, java.util.Random random, BlockPos pos) {
		return TerrainGen.saplingGrowTree(level, random, pos);
	}

	public static final class BonemealResult {
		private final boolean canceled;
		private final boolean successful;

		BonemealResult(boolean canceled, boolean successful) {
			this.canceled = canceled;
			this.successful = successful;
		}

		public boolean isCanceled() {
			return this.canceled;
		}

		public boolean isSuccessful() {
			return this.successful;
		}
	}
}
