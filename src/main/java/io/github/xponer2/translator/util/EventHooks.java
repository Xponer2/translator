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
