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
package io.github.xponer2.translator.entity;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;

/**
 * The default methods the AccessWidener gives 1.12.2's {@code DataSerializer}: a 1.21 mod's entity data serializer
 * ({@code EntityDataSerializer.forValueType(codec)}, a class of its own) has only 1.21's {@code codec()}; 1.12.2 calls
 * write, read, createKey and copyValue, which it didn't have (AbstractMethodError in the entity's static init:
 * Supplementaries' hat stand, piñata and cannon boat were never made). Here they go through the codec.
 */
public final class DataSerializerDefaults {
	/** A serializer class's 1.21 {@code codec()}, null when it has none. */
	private static final ClassValue<Method> CODEC = new ClassValue<Method>() {
		@Override
		protected Method computeValue(Class<?> type) {
			for (Class<?> current = type; current != null; current = current.getSuperclass()) {
				for (Method method : current.getDeclaredMethods()) {
					if ("codec".equals(method.getName()) && method.getParameterCount() == 0 && StreamCodec.class.isAssignableFrom(method.getReturnType())) {
						method.setAccessible(true);
						return method;
					}
				}
			}
			return null;
		}
	};

	private DataSerializerDefaults() {
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void write(DataSerializer<?> self, PacketBuffer buffer, Object value) {
		((StreamCodec) codec(self)).encode(friendly(buffer), value);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Object read(DataSerializer<?> self, PacketBuffer buffer) {
		return ((StreamCodec) codec(self)).decode(friendly(buffer));
	}

	/** 1.21's {@code createAccessor(id)}: the serializer is registered first when no registry did it (1.12.2 refuses data of one it doesn't know). */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static DataParameter<?> createKey(DataSerializer<?> self, int id) {
		register(self);
		return new DataParameter(id, self);
	}

	/** 1.21's default {@code copy(value)}: the value itself. */
	public static Object copyValue(DataSerializer<?> self, Object value) {
		return value;
	}

	/** Registers a mod's serializer with 1.12.2 (its number, sent to the clients, follows the registration order). */
	public static synchronized void register(@Nullable Object serializer) {
		if (serializer instanceof DataSerializer<?> && DataSerializers.getSerializerId((DataSerializer<?>) serializer) < 0) {
			DataSerializers.registerSerializer((DataSerializer<?>) serializer);
		}
	}

	private static Object codec(DataSerializer<?> self) {
		Method method = CODEC.get(self.getClass());
		if (method == null) {
			throw new IllegalStateException("Translator: the entity data serializer " + self.getClass().getName() + " has neither 1.12.2's methods nor 1.21's codec()");
		}
		try {
			return method.invoke(self);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Translator: the codec of the entity data serializer " + self.getClass().getName(), e);
		}
	}

	private static RegistryFriendlyByteBuf friendly(PacketBuffer buffer) {
		return buffer instanceof RegistryFriendlyByteBuf ? (RegistryFriendlyByteBuf) buffer : new RegistryFriendlyByteBuf(buffer);
	}
}
