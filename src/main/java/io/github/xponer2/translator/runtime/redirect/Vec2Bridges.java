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

import net.minecraft.util.math.Vec2f;

/** The 1.21 {@code Vec2} math (R row of 1.12.2's Vec2f, which has only its two fields and constants). */
public final class Vec2Bridges {
	private Vec2Bridges() {
	}

	public static Vec2f scale(Vec2f v, float factor) {
		return new Vec2f(v.x * factor, v.y * factor);
	}

	public static float dot(Vec2f v, Vec2f other) {
		return v.x * other.x + v.y * other.y;
	}

	public static Vec2f add(Vec2f v, Vec2f other) {
		return new Vec2f(v.x + other.x, v.y + other.y);
	}

	public static Vec2f add(Vec2f v, float value) {
		return new Vec2f(v.x + value, v.y + value);
	}

	/** 1.21 {@code equals(Vec2)}: the same two values. */
	public static boolean equals(Vec2f v, Vec2f other) {
		return v.x == other.x && v.y == other.y;
	}

	public static Vec2f normalized(Vec2f v) {
		float length = (float) Math.sqrt(v.x * v.x + v.y * v.y);
		return length < 1.0E-4F ? Vec2f.ZERO : new Vec2f(v.x / length, v.y / length);
	}

	public static float length(Vec2f v) {
		return (float) Math.sqrt(v.x * v.x + v.y * v.y);
	}

	public static float lengthSquared(Vec2f v) {
		return v.x * v.x + v.y * v.y;
	}

	public static float distanceToSqr(Vec2f v, Vec2f other) {
		float dx = other.x - v.x;
		float dy = other.y - v.y;
		return dx * dx + dy * dy;
	}

	public static Vec2f negated(Vec2f v) {
		return new Vec2f(-v.x, -v.y);
	}
}
