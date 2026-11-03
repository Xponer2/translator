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
package io.github.xponer2.translator.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * The buffer of payloads and stream codecs (1.21 {@code net.minecraft.network.RegistryFriendlyByteBuf} and its super
 * class {@code FriendlyByteBuf}): a 1.12.2 {@link PacketBuffer} with the 1.21 methods it doesn't have. Registry
 * objects are written by id, so it needs no registry access.
 */
public class RegistryFriendlyByteBuf extends PacketBuffer {
    public static final int MAX_STRING_LENGTH = 32767;

    public RegistryFriendlyByteBuf(ByteBuf buffer) {
        super(buffer);
    }

    /**
     * The buffer itself if it is one already, else a view of it (same content and indices).
     */
    /** 1.21 {@code new RegistryFriendlyByteBuf(buffer, registryAccess)}: the registries are the game's anyway. */
    public RegistryFriendlyByteBuf(ByteBuf buffer, io.github.xponer2.translator.registry.RegistryAccess registries) {
        super(buffer);
    }

    public io.github.xponer2.translator.registry.RegistryAccess registryAccess() {
        return io.github.xponer2.translator.registry.RegistryAccess.INSTANCE;
    }

    public static RegistryFriendlyByteBuf wrap(ByteBuf buffer) {
        return buffer instanceof RegistryFriendlyByteBuf ? (RegistryFriendlyByteBuf) buffer : new RegistryFriendlyByteBuf(buffer);
    }

    public String readUtf() {
        return this.readString(MAX_STRING_LENGTH);
    }

    public String readUtf(int maxLength) {
        return this.readString(maxLength);
    }

    /**
     * {@code readUtf()}, under its 1.12.2 name.
     */
    public String readString() {
        return this.readString(MAX_STRING_LENGTH);
    }

    public RegistryFriendlyByteBuf writeUtf(String value) {
        this.writeString(value);
        return this;
    }

    // the writes return the buffer itself as in 1.21 (mods chain them: buf.writeInt(x).writeBoolean(y))

