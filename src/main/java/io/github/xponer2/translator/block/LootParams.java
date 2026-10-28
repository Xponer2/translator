package io.github.xponer2.translator.block;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * 1.21 loot parameters of a block drop, built from the 1.12.2 Block#getDrops arguments and the harvesting player.
 * The converter adds the 1.12.2 getDrops override to blocks declaring the 1.21 one.
 */
public final class LootParams {
	private LootParams() {
	}

	public static final class Builder {
		private final IBlockAccess level;
		private final Map<LootContextParams.Param<?>, Object> params = new HashMap<>();

		public Builder(IBlockAccess level, BlockPos pos, @Nullable EntityPlayer harvester, @Nullable net.minecraft.block.state.IBlockState state) {
			this.level = level;
			this.params.put(LootContextParams.ORIGIN, new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
			if (harvester != null) {
				this.params.put(LootContextParams.THIS_ENTITY, harvester);
				this.params.put(LootContextParams.TOOL, harvester.getHeldItemMainhand());
			}
			if (state != null) {
				this.params.put(LootContextParams.BLOCK_STATE, state);
			}
			var tile = level.getTileEntity(pos);
			if (tile != null) {
				this.params.put(LootContextParams.BLOCK_ENTITY, tile);
			}
		}

		/** The world (1.12.2 passes an IBlockAccess; it is the world when blocks are broken). */
		public World getLevel() {
			return (World) this.level;
		}

		@Nullable
		@SuppressWarnings("unchecked")
		public <T> T getOptionalParameter(LootContextParams.Param<T> param) {
			return (T) this.params.get(param);
		}

		@SuppressWarnings("unchecked")
		public <T> T getParameter(LootContextParams.Param<T> param) {
			var value = (T) this.params.get(param);
			if (value == null) {
				throw new IllegalArgumentException("No parameter " + param);
			}
			return value;
		}

		public <T> Builder withParameter(LootContextParams.Param<T> param, T value) {
			this.params.put(param, value);
			return this;
		}

		@Nullable
		public ItemStack tool() {
			return this.getOptionalParameter(LootContextParams.TOOL);
		}
	}
}
