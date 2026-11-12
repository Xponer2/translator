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

import java.util.Random;

import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamDecoder;
import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.Registries;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/** The client side of {@link ModParticles} (only loaded on the client). */
final class ClientParticles {
	private static final Random RANDOM = new Random();
	private static boolean reported;

	private ClientParticles() {
	}

	static void schedule(byte[] payload) {
		Minecraft.getMinecraft().addScheduledTask(() -> {
			try {
				receive(payload);
			} catch (RuntimeException | LinkageError e) {
				if (!reported) {
					reported = true;
					io.github.xponer2.translator.runtime.RuntimeStubs.failed("a sent mod particle", e);
				}
			}
		});
	}

	/** On the client: the particle back from the packet, spawned as 1.21's client spawns sent particles. */
	@SuppressWarnings({"rawtypes"})
	private static void receive(byte[] payload) {
		var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload));
		ResourceLocation id = buffer.readResourceLocation();
		boolean force = buffer.readBoolean();
		double x = buffer.readDouble();
		double y = buffer.readDouble();
		double z = buffer.readDouble();
		double xOffset = buffer.readFloat();
		double yOffset = buffer.readFloat();
		double zOffset = buffer.readFloat();
		double speed = buffer.readFloat();
		int count = buffer.readVarInt();
		Registry registry = BuiltInRegistries.byKey(Registries.PARTICLE_TYPE);
		Object type = registry.get(id);
		if (type == null) {
			return;
		}
		Object particle = type;
		if (!(type instanceof ParticleOptions)) {
			Object codec = ModParticles.call(type, "streamCodec");
			if (!(codec instanceof StreamDecoder decoder)) {
				return;
			}
			particle = decoder.decode(buffer);
		}
		if (!(particle instanceof ParticleOptions options)) {
			return;
		}
		var level = Minecraft.getMinecraft().world;
		if (level == null) {
			return;
		}
		if (count == 0) {
			Particles.addParticle(level, options, x, y, z, speed * xOffset, speed * yOffset, speed * zOffset);
			return;
		}
		for (int i = 0; i < count; i++) {
			Particles.addParticle(level, options, x + RANDOM.nextGaussian() * xOffset, y + RANDOM.nextGaussian() * yOffset,
					z + RANDOM.nextGaussian() * zOffset, RANDOM.nextGaussian() * speed, RANDOM.nextGaussian() * speed, RANDOM.nextGaussian() * speed);
		}
	}
}
