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

import javax.annotation.Nullable;

import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamEncoder;
import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.Registries;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 1.21 {@code ServerLevel.sendParticles} of the mods' own particle types (Iron's Spells' spell effects, Twilight
 * Forest's, Ars Nouveau's): 1.12.2 sends only its own particles, so these were dropped. The server writes the
 * particle with its type's stream codec (the mod's), the client reads it back and spawns it as 1.21's client does,
 * through {@link Particles#addParticle}. Call {@link #init()} in pre-init.
 */
public final class ModParticles {
	private static SimpleNetworkWrapper channel;
	private static boolean reported;

	private ModParticles() {
	}

	public static void init() {
		channel = NetworkRegistry.INSTANCE.newSimpleChannel("translator_particle");
		channel.registerMessage(Message.Handler.class, Message.class, 0, Side.CLIENT);
	}

	/**
	 * Sends a mod's particles to the players near (32 blocks, 512 when forced) or to one player; the number of players
	 * it went to, -1 when the particle can't be sent (not a registered type, or its codec fails).
	 */
	public static int send(WorldServer level, @Nullable EntityPlayerMP only, ParticleOptions particle, boolean force, double x, double y, double z,
			int count, double xOffset, double yOffset, double zOffset, double speed) {
		if (channel == null) {
			return -1;
		}
		byte[] payload = encode(particle, force, x, y, z, count, xOffset, yOffset, zOffset, speed);
		if (payload == null) {
			return -1;
		}
		int sent = 0;
		double range = force ? 512 : 32;
		for (EntityPlayerMP player : level.getPlayers(EntityPlayerMP.class, p -> true)) {
			if ((only == null || player == only) && player.getDistanceSq(x, y, z) < range * range) {
				channel.sendTo(new Message(payload), player);
				sent++;
			}
		}
		return sent;
	}

	@Nullable
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static byte[] encode(ParticleOptions particle, boolean force, double x, double y, double z, int count, double xOffset, double yOffset,
			double zOffset, double speed) {
		try {
			Object type = call(particle, "getType");
			Registry registry = BuiltInRegistries.byKey(Registries.PARTICLE_TYPE);
			ResourceLocation id = type == null ? null : registry.getKey(type);
			if (id == null) {
				return null;
			}
			var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer());
			buffer.writeResourceLocation(id);
			buffer.writeBoolean(force);
			buffer.writeDouble(x);
			buffer.writeDouble(y);
			buffer.writeDouble(z);
			buffer.writeFloat((float) xOffset);
			buffer.writeFloat((float) yOffset);
			buffer.writeFloat((float) zOffset);
			buffer.writeFloat((float) speed);
			buffer.writeVarInt(count);
			// a simple type is its own options: nothing more to write
			Object codec = type == particle ? null : call(type, "streamCodec");
			if (codec instanceof StreamEncoder encoder) {
				encoder.encode(buffer, particle);
			} else if (type != particle) {
				return null;
			}
			byte[] bytes = new byte[buffer.readableBytes()];
			buffer.readBytes(bytes);
			return bytes;
		} catch (RuntimeException | LinkageError e) {
			if (!reported) {
				reported = true;
				io.github.xponer2.translator.runtime.RuntimeStubs.failed("sending the particle " + particle.getClass().getName(), e);
			}
			return null;
		}
	}

	@Nullable
	static Object call(Object target, String name) {
		try {
			var method = target.getClass().getMethod(name);
			method.setAccessible(true);
			return method.invoke(target);
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}

	public static final class Message implements IMessage {
		private byte[] payload;

		public Message() {
		}

		Message(byte[] payload) {
			this.payload = payload;
		}

		@Override
		public void fromBytes(ByteBuf buffer) {
			this.payload = new byte[buffer.readableBytes()];
			buffer.readBytes(this.payload);
		}

		@Override
		public void toBytes(ByteBuf buffer) {
			buffer.writeBytes(this.payload);
		}

		public static final class Handler implements IMessageHandler<Message, IMessage> {
			@Override
			public IMessage onMessage(Message message, MessageContext context) {
				// the client classes are only loaded here, on the client
				ClientParticles.schedule(message.payload);
				return null;
			}
		}
	}
}
