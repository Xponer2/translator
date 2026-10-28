package io.github.xponer2.translator.shape;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * 1.21 shape factory. {@link #box16} is 1.21 {@code Block.box} (pixel units). The {@code addCollisionBoxes} /
 * {@code selectionBox} helpers are what the converter's block overrides call with the block's 1.21 getShape.
 */
public final class Shapes {
	private Shapes() {
	}

	public static VoxelShape empty() {
		return VoxelShape.EMPTY;
	}

	public static VoxelShape block() {
		return VoxelShape.BLOCK;
	}

	public static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
		var list = new ArrayList<AxisAlignedBB>(1);
		list.add(new AxisAlignedBB(x1, y1, z1, x2, y2, z2));
		return new VoxelShape(list);
	}

	public static VoxelShape box16(double x1, double y1, double z1, double x2, double y2, double z2) {
		return box(x1 / 16.0, y1 / 16.0, z1 / 16.0, x2 / 16.0, y2 / 16.0, z2 / 16.0);
	}

	public static VoxelShape create(AxisAlignedBB box) {
		return box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
	}

	public static VoxelShape or(VoxelShape first, VoxelShape... others) {
		var list = new ArrayList<>(first.toAabbs());
		for (var shape : others) {
			list.addAll(shape.toAabbs());
		}

		return new VoxelShape(list);
	}

	/** Selection box: the bounds of the shape (1.12.2 has one box), the full block for an empty shape. */
	public static AxisAlignedBB selectionBox(VoxelShape shape) {
		return shape.isEmpty() ? Block.FULL_BLOCK_AABB : shape.bounds();
	}

	/** Adds the boxes of the shape that intersect the entity box, like Block#addCollisionBoxToList. */
	public static void addCollisionBoxes(VoxelShape shape, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> list) {
		for (var box : shape.toAabbs()) {
			var moved = box.offset(pos);
			if (entityBox.intersects(moved)) {
				list.add(moved);
			}
		}
	}

	/** Collision bounds for Block#getCollisionBoundingBox: null (no collision) for an empty shape. */
	@Nullable
	public static AxisAlignedBB collisionBox(VoxelShape shape) {
		return shape.isEmpty() ? Block.NULL_AABB : shape.bounds();
	}

	public static boolean isFullBlock(IBlockState state, VoxelShape shape) {
		return shape.toAabbs().size() == 1 && shape.toAabbs().get(0).equals(Block.FULL_BLOCK_AABB);
	}

	/**
	 * Whether the 1.21 shape of a state fills its block: 1.12.2 asks without a world (isOpaqueCube, isFullCube), and a
	 * block that isn't full mustn't hide the faces of its neighbours. A shape that needs the world counts as not full.
	 */
	public static boolean isFullBlock(java.util.function.Supplier<VoxelShape> shape) {
		try {
			var boxes = shape.get().toAabbs();
			return boxes.size() == 1 && boxes.get(0).equals(Block.FULL_BLOCK_AABB);
		} catch (RuntimeException e) {
			return false;
		}
	}
}
