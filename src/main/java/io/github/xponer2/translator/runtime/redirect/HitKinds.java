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
package io.github.xponer2.translator.runtime.redirect;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;

/**
 * 1.21's two kinds of hit result on 1.12.2's one: {@code BlockHitResult} and {@code EntityHitResult} are both a
 * RayTraceResult here, told apart by its type (the table's I lines send {@code instanceof} here). The entity hit was a
 * carried 1.21 class before: no 1.12.2 hit was ever one, and it couldn't be made (its constructor needs 1.21's
 * HitResult), so the entity branch of every mod's ray trace and projectile hit never ran.
 */
public final class HitKinds {
	private HitKinds() {
	}

	/** {@code hit instanceof EntityHitResult}. */
	public static boolean isEntityHit(@Nullable Object hit) {
		return hit instanceof RayTraceResult && ((RayTraceResult) hit).typeOfHit == RayTraceResult.Type.ENTITY;
	}

	/** {@code hit instanceof BlockHitResult}: a block or a miss (1.21's miss is a block hit result). */
	public static boolean isBlockHit(@Nullable Object hit) {
		return hit instanceof RayTraceResult && ((RayTraceResult) hit).typeOfHit != RayTraceResult.Type.ENTITY;
	}

	/** 1.21 {@code entityHitResult.getEntity()}. */
	@Nullable
	public static Entity getEntity(RayTraceResult hit) {
		return hit.entityHit;
	}
}
