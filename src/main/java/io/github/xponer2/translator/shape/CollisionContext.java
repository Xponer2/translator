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

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;

/**
 * 1.21 collision context: who asks for the shape. 1.12.2 passes the entity only to collision queries, so the
 * selection box is asked with {@link #empty()}.
 */
public interface CollisionContext {
	CollisionContext EMPTY = () -> null;

	@Nullable
	Entity getEntity();

	static CollisionContext empty() {
		return EMPTY;
	}

	static CollisionContext of(@Nullable Entity entity) {
		return entity == null ? EMPTY : () -> entity;
	}

	/**
	 * 1.21 {@code isHoldingItem(item)}: the entity asking holds it in its main hand (scaffolding and Supplementaries'
	 * blocks change their shape for a player holding them).
	 */
	default boolean isHoldingItem(net.minecraft.item.Item item) {
		return this.getEntity() instanceof net.minecraft.entity.EntityLivingBase living && living.getHeldItemMainhand().getItem() == item;
	}

	/**
	 * 1.21 {@code isAbove(shape, pos, canAscend)}: the entity asking stands above the shape's top at that block (blocks
	 * solid from above only: Supplementaries' bubble block asked every tick and failed). None asking: not above.
	 */
	default boolean isAbove(VoxelShape shape, net.minecraft.util.math.BlockPos pos, boolean canAscend) {
		Entity entity = this.getEntity();
		return entity != null && entity.posY > pos.getY() + shape.max(net.minecraft.util.EnumFacing.Axis.Y) - 1.0E-5F;
	}

	/**
	 * 1.21 {@code canStandOnFluid(above, fluid)}: whether the entity asking walks on that fluid (a strider on lava); no
	 * 1.12.2 entity does (Supplementaries' finite liquid blocks ask for their collision shape).
	 */
	default boolean canStandOnFluid(io.github.xponer2.translator.util.FluidState above, io.github.xponer2.translator.util.FluidState fluid) {
		return false;
	}

	/** 1.21 {@code isDescending()}: the entity asking sneaks (it goes down through scaffolding). */
	default boolean isDescending() {
		return this.getEntity() != null && this.getEntity().isSneaking();
	}
}
