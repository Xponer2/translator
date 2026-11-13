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

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamDecoder;
import io.github.xponer2.translator.network.StreamEncoder;
import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.Registries;

import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21 {@code ParticleTypes.STREAM_CODEC} (any particle, with its type) and {@code ParticleOptions.getType()} (R line:
 * a mod's options have their own getType, the interface here has none). Twilight Forest sends its particles in a packet
 * of its own written with them: the packet failed, so none of its effects showed. A mod's type is written by its id and
 * its stream codec, as 1.21 writes it; a 1.12.2 particle by the Translator's id {@code translator:legacy} with its
 * 1.12.2 id and arguments.
 */
public final class ParticleCodec {
	private static final ResourceLocation LEGACY = new ResourceLocation("translator", "legacy");

	private ParticleCodec() {
	}

	/** 1.21 {@code ParticleOptions.getType()}: the options' own getType when it gives a 1.21 type, null otherwise. */
	@Nullable
	public static io.github.xponer2.translator.runtime.mc.ParticleType<?> getType(ParticleOptions options) {
		if (options instanceof io.github.xponer2.translator.runtime.mc.ParticleType<?> type) {
			// a simple type is its own options
			return type;
		}
		for (Class<?> type = options.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
			for (Method method : type.getDeclaredMethods()) {
				if ("getType".equals(method.getName()) && method.getParameterCount() == 0
						&& io.github.xponer2.translator.runtime.mc.ParticleType.class.isAssignableFrom(method.getReturnType())) {
					try {
						method.setAccessible(true);
						return (io.github.xponer2.translator.runtime.mc.ParticleType<?>) method.invoke(options);
					} catch (ReflectiveOperationException | RuntimeException e) {
						return null;
					}
				}
			}
		}
		return null;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	static void encode(RegistryFriendlyByteBuf buffer, ParticleOptions options) {
		if (options instanceof LegacyParticleOptions legacy) {
			buffer.writeResourceLocation(LEGACY);
			buffer.writeVarInt(legacy.legacyType().getParticleID());
			int[] arguments = legacy.legacyArguments();
			buffer.writeVarInt(arguments.length);
			for (int argument : arguments) {
				buffer.writeVarInt(argument);
			}
			return;
		}
		Object type = getType(options);
		Registry registry = BuiltInRegistries.byKey(Registries.PARTICLE_TYPE);
		ResourceLocation id = type == null ? null : registry.getKey(type);
		if (id == null) {
			throw new IllegalArgumentException("A particle of an unregistered type: " + options);
		}
		buffer.writeResourceLocation(id);
		if (type != options) {
			Object codec = ModParticles.call(type, "streamCodec");
			if (!(codec instanceof StreamEncoder encoder)) {
				throw new IllegalArgumentException("No stream codec for the particle type " + id);
			}
			encoder.encode(buffer, options);
		}
	}

	@SuppressWarnings("rawtypes")
	static ParticleOptions decode(RegistryFriendlyByteBuf buffer) {
		ResourceLocation id = buffer.readResourceLocation();
		if (LEGACY.equals(id)) {
			EnumParticleTypes type = EnumParticleTypes.getParticleFromId(buffer.readVarInt());
			int[] arguments = new int[buffer.readVarInt()];
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = buffer.readVarInt();
			}
			return new LegacyParticleOptions(type == null ? EnumParticleTypes.SMOKE_NORMAL : type, arguments);
		}
		Object type = BuiltInRegistries.byKey(Registries.PARTICLE_TYPE).get(id);
		if (type instanceof ParticleOptions simple) {
			return simple;
		}
		Object codec = type == null ? null : ModParticles.call(type, "streamCodec");
		if (!(codec instanceof StreamDecoder decoder)) {
			throw new IllegalArgumentException("Unknown particle type " + id);
		}
		return (ParticleOptions) decoder.decode(buffer);
	}
}
