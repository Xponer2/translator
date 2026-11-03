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

	/**
	 * 1.21 {@code Shapes.join}. The shapes here are lists of boxes: a union is exact, the other operations keep the
	 * first shape (the boxes 1.12.2 uses for selection and collision don't need more).
	 */
	public static VoxelShape join(VoxelShape first, VoxelShape second, BooleanOp operation) {
		if (operation == BooleanOp.OR) {
			return or(first, second);
		}
		return operation == BooleanOp.SECOND ? second : first;
	}

	/** 1.21 {@code Shapes.create(x1, y1, z1, x2, y2, z2)} (block units, like {@link #box}). */
	public static VoxelShape create(double x1, double y1, double z1, double x2, double y2, double z2) {
		return box(x1, y1, z1, x2, y2, z2);
	}

	/** 1.21 {@code Shapes.INFINITY}: a box larger than any world. */
	public static final VoxelShape INFINITY = box(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
			Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

	public static VoxelShape joinUnoptimized(VoxelShape first, VoxelShape second, BooleanOp operation) {
		return join(first, second, operation);
	}

	/**
	 * 1.21 {@code Shapes.joinIsNotEmpty}: whether combining the shapes leaves anything, from where the boxes are: in
	 * both, only in the first, only in the second (a box counts as covered when one box of the other shape holds it).
	 */
	public static boolean joinIsNotEmpty(VoxelShape first, VoxelShape second, BooleanOp operation) {
		var a = first == null ? java.util.Collections.<AxisAlignedBB>emptyList() : first.toAabbs();
		var b = second == null ? java.util.Collections.<AxisAlignedBB>emptyList() : second.toAabbs();
		if (operation.apply(true, true) && intersects(a, b)) {
			return true;
		}
		if (operation.apply(true, false) && notCovered(a, b)) {
			return true;
		}
		return operation.apply(false, true) && notCovered(b, a);
	}

	private static boolean intersects(List<AxisAlignedBB> a, List<AxisAlignedBB> b) {
		for (var x : a) {
			for (var y : b) {
				if (x.intersects(y)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean notCovered(List<AxisAlignedBB> boxes, List<AxisAlignedBB> cover) {
		for (var box : boxes) {
			boolean covered = false;
			for (var other : cover) {
				if (other.minX <= box.minX && other.minY <= box.minY && other.minZ <= box.minZ
						&& other.maxX >= box.maxX && other.maxY >= box.maxY && other.maxZ >= box.maxZ) {
					covered = true;
					break;
				}
			}
			if (!covered) {
				return true;
			}
		}
		return false;
	}

	/** 1.21 {@code Shapes.collide}: how far an entity box can move along an axis past the shapes (1.12.2 offsets). */
	public static double collide(net.minecraft.util.EnumFacing.Axis axis, AxisAlignedBB box, Iterable<VoxelShape> shapes, double distance) {
		for (var shape : shapes) {
			if (shape == null) {
				continue;
			}
			for (var other : shape.toAabbs()) {
				distance = switch (axis) {
					case X -> other.calculateXOffset(box, distance);
					case Y -> other.calculateYOffset(box, distance);
					case Z -> other.calculateZOffset(box, distance);
				};
			}
		}
		return distance;
	}

	/** The common 2-shape {@code or} (another method than the varargs one for compiled code). */
	public static VoxelShape or(VoxelShape first, VoxelShape second) {
		return or(first, new VoxelShape[] {second});
	}

	public static VoxelShape or(VoxelShape first, VoxelShape... others) {
		// a shape a mod made with what the Translator lacks is null: left out
		var list = first == null ? new ArrayList<net.minecraft.util.math.AxisAlignedBB>() : new ArrayList<>(first.toAabbs());
		for (var shape : others) {
			if (shape != null) {
				list.addAll(shape.toAabbs());
			}
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