    @Override
    public RegistryFriendlyByteBuf writeBoolean(boolean value) {
        super.writeBoolean(value);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writeByte(int value) {
        super.writeByte(value);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writeShort(int value) {
        super.writeShort(value);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writeInt(int value) {
        super.writeInt(value);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writeLong(long value) {
        super.writeLong(value);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writeFloat(float value) {
        super.writeFloat(value);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writeDouble(double value) {
        super.writeDouble(value);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writeVarInt(int value) {
        super.writeVarInt(value);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writeVarLong(long value) {
        super.writeVarLong(value);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writeResourceLocation(net.minecraft.util.ResourceLocation location) {
        super.writeResourceLocation(location);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writeBlockPos(net.minecraft.util.math.BlockPos pos) {
        super.writeBlockPos(pos);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writeUniqueId(java.util.UUID id) {
        super.writeUniqueId(id);
        return this;
    }

    public RegistryFriendlyByteBuf writeUUID(java.util.UUID id) {
        return this.writeUniqueId(id);
    }

    public java.util.UUID readUUID() {
        return this.readUniqueId();
    }

    @Override
    public RegistryFriendlyByteBuf writeByteArray(byte[] bytes) {
        super.writeByteArray(bytes);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writeLongArray(long[] values) {
        super.writeLongArray(values);
        return this;
    }

    public long[] readLongArray() {
        return this.readLongArray(null);
    }

    @Override
    public RegistryFriendlyByteBuf writeEnumValue(Enum<?> value) {
        super.writeEnumValue(value);
        return this;
    }

    public RegistryFriendlyByteBuf writeEnum(Enum<?> value) {
        return this.writeEnumValue(value);
    }

    public <T extends Enum<T>> T readEnum(Class<T> type) {
        return this.readEnumValue(type);
    }

    public RegistryFriendlyByteBuf writeByte(byte value) {
        super.writeByte(value);
        return this;
    }

    public RegistryFriendlyByteBuf writeUtf(String value, int maxLength) {
        this.writeString(value);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf readBytes(byte[] bytes) {
        super.readBytes(bytes);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writeBytes(byte[] bytes) {
        super.writeBytes(bytes);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf writerIndex(int index) {
        super.writerIndex(index);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf readerIndex(int index) {
        super.readerIndex(index);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf capacity(int capacity) {
        super.capacity(capacity);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf ensureWritable(int bytes) {
        super.ensureWritable(bytes);
        return this;
    }

    @Override
    public RegistryFriendlyByteBuf getBytes(int index, ByteBuf target, int length) {
        super.getBytes(index, target, length);
        return this;
    }

    // NBT: any tag, a compound on the wire (a tag that isn't a compound goes wrapped in one, marked)

    private static final String WRAPPED = "translator$tag";

    public RegistryFriendlyByteBuf writeNbt(@javax.annotation.Nullable net.minecraft.nbt.NBTBase tag) {
        if (tag == null || tag instanceof net.minecraft.nbt.NBTTagCompound) {
            this.writeCompoundTag((net.minecraft.nbt.NBTTagCompound) tag);
        } else {
            var wrapper = new net.minecraft.nbt.NBTTagCompound();
            wrapper.setTag(WRAPPED, tag);
            this.writeCompoundTag(wrapper);
        }
        return this;
    }

    public RegistryFriendlyByteBuf writeCompoundTag(@javax.annotation.Nullable net.minecraft.nbt.NBTBase tag) {
        return this.writeNbt(tag);
    }

    @javax.annotation.Nullable
    public net.minecraft.nbt.NBTTagCompound readNbt() {
        try {
            return this.readCompoundTag();
        } catch (java.io.IOException e) {
            throw new io.netty.handler.codec.DecoderException(e);
        }
    }

    /** Any tag (a tag that isn't a compound comes wrapped, see writeNbt). */
    @javax.annotation.Nullable
    public net.minecraft.nbt.NBTBase readNbt(net.minecraft.nbt.NBTSizeTracker accounter) {
        net.minecraft.nbt.NBTTagCompound tag = this.readNbt();
        return tag != null && tag.getSize() == 1 && tag.hasKey(WRAPPED) ? tag.getTag(WRAPPED) : tag;
    }

    public <T> RegistryFriendlyByteBuf writeWithCodec(com.mojang.serialization.DynamicOps<net.minecraft.nbt.NBTBase> ops,
            com.mojang.serialization.Codec<T> codec, T value) {
        return this.writeNbt(codec.encodeStart(ops, value).result()
                .orElseThrow(() -> new io.netty.handler.codec.EncoderException("Failed to encode " + value)));
    }

    public <T> T readWithCodecTrusted(com.mojang.serialization.DynamicOps<net.minecraft.nbt.NBTBase> ops, com.mojang.serialization.Codec<T> codec) {
        var tag = this.readNbt(null);
        return codec.parse(ops, tag).result().orElseThrow(() -> new io.netty.handler.codec.DecoderException("Failed to decode " + tag));
    }

    public <T> T readWithCodec(com.mojang.serialization.DynamicOps<net.minecraft.nbt.NBTBase> ops, com.mojang.serialization.Codec<T> codec,
            net.minecraft.nbt.NBTSizeTracker accounter) {
        return this.readWithCodecTrusted(ops, codec);
    }

    public <T> io.github.xponer2.translator.registry.ResourceKey<T> readResourceKey(
            io.github.xponer2.translator.registry.ResourceKey<? extends io.github.xponer2.translator.registry.Registry<T>> registry) {
        return io.github.xponer2.translator.registry.ResourceKey.create(registry, this.readResourceLocation());
    }

    public void writeResourceKey(io.github.xponer2.translator.registry.ResourceKey<?> key) {
        this.writeResourceLocation(key.location());
    }

    /** The 1.21 block hit: position, face, the hit point relative to the position, inside, world border hit. */
    public net.minecraft.util.math.RayTraceResult readBlockHitResult() {
        var pos = this.readBlockPos();
        var face = this.readEnumValue(net.minecraft.util.EnumFacing.class);
        var hit = new net.minecraft.util.math.Vec3d(pos.getX() + this.readFloat(), pos.getY() + this.readFloat(), pos.getZ() + this.readFloat());
        this.readBoolean();
        this.readBoolean();
        return new net.minecraft.util.math.RayTraceResult(hit, face, pos);
    }

    public void writeBlockHitResult(net.minecraft.util.math.RayTraceResult hit) {
        var pos = hit.getBlockPos();
        this.writeBlockPos(pos);
        this.writeEnumValue(hit.sideHit);
        this.writeFloat((float) (hit.hitVec.x - pos.getX()));
        this.writeFloat((float) (hit.hitVec.y - pos.getY()));
        this.writeFloat((float) (hit.hitVec.z - pos.getZ()));
        this.writeBoolean(false);
        this.writeBoolean(false);
    }

    public <T> T readById(IntFunction<T> byId) {
        return byId.apply(this.readVarInt());
    }

    public <T> RegistryFriendlyByteBuf writeById(java.util.function.ToIntFunction<T> toId, T value) {
        return this.writeVarInt(toId.applyAsInt(value));
    }

    @javax.annotation.Nullable
    public <T> T readNullable(StreamDecoder<? super RegistryFriendlyByteBuf, T> reader) {
        return this.readBoolean() ? reader.decode(this) : null;
    }

    public <T> void writeNullable(@javax.annotation.Nullable T value, StreamEncoder<? super RegistryFriendlyByteBuf, T> writer) {
        this.writeBoolean(value != null);
        if (value != null) {
            writer.encode(this, value);
        }
    }

    public <T, C extends Collection<T>> C readCollection(IntFunction<C> factory, StreamDecoder<? super RegistryFriendlyByteBuf, T> reader) {
        int size = this.readVarInt();
        C collection = factory.apply(size);
        for (int i = 0; i < size; i++) {
            collection.add(reader.decode(this));
        }
        return collection;
    }

    public <T> void writeCollection(Collection<T> collection, StreamEncoder<? super RegistryFriendlyByteBuf, T> writer) {
        this.writeVarInt(collection.size());
        for (T value : collection) {
            writer.encode(this, value);
        }
    }

    public <T> List<T> readList(StreamDecoder<? super RegistryFriendlyByteBuf, T> reader) {
        return this.readCollection(ArrayList::new, reader);
    }

    public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> factory, StreamDecoder<? super RegistryFriendlyByteBuf, K> keyReader,
                                                StreamDecoder<? super RegistryFriendlyByteBuf, V> valueReader) {
        int size = this.readVarInt();
        M map = factory.apply(size);
        for (int i = 0; i < size; i++) {
            K key = keyReader.decode(this);
            map.put(key, valueReader.decode(this));
        }
        return map;
    }

    public <K, V> Map<K, V> readMap(StreamDecoder<? super RegistryFriendlyByteBuf, K> keyReader,
                                    StreamDecoder<? super RegistryFriendlyByteBuf, V> valueReader) {
        return this.readMap(HashMap::new, keyReader, valueReader);
    }

    public <K, V> void writeMap(Map<K, V> map, StreamEncoder<? super RegistryFriendlyByteBuf, K> keyWriter,
                                StreamEncoder<? super RegistryFriendlyByteBuf, V> valueWriter) {
        this.writeVarInt(map.size());
        for (Map.Entry<K, V> entry : map.entrySet()) {
            keyWriter.encode(this, entry.getKey());
            valueWriter.encode(this, entry.getValue());
        }
    }

    public <T> Optional<T> readOptional(StreamDecoder<? super RegistryFriendlyByteBuf, T> reader) {
        return this.readBoolean() ? Optional.of(reader.decode(this)) : Optional.empty();
    }

    public <T> void writeOptional(Optional<T> value, StreamEncoder<? super RegistryFriendlyByteBuf, T> writer) {
        this.writeBoolean(value.isPresent());
        value.ifPresent(v -> writer.encode(this, v));
    }
}
