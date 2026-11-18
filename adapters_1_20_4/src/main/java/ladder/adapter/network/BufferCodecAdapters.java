/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.network;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import ladder.carried.net.minecraft.network.codec.StreamDecoder;
import ladder.carried.net.minecraft.network.codec.StreamEncoder;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 1.20.5's {@code FriendlyByteBuf} collection, map, optional and nullable helpers take stream codecs; 1.20.4's same
 * helpers take its {@code Reader} / {@code Writer} functions and write the same bytes (receiver first).
 */
public final class BufferCodecAdapters {
    private BufferCodecAdapters() {
    }

    private static <T> FriendlyByteBuf.Reader<T> reader(StreamDecoder<? super FriendlyByteBuf, T> decoder) {
        return decoder::decode;
    }

    private static <T> FriendlyByteBuf.Writer<T> writer(StreamEncoder<? super FriendlyByteBuf, T> encoder) {
        return encoder::encode;
    }

    public static <T, C extends Collection<T>> C readCollection(FriendlyByteBuf buf, IntFunction<C> factory, StreamDecoder<? super FriendlyByteBuf, T> decoder) {
        return buf.readCollection(factory, reader(decoder));
    }

    public static <T> void writeCollection(FriendlyByteBuf buf, Collection<T> collection, StreamEncoder<? super FriendlyByteBuf, T> encoder) {
        buf.writeCollection(collection, writer(encoder));
    }

    public static <T> List<T> readList(FriendlyByteBuf buf, StreamDecoder<? super FriendlyByteBuf, T> decoder) {
        return buf.readList(reader(decoder));
    }

    public static <K, V, M extends Map<K, V>> M readMap(FriendlyByteBuf buf, IntFunction<M> factory, StreamDecoder<? super FriendlyByteBuf, K> keys,
            StreamDecoder<? super FriendlyByteBuf, V> values) {
        return buf.readMap(factory, reader(keys), reader(values));
    }

    public static <K, V> Map<K, V> readMap(FriendlyByteBuf buf, StreamDecoder<? super FriendlyByteBuf, K> keys, StreamDecoder<? super FriendlyByteBuf, V> values) {
        return buf.readMap(reader(keys), reader(values));
    }

    public static <K, V> void writeMap(FriendlyByteBuf buf, Map<K, V> map, StreamEncoder<? super FriendlyByteBuf, K> keys,
            StreamEncoder<? super FriendlyByteBuf, V> values) {
        buf.writeMap(map, writer(keys), writer(values));
    }

    public static <T> Optional<T> readOptional(FriendlyByteBuf buf, StreamDecoder<? super FriendlyByteBuf, T> decoder) {
        return buf.readOptional(reader(decoder));
    }

    public static <T> void writeOptional(FriendlyByteBuf buf, Optional<T> value, StreamEncoder<? super FriendlyByteBuf, T> encoder) {
        buf.writeOptional(value, writer(encoder));
    }

    @Nullable
    public static <T> T readNullable(FriendlyByteBuf buf, StreamDecoder<? super FriendlyByteBuf, T> decoder) {
        return buf.readNullable(reader(decoder));
    }

    public static <T> void writeNullable(FriendlyByteBuf buf, @Nullable T value, StreamEncoder<? super FriendlyByteBuf, T> encoder) {
        buf.writeNullable(value, writer(encoder));
    }

    /** 1.21's static {@code readNullable(ByteBuf, decoder)}: a presence flag, then the value, on any buffer. */
    @Nullable
    public static <B extends ByteBuf, T> T readNullable(B buf, StreamDecoder<? super B, T> decoder) {
        return buf.readBoolean() ? decoder.decode(buf) : null;
    }

    public static <B extends ByteBuf, T> void writeNullable(B buf, @Nullable T value, StreamEncoder<? super B, T> encoder) {
        if (value != null) {
            buf.writeBoolean(true);
            encoder.encode(buf, value);
        } else {
            buf.writeBoolean(false);
        }
    }
}
