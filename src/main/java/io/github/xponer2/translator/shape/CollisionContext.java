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

	/** 1.21 {@code isDescending()}: the entity asking sneaks (it goes down through scaffolding). */
	default boolean isDescending() {
		return this.getEntity() != null && this.getEntity().isSneaking();
	}
}
