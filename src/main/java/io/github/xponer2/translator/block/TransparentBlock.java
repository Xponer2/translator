package io.github.xponer2.translator.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code TransparentBlock}: glass that hides the faces between two blocks of the same kind.
 */
public class TransparentBlock extends PropertiesBlock {
	public TransparentBlock(BlockProperties properties) {
		super(properties);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.CUTOUT;
	}

	@Override
	@SideOnly(Side.CLIENT)
	@SuppressWarnings("deprecation")
	public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		var other = world.getBlockState(pos.offset(side));
		return other.getBlock() != this && super.shouldSideBeRendered(state, world, pos, side);
	}
}
