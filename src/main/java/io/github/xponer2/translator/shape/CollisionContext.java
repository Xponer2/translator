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
}
