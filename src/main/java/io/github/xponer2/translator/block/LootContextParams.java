package io.github.xponer2.translator.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.Vec3d;

/**
 * 1.21 loot context parameters of block drops.
 */
public final class LootContextParams {
	public static final Param<Vec3d> ORIGIN = new Param<>("origin");
	public static final Param<Entity> THIS_ENTITY = new Param<>("this_entity");
	public static final Param<ItemStack> TOOL = new Param<>("tool");
	public static final Param<IBlockState> BLOCK_STATE = new Param<>("block_state");
	public static final Param<TileEntity> BLOCK_ENTITY = new Param<>("block_entity");
	public static final Param<Float> EXPLOSION_RADIUS = new Param<>("explosion_radius");

	private LootContextParams() {
	}

	public static final class Param<T> {
		private final String name;

		Param(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}
