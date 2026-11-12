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
		if (level instanceof WorldServer server && particle instanceof VanillaParticle legacy) {
			server.spawnParticle(legacy.legacyType(), x, y, z, count, xOffset, yOffset, zOffset, speed, legacy.legacyArguments());
			return count;
		}
		if (level instanceof WorldServer server && particle != null) {
			// a mod's own particle type: sent with its codec (ModParticles)
			return Math.max(0, ModParticles.send(server, null, particle, false, x, y, z, count, xOffset, yOffset, zOffset, speed));
		}

		return 0;
	}

	/** 1.21 {@code sendParticles(player, particle, force, ...)}: to that one player; whether it was sent. */
	public static boolean sendParticles(World level, net.minecraft.entity.player.EntityPlayerMP player, ParticleOptions particle, boolean force,
			double x, double y, double z, int count, double xOffset, double yOffset, double zOffset, double speed) {
		if (!(level instanceof WorldServer server) || particle == null || player.getDistanceSq(x, y, z) >= (force ? 512 * 512 : 32 * 32)) {
			return false;
		}
		if (particle instanceof VanillaParticle legacy) {
			server.spawnParticle(player, legacy.legacyType(), force, x, y, z, count, xOffset, yOffset, zOffset, speed, legacy.legacyArguments());
			return true;
		}
		return ModParticles.send(server, player, particle, force, x, y, z, count, xOffset, yOffset, zOffset, speed) > 0;
	}

	public static void addParticle(World level, ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		if (particle instanceof VanillaParticle legacy) {
			level.spawnParticle(legacy.legacyType(), x, y, z, xSpeed, ySpeed, zSpeed, legacy.legacyArguments());
		} else if (particle != null && level.isRemote) {
			// a mod's own particle: made by its provider on the client (a server world draws none, as 1.21's)
			io.github.xponer2.translator.client.particle.ParticleProviders.addFromCommon(level, particle, x, y, z, xSpeed, ySpeed, zSpeed);
		}
	}
}
