package io.github.xponer2.translator.util.particle;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * 1.21 particle calls: ServerLevel#sendParticles and Level#addParticle (the converter writes level.x(..) as
 * Particles.x(level, ..)).
 */
public final class Particles {
	private Particles() {
	}

	/** Sends particles to the players near the position; returns how many were sent (1.21 returns the count). */
	public static int sendParticles(World level, ParticleOptions particle, double x, double y, double z, int count,
			double xOffset, double yOffset, double zOffset, double speed) {
		if (level instanceof WorldServer server) {
			server.spawnParticle(particle.getType(), x, y, z, count, xOffset, yOffset, zOffset, speed, particle.getArguments());
			return count;
		}

		return 0;
	}

	public static void addParticle(World level, ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		level.spawnParticle(particle.getType(), x, y, z, xSpeed, ySpeed, zSpeed, particle.getArguments());
	}
}
