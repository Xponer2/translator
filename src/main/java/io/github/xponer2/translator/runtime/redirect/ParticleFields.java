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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.particle.Particle;

/**
 * The fields 1.21 {@code Particle} has and 1.12.2's lacks (runtime, a missing field read or write of a particle comes
 * here): {@code friction} (how much of its speed a particle keeps each tick, 0.98 as 1.12.2 always does) and
 * {@code speedUpWhenYMotionIsBlocked}. A mod's particle sets them when it's made and its own tick reads them (Biomes
 * O' Plenty's leaves); without them that tick was left empty, so the particle never moved nor aged.
 */
public final class ParticleFields {
	private static final Map<Particle, Float> FRICTION = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Map<Particle, Boolean> SPEED_UP = Collections.synchronizedMap(new WeakHashMap<>());

	private ParticleFields() {
	}

	public static float friction(Particle particle) {
		Float friction = FRICTION.get(particle);
		return friction == null ? 0.98F : friction;
	}

	public static void friction(Particle particle, float friction) {
		FRICTION.put(particle, friction);
		any = true;
	}

	/** Whether any particle set its own friction (until then 1.12.2's particles skip the lookup). */
	private static volatile boolean any;

	/**
	 * The share of its motion a particle keeps each tick: its 1.21 friction, 1.12.2's 0.98 when it set none
	 * ({@code core.ParticleFriction} has 1.12.2's {@code Particle.onUpdate} ask here).
	 */
	public static double motionFactor(Object particle) {
		if (!any) {
			return 0.9800000190734863D;
		}
		Float friction = FRICTION.get(particle);
		return friction == null ? 0.9800000190734863D : friction;
	}

	public static boolean speedUpWhenYMotionIsBlocked(Particle particle) {
		return Boolean.TRUE.equals(SPEED_UP.get(particle));
	}

	public static void speedUpWhenYMotionIsBlocked(Particle particle, boolean speedUp) {
		SPEED_UP.put(particle, speedUp);
	}
}
