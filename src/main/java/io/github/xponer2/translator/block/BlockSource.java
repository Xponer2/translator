package io.github.xponer2.translator.block;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.21 dispenser source (a record there) on the 1.12.2 IBlockSource.
 */
public final class BlockSource {
	private final IBlockSource source;

	public BlockSource(IBlockSource source) {
		this.source = source;
	}

	public World level() {
		return this.source.getWorld();
	}

	public BlockPos pos() {
		return this.source.getBlockPos();
	}

	public IBlockState state() {
		return this.source.getBlockState();
	}

	@Nullable
	public TileEntity blockEntity() {
		return this.source.getBlockTileEntity();
	}

	public IBlockSource source() {
		return this.source;
	}
}
