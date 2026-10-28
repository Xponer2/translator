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
