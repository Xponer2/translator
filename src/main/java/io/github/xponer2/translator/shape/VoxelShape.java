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
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * 1.21 block shape as the list of its boxes (block units, 0..1). 1.12.2 uses the boxes for collision and their
 * bounds for the selection box (see Shapes for the block overrides the converter adds).
 */
public class VoxelShape {
	static final VoxelShape EMPTY = new VoxelShape(Collections.emptyList());
	static final VoxelShape BLOCK = new VoxelShape(Collections.singletonList(Block.FULL_BLOCK_AABB));

	private final List<AxisAlignedBB> boxes;

	VoxelShape(List<AxisAlignedBB> boxes) {
		this.boxes = boxes;
	}

	public boolean isEmpty() {
		return this.boxes.isEmpty();
	}

	/** Each box of the shape to a 1.21 Shapes.DoubleLineConsumer (carried with the API): its one method takes 6 doubles. */
	public void forAllBoxes(Object consumer) {
		java.lang.reflect.Method accept = null;
		for (var method : consumer.getClass().getMethods()) {
			if (method.getParameterCount() == 6 && method.getDeclaringClass() != Object.class) {
				accept = method;
				break;
			}
		}
		if (accept == null) {
			return;
		}
		accept.setAccessible(true);
		for (AxisAlignedBB box : this.boxes) {
			try {
				accept.invoke(consumer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
			} catch (ReflectiveOperationException e) {
				return;
			}
		}
	}

	public void forAllEdges(Object consumer) {
		this.forAllBoxes(consumer);
	}

	public List<AxisAlignedBB> toAabbs() {
		return Collections.unmodifiableList(this.boxes);
	}

	public AxisAlignedBB bounds() {
		if (this.boxes.isEmpty()) {
			throw new UnsupportedOperationException("No bounds for empty shape.");
		}

		var bounds = this.boxes.get(0);
		for (int i = 1; i < this.boxes.size(); i++) {
			bounds = bounds.union(this.boxes.get(i));
		}

		return bounds;
	}

	public VoxelShape move(double x, double y, double z) {
		var moved = new ArrayList<AxisAlignedBB>(this.boxes.size());
		for (var box : this.boxes) {
			moved.add(box.offset(x, y, z));
		}

		return new VoxelShape(moved);
	}

	public VoxelShape optimize() {
		return this;
	}

	public double min(net.minecraft.util.EnumFacing.Axis axis) {
		return this.isEmpty() ? Double.POSITIVE_INFINITY : axis == net.minecraft.util.EnumFacing.Axis.X ? this.bounds().minX : axis == net.minecraft.util.EnumFacing.Axis.Y ? this.bounds().minY : this.bounds().minZ;
	}

	public double max(net.minecraft.util.EnumFacing.Axis axis) {
		return this.isEmpty() ? Double.NEGATIVE_INFINITY : axis == net.minecraft.util.EnumFacing.Axis.X ? this.bounds().maxX : axis == net.minecraft.util.EnumFacing.Axis.Y ? this.bounds().maxY : this.bounds().maxZ;
	}
}
