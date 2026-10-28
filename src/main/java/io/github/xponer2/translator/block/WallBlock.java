package io.github.xponer2.translator.block;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * 1.21 {@code WallBlock} with the 1.12.2 wall states: {@code up} and boolean {@code north/east/south/west} sides (1.12.2
 * walls have no tall sides), computed from the neighbors like the 1.12.2 cobblestone wall.
 */
public class WallBlock extends PropertiesBlock {
	public static final PropertyBool UP = PropertyBool.create("up");
	public static final PropertyBool NORTH = PropertyBool.create("north");
	public static final PropertyBool EAST = PropertyBool.create("east");
	public static final PropertyBool SOUTH = PropertyBool.create("south");
	public static final PropertyBool WEST = PropertyBool.create("west");
	// post + sides as boxes, indexed by the side bits (south 1, west 2, north 4, east 8)
	private static final AxisAlignedBB[] BOXES = boxes(1.0D);
	private static final AxisAlignedBB[] COLLISION_BOXES = boxes(1.5D);

	public WallBlock(BlockProperties properties) {
		super(properties.noOcclusion());
		this.setDefaultState(this.blockState.getBaseState().withProperty(UP, false).withProperty(NORTH, false)
				.withProperty(EAST, false).withProperty(SOUTH, false).withProperty(WEST, false));
	}

	private static AxisAlignedBB[] boxes(double height) {
		var result = new AxisAlignedBB[16];
		for (int i = 0; i < 16; i++) {
			double minX = (i & 2) != 0 ? 0.0D : 0.25D;
			double maxX = (i & 8) != 0 ? 1.0D : 0.75D;
			double minZ = (i & 4) != 0 ? 0.0D : 0.25D;
			double maxZ = (i & 1) != 0 ? 1.0D : 0.75D;
			// a straight wall is thinner, like the 1.12.2 wall
			if (i == 5) {
				minX = 0.3125D;
				maxX = 0.6875D;
			} else if (i == 10) {
				minZ = 0.3125D;
				maxZ = 0.6875D;
			}
			result[i] = new AxisAlignedBB(minX, 0.0D, minZ, maxX, height, maxZ);
		}
		return result;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, UP, NORTH, EAST, SOUTH, WEST);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return 0;
	}

	@Override
	@SuppressWarnings("deprecation")
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
		var north = this.canConnectTo(world, pos, EnumFacing.NORTH);
		var east = this.canConnectTo(world, pos, EnumFacing.EAST);
		var south = this.canConnectTo(world, pos, EnumFacing.SOUTH);
		var west = this.canConnectTo(world, pos, EnumFacing.WEST);
		var straight = (north && south && !east && !west) || (!north && !south && east && west);
		var up = !straight || !world.isAirBlock(pos.up());
		return state.withProperty(UP, up).withProperty(NORTH, north).withProperty(EAST, east).withProperty(SOUTH, south).withProperty(WEST, west);
	}

	private boolean canConnectTo(IBlockAccess world, BlockPos pos, EnumFacing side) {
		var other = pos.offset(side);
		var state = world.getBlockState(other);
		var block = state.getBlock();
		var shape = state.getBlockFaceShape(world, other, side.getOpposite());
		var exceptional = isExceptBlockForAttachWithPiston(block) || block instanceof BlockFenceGate;
		return block instanceof WallBlock || block instanceof BlockWall || shape == BlockFaceShape.MIDDLE_POLE_THICK
				|| (shape == BlockFaceShape.SOLID && !exceptional);
	}

	private static int index(IBlockState state) {
		int i = 0;
		if (state.getValue(SOUTH)) i |= 1;
		if (state.getValue(WEST)) i |= 2;
		if (state.getValue(NORTH)) i |= 4;
		if (state.getValue(EAST)) i |= 8;
		return i;
	}

	@Override
	@SuppressWarnings("deprecation")
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return BOXES[index(state.getActualState(world, pos))];
	}

	@Override
	@SuppressWarnings("deprecation")
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
			@Nullable Entity entity, boolean isActualState) {
		if (!isActualState) {
			state = state.getActualState(world, pos);
		}
		addCollisionBoxToList(pos, entityBox, boxes, COLLISION_BOXES[index(state)]);
	}

	@Nullable
	@Override
	@SuppressWarnings("deprecation")
	public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return COLLISION_BOXES[index(state.getActualState(world, pos))];
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isPassable(IBlockAccess world, BlockPos pos) {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
		return face != EnumFacing.UP && face != EnumFacing.DOWN ? BlockFaceShape.MIDDLE_POLE_THICK : BlockFaceShape.CENTER_BIG;
	}

	@Override
	public boolean canPlaceTorchOnTop(IBlockState state, IBlockAccess world, BlockPos pos) {
		return true;
	}
}
