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
import java.util.UUID;

import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.math.BlockPos;

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

	/**
	 * 1.21's BLOCK_STATE, a block state: 1.12.2 has only an optional one, of a Guava Optional (the runtime table names
	 * these for the mods' EntityDataSerializers fields: 1.12.2's own are another type).
	 */
	public static final DataSerializer<IBlockState> BLOCK_STATE = new Serializer<IBlockState>() {
		@Override
		public void write(PacketBuffer buffer, IBlockState value) {
			buffer.writeVarInt(Block.getStateId(value));
		}

		@Override
		public IBlockState read(PacketBuffer buffer) {
			return Block.getStateById(buffer.readVarInt());
		}
	};

	/** 1.21's OPTIONAL_BLOCK_STATE: a java.util.Optional (empty is air, as on 1.21). */
	public static final DataSerializer<Optional<IBlockState>> OPTIONAL_STATE = new Serializer<Optional<IBlockState>>() {
		@Override
		public void write(PacketBuffer buffer, Optional<IBlockState> value) {
			buffer.writeVarInt(value.isPresent() ? Block.getStateId(value.get()) : 0);
		}

		@Override
		public Optional<IBlockState> read(PacketBuffer buffer) {
			int id = buffer.readVarInt();
			return id == 0 ? Optional.empty() : Optional.of(Block.getStateById(id));
		}
	};

	/** 1.21's OPTIONAL_BLOCK_POS: a java.util.Optional. */
	public static final DataSerializer<Optional<BlockPos>> OPTIONAL_POS = new Serializer<Optional<BlockPos>>() {
		@Override
		public void write(PacketBuffer buffer, Optional<BlockPos> value) {
			buffer.writeBoolean(value.isPresent());
			if (value.isPresent()) {
				buffer.writeBlockPos(value.get());
			}
		}

		@Override
		public Optional<BlockPos> read(PacketBuffer buffer) {
			return buffer.readBoolean() ? Optional.of(buffer.readBlockPos()) : Optional.empty();
		}
	};

	/** 1.21's OPTIONAL_UUID: a java.util.Optional (the TF lich's master; 1.12.2's OPTIONAL_UNIQUE_ID is a Guava one). */
	public static final DataSerializer<Optional<UUID>> OPTIONAL_UUID = new Serializer<Optional<UUID>>() {
		@Override
		public void write(PacketBuffer buffer, Optional<UUID> value) {
			buffer.writeBoolean(value.isPresent());
			if (value.isPresent()) {
				buffer.writeUniqueId(value.get());
			}
		}

		@Override
		public Optional<UUID> read(PacketBuffer buffer) {
			return buffer.readBoolean() ? Optional.of(buffer.readUniqueId()) : Optional.empty();
		}
	};

	/** 1.21 {@code LONG}. */
	public static final DataSerializer<Long> LONG = new Serializer<Long>() {
		@Override
		public void write(PacketBuffer buffer, Long value) {
			buffer.writeVarLong(value);
		}

		@Override
		public Long read(PacketBuffer buffer) {
			return buffer.readVarLong();
		}
	};

	/** 1.21 {@code OPTIONAL_COMPONENT} (an entity's custom name in 1.21's own Entity). */
	public static final DataSerializer<Optional<net.minecraft.util.text.ITextComponent>> OPTIONAL_COMPONENT = new Serializer<Optional<net.minecraft.util.text.ITextComponent>>() {
		@Override
		public void write(PacketBuffer buffer, Optional<net.minecraft.util.text.ITextComponent> value) {
			buffer.writeBoolean(value.isPresent());
			if (value.isPresent()) {
				buffer.writeTextComponent(value.get());
			}
		}

		@Override
		public Optional<net.minecraft.util.text.ITextComponent> read(PacketBuffer buffer) {
			try {
				return buffer.readBoolean() ? Optional.of(buffer.readTextComponent()) : Optional.empty();
			} catch (java.io.IOException e) {
				return Optional.empty();
			}
		}
	};

	/** 1.21 {@code PARTICLE}: a particle with its options, as 1.21 sends it. */
	public static final DataSerializer<Object> PARTICLE = new Serializer<Object>() {
		@Override
		public void write(PacketBuffer buffer, Object value) {
			io.github.xponer2.translator.util.particle.ParticleTypes.STREAM_CODEC.encode(friendly(buffer),
					(io.github.xponer2.translator.util.particle.ParticleOptions) value);
		}

		@Override
		public Object read(PacketBuffer buffer) {
			return io.github.xponer2.translator.util.particle.ParticleTypes.STREAM_CODEC.decode(friendly(buffer));
		}
	};

	/**
	 * 1.21 {@code PARTICLES}: a list of them (a living entity's effect particles). Without it 1.21's own LivingEntity
	 * didn't initialize (every mod code that named one of its constants failed: Supplementaries' flute).
	 */
	public static final DataSerializer<java.util.List<Object>> PARTICLES = new Serializer<java.util.List<Object>>() {
		@Override
		public void write(PacketBuffer buffer, java.util.List<Object> value) {
			buffer.writeVarInt(value.size());
			for (Object particle : value) {
				PARTICLE.write(buffer, particle);
			}
		}

		@Override
		public java.util.List<Object> read(PacketBuffer buffer) throws java.io.IOException {
			int size = buffer.readVarInt();
			java.util.List<Object> particles = new java.util.ArrayList<>(Math.min(size, 64));
			for (int i = 0; i < size; i++) {
				particles.add(PARTICLE.read(buffer));
			}
			return particles;
		}
	};

	/** 1.21 {@code QUATERNION}. */
	public static final DataSerializer<org.joml.Quaternionf> QUATERNION = new Serializer<org.joml.Quaternionf>() {
		@Override
		public void write(PacketBuffer buffer, org.joml.Quaternionf value) {
			buffer.writeFloat(value.x);
			buffer.writeFloat(value.y);
			buffer.writeFloat(value.z);
			buffer.writeFloat(value.w);
		}

		@Override
		public org.joml.Quaternionf read(PacketBuffer buffer) {
			return new org.joml.Quaternionf(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
		}

		@Override
		public org.joml.Quaternionf copyValue(org.joml.Quaternionf value) {
			return new org.joml.Quaternionf(value);
		}
	};

	// 1.21's kinds of carried values, each by its class's STREAM_CODEC
	public static final DataSerializer<Object> POSE = new Carried("net.minecraft.world.entity.Pose", false);
	public static final DataSerializer<Object> CAT_VARIANT = new Carried("net.minecraft.world.entity.animal.CatVariant", false);
	public static final DataSerializer<Object> FROG_VARIANT = new Carried("net.minecraft.world.entity.animal.FrogVariant", false);
	public static final DataSerializer<Object> PAINTING_VARIANT = new Carried("net.minecraft.world.entity.decoration.PaintingVariant", false);
	public static final DataSerializer<Object> ARMADILLO_STATE = new Carried("net.minecraft.world.entity.animal.armadillo.Armadillo$ArmadilloState", false);
	public static final DataSerializer<Object> SNIFFER_STATE = new Carried("net.minecraft.world.entity.animal.sniffer.Sniffer$State", false);

	private static RegistryFriendlyByteBuf friendly(PacketBuffer buffer) {
		return buffer instanceof RegistryFriendlyByteBuf ? (RegistryFriendlyByteBuf) buffer : new RegistryFriendlyByteBuf(buffer);
	}

	private ExtraDataSerializers() {
	}

	/** Registers them with 1.12.2 (the pre-init, before any mod's: the same numbers on the server and the clients). */
	public static void register() {
		for (DataSerializer<?> serializer : new DataSerializer<?>[] {OPTIONAL_UNSIGNED_INT, VECTOR3, VILLAGER_DATA, OPTIONAL_GLOBAL_POS, WOLF_VARIANT, BLOCK_STATE,
				OPTIONAL_STATE, OPTIONAL_POS, OPTIONAL_UUID, LONG, OPTIONAL_COMPONENT, PARTICLE, PARTICLES, QUATERNION, POSE, CAT_VARIANT, FROG_VARIANT,
				PAINTING_VARIANT, ARMADILLO_STATE, SNIFFER_STATE}) {
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

		private boolean reported;

		/**
		 * The value is written first into a buffer of its own, after a marker: a codec that throws (a registry entry with
		 * no id yet: Twilight Forest's wolf variants) writes the marker alone and the reader gets none. It took the
		 * server down in the spawn packet of the entity.
		 */
		@Override
		public void write(PacketBuffer buffer, Object value) {
			RegistryFriendlyByteBuf friendly = buffer instanceof RegistryFriendlyByteBuf ? (RegistryFriendlyByteBuf) buffer : new RegistryFriendlyByteBuf(buffer);
			Object present = value;
			if (this.optional) {
				present = ((Optional<?>) value).orElse(null);
			}
			if (present == null) {
				friendly.writeBoolean(false);
				return;
			}
			var scratch = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
			try {
				this.codec().encode(scratch, present);
			} catch (RuntimeException | LinkageError e) {
				if (!this.reported) {
					this.reported = true;
					io.github.xponer2.translator.runtime.RuntimeStubs.failed("writing entity data of " + this.owner, e);
				}
				friendly.writeBoolean(false);
				return;
			}
			friendly.writeBoolean(true);
			friendly.writeBytes(scratch);
		}

		@Override
		public Object read(PacketBuffer buffer) {
			RegistryFriendlyByteBuf friendly = buffer instanceof RegistryFriendlyByteBuf ? (RegistryFriendlyByteBuf) buffer : new RegistryFriendlyByteBuf(buffer);
			Object value = friendly.readBoolean() ? this.codec().decode(friendly) : null;
			return this.optional ? Optional.ofNullable(value) : value;
		}
	}
}
