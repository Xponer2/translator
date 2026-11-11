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

import java.util.Optional;
import java.util.OptionalInt;

import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;

/**
 * The 1.21 entity data serializers 1.12.2's DataSerializers lacks, which mods' entities use (the converter reads a
 * missing {@code EntityDataSerializers.X} from here: the R line of DataSerializers). Without them the mod's data
 * accessor was null: Iron's Spells' priest (VILLAGER_DATA) was never made. Registered with 1.12.2 in the Translator's
 * pre-init, in this order, on both sides (their numbers are sent to the clients).
 */
public final class ExtraDataSerializers {
	public static final DataSerializer<OptionalInt> OPTIONAL_UNSIGNED_INT = new Serializer<OptionalInt>() {
		@Override
		public void write(PacketBuffer buffer, OptionalInt value) {
			buffer.writeVarInt(value.isPresent() ? value.getAsInt() + 1 : 0);
		}

		@Override
		public OptionalInt read(PacketBuffer buffer) {
			int value = buffer.readVarInt();
			return value == 0 ? OptionalInt.empty() : OptionalInt.of(value - 1);
		}
	};

	public static final DataSerializer<org.joml.Vector3f> VECTOR3 = new Serializer<org.joml.Vector3f>() {
		@Override
		public void write(PacketBuffer buffer, org.joml.Vector3f value) {
			buffer.writeFloat(value.x);
			buffer.writeFloat(value.y);
			buffer.writeFloat(value.z);
		}

		@Override
		public org.joml.Vector3f read(PacketBuffer buffer) {
			return new org.joml.Vector3f(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
		}

		@Override
		public org.joml.Vector3f copyValue(org.joml.Vector3f value) {
			return new org.joml.Vector3f(value);
		}
	};

	/** A carried 1.21 VillagerData, by its own stream codec. */
	public static final DataSerializer<Object> VILLAGER_DATA = new Carried("net.minecraft.world.entity.npc.VillagerData", false);

	/** An optional carried 1.21 GlobalPos (a dimension and a position), by its own stream codec. */
	public static final DataSerializer<Object> OPTIONAL_GLOBAL_POS = new Carried("net.minecraft.core.GlobalPos", true);

	/** A carried 1.21 wolf variant holder, by the variant's stream codec. */
	public static final DataSerializer<Object> WOLF_VARIANT = new Carried("net.minecraft.world.entity.animal.WolfVariant", false);

	private ExtraDataSerializers() {
	}

	/** Registers them with 1.12.2 (the pre-init, before any mod's: the same numbers on the server and the clients). */
	public static void register() {
		for (DataSerializer<?> serializer : new DataSerializer<?>[] {OPTIONAL_UNSIGNED_INT, VECTOR3, VILLAGER_DATA, OPTIONAL_GLOBAL_POS, WOLF_VARIANT}) {
			DataSerializerDefaults.register(serializer);
		}
	}

	private abstract static class Serializer<T> implements DataSerializer<T> {
		@Override
		public DataParameter<T> createKey(int id) {
			return new DataParameter<>(id, this);
		}

		@Override
		public T copyValue(T value) {
			return value;
		}
	}

	/** A value of a carried 1.21 class, written by that class's STREAM_CODEC (found when first used). */
	private static final class Carried extends Serializer<Object> {
		private final String owner;
		private final boolean optional;
		private volatile StreamCodec<Object, Object> codec;

		Carried(String owner, boolean optional) {
			this.owner = owner;
			this.optional = optional;
		}

		@SuppressWarnings("unchecked")
		private StreamCodec<Object, Object> codec() {
			StreamCodec<Object, Object> codec = this.codec;
			if (codec == null) {
				try {
					codec = (StreamCodec<Object, Object>) Class.forName(this.owner, true, net.minecraft.launchwrapper.Launch.classLoader).getField("STREAM_CODEC")
							.get(null);
				} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
					throw new IllegalStateException("Translator: the stream codec of " + this.owner, e);
				}
				this.codec = codec;
			}
			return codec;
		}

		@Override
		public void write(PacketBuffer buffer, Object value) {
			RegistryFriendlyByteBuf friendly = buffer instanceof RegistryFriendlyByteBuf ? (RegistryFriendlyByteBuf) buffer : new RegistryFriendlyByteBuf(buffer);
			if (this.optional) {
				Optional<?> present = (Optional<?>) value;
				friendly.writeBoolean(present.isPresent());
				if (present.isPresent()) {
					this.codec().encode(friendly, present.get());
				}
				return;
			}
			this.codec().encode(friendly, value);
		}

		@Override
		public Object read(PacketBuffer buffer) {
			RegistryFriendlyByteBuf friendly = buffer instanceof RegistryFriendlyByteBuf ? (RegistryFriendlyByteBuf) buffer : new RegistryFriendlyByteBuf(buffer);
			if (this.optional) {
				return friendly.readBoolean() ? Optional.ofNullable(this.codec().decode(friendly)) : Optional.empty();
			}
			return this.codec().decode(friendly);
		}
	}
}
