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

import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** 1.21 {@code ClipContext}: a ray from one point to another, traced with {@link #clip}. */
public class ClipContext {
	private final Vec3d from;
	private final Vec3d to;
	private final Block block;
	private final Fluid fluid;

	public ClipContext(Vec3d from, Vec3d to, Block block, Fluid fluid, @Nullable Entity entity) {
		this.from = from;
		this.to = to;
		this.block = block;
		this.fluid = fluid;
	}

	/** 1.21 {@code new ClipContext(from, to, block, fluid, collisionContext)} (8 mods ray-trace with one). */
	public ClipContext(Vec3d from, Vec3d to, Block block, Fluid fluid, io.github.xponer2.translator.shape.CollisionContext context) {
		this(from, to, block, fluid, context == null ? null : context.getEntity());
	}

	public Vec3d getFrom() {
		return this.from;
	}

	public Vec3d getTo() {
		return this.to;
	}

	/** 1.21 {@code level.clip(context)}: the block hit, or a miss at the end of the ray. */
	public static RayTraceResult clip(World world, ClipContext context) {
		boolean fluids = context.fluid != Fluid.NONE;
		// COLLIDER traces through blocks without a collision box (1.21 MiningGadgets relies on it), OUTLINE doesn't
		boolean ignoreNoCollision = context.block == Block.COLLIDER;
		RayTraceResult hit = world.rayTraceBlocks(context.from, context.to, fluids, ignoreNoCollision, false);
		if (hit != null) {
			return hit;
		}
		var direction = context.to.subtract(context.from);
		return new RayTraceResult(RayTraceResult.Type.MISS, context.to,
				EnumFacing.getFacingFromVector((float) direction.x, (float) direction.y, (float) direction.z), new BlockPos(context.to));
	}

	public enum Block {
		COLLIDER,
		OUTLINE,
		VISUAL,
		FALLDAMAGE_RESETTING
	}

	public enum Fluid {
		NONE,
		SOURCE_ONLY,
		ANY,
		WATER
	}
}
