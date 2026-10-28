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

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import io.netty.buffer.ByteBuf;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Reads and writes a value of type {@code V} on a buffer of type {@code B} (1.21
 * {@code net.minecraft.network.codec.StreamCodec}). Used by the payloads of {@link PacketDistributor} and by the
 * recipe serializers.
 */
public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
    static <B, V> StreamCodec<B, V> of(StreamEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B buffer) {
                return decoder.decode(buffer);
            }

            @Override
            public void encode(B buffer, V value) {
                encoder.encode(buffer, value);
            }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(StreamMemberEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return of((buffer, value) -> encoder.encode(value, buffer), decoder);
    }

    /**
     * Writes nothing and always reads {@code value}.
     */
    static <B, V> StreamCodec<B, V> unit(V value) {
        return of((buffer, v) -> {
            if (!v.equals(value)) {
                throw new IllegalStateException("Can't encode '" + v + "', expected '" + value + "'");
            }
        }, buffer -> value);
    }

    static <B, C, T1> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
                                                 Function<T1, C> factory) {
        return of((buffer, value) -> codec1.encode(buffer, getter1.apply(value)),
                buffer -> factory.apply(codec1.decode(buffer)));
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
                                                     StreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
                                                     BiFunction<T1, T2, C> factory) {
        return of((buffer, value) -> {
            codec1.encode(buffer, getter1.apply(value));
            codec2.encode(buffer, getter2.apply(value));
        }, buffer -> {
            T1 v1 = codec1.decode(buffer);
            T2 v2 = codec2.decode(buffer);
            return factory.apply(v1, v2);
        });
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
                                                         StreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
                                                         StreamCodec<? super B, T3> codec3, Function<C, T3> getter3,
                                                         Function3<T1, T2, T3, C> factory) {
        return of((buffer, value) -> {
            codec1.encode(buffer, getter1.apply(value));
            codec2.encode(buffer, getter2.apply(value));
            codec3.encode(buffer, getter3.apply(value));
        }, buffer -> {
            T1 v1 = codec1.decode(buffer);
            T2 v2 = codec2.decode(buffer);
            T3 v3 = codec3.decode(buffer);
            return factory.apply(v1, v2, v3);
        });
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
                                                             StreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
                                                             StreamCodec<? super B, T3> codec3, Function<C, T3> getter3,
                                                             StreamCodec<? super B, T4> codec4, Function<C, T4> getter4,
                                                             Function4<T1, T2, T3, T4, C> factory) {
        return of((buffer, value) -> {
            codec1.encode(buffer, getter1.apply(value));
            codec2.encode(buffer, getter2.apply(value));
            codec3.encode(buffer, getter3.apply(value));
            codec4.encode(buffer, getter4.apply(value));
        }, buffer -> {
            T1 v1 = codec1.decode(buffer);
            T2 v2 = codec2.decode(buffer);
            T3 v3 = codec3.decode(buffer);
            T4 v4 = codec4.decode(buffer);
            return factory.apply(v1, v2, v3, v4);
        });
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
                                                                 StreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
                                                                 StreamCodec<? super B, T3> codec3, Function<C, T3> getter3,
                                                                 StreamCodec<? super B, T4> codec4, Function<C, T4> getter4,
                                                                 StreamCodec<? super B, T5> codec5, Function<C, T5> getter5,
                                                                 Function5<T1, T2, T3, T4, T5, C> factory) {
        return of((buffer, value) -> {
            codec1.encode(buffer, getter1.apply(value));
            codec2.encode(buffer, getter2.apply(value));
            codec3.encode(buffer, getter3.apply(value));
            codec4.encode(buffer, getter4.apply(value));
            codec5.encode(buffer, getter5.apply(value));
        }, buffer -> {
            T1 v1 = codec1.decode(buffer);
            T2 v2 = codec2.decode(buffer);
            T3 v3 = codec3.decode(buffer);
            T4 v4 = codec4.decode(buffer);
            T5 v5 = codec5.decode(buffer);
            return factory.apply(v1, v2, v3, v4, v5);
        });
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
                                                                     StreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
                                                                     StreamCodec<? super B, T3> codec3, Function<C, T3> getter3,
                                                                     StreamCodec<? super B, T4> codec4, Function<C, T4> getter4,
                                                                     StreamCodec<? super B, T5> codec5, Function<C, T5> getter5,
                                                                     StreamCodec<? super B, T6> codec6, Function<C, T6> getter6,
                                                                     Function6<T1, T2, T3, T4, T5, T6, C> factory) {
        return of((buffer, value) -> {
            codec1.encode(buffer, getter1.apply(value));
            codec2.encode(buffer, getter2.apply(value));
            codec3.encode(buffer, getter3.apply(value));
            codec4.encode(buffer, getter4.apply(value));
            codec5.encode(buffer, getter5.apply(value));
            codec6.encode(buffer, getter6.apply(value));
        }, buffer -> {
            T1 v1 = codec1.decode(buffer);
            T2 v2 = codec2.decode(buffer);
            T3 v3 = codec3.decode(buffer);
            T4 v4 = codec4.decode(buffer);
            T5 v5 = codec5.decode(buffer);
            T6 v6 = codec6.decode(buffer);
            return factory.apply(v1, v2, v3, v4, v5, v6);
        });
    }

    /**
     * Applies an operation like {@link ByteBufCodecs#list()}: {@code ITEM_CODEC.apply(ByteBufCodecs.list())}.
     */
    default <O> StreamCodec<B, O> apply(CodecOperation<B, V, O> operation) {
        return operation.apply(this);
    }

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
        StreamCodec<B, V> self = this;
        return of((buffer, value) -> self.encode(buffer, from.apply(value)), buffer -> to.apply(self.decode(buffer)));
    }

    default <O extends ByteBuf> StreamCodec<O, V> mapStream(Function<O, ? extends B> bufferTransformer) {
        StreamCodec<B, V> self = this;
        return of((buffer, value) -> self.encode(bufferTransformer.apply(buffer), value),
                buffer -> self.decode(bufferTransformer.apply(buffer)));
    }

    /**
     * Writes the key of a value ({@code typeGetter}), then the value with the codec of that key.
     */
    @SuppressWarnings("unchecked")
    default <U> StreamCodec<B, U> dispatch(Function<? super U, ? extends V> typeGetter,
                                           Function<? super V, ? extends StreamCodec<? super B, ? extends U>> codecGetter) {
        StreamCodec<B, V> self = this;
        return of((buffer, value) -> {
            V key = typeGetter.apply(value);
            self.encode(buffer, key);
            ((StreamCodec<? super B, U>) codecGetter.apply(key)).encode(buffer, value);
        }, buffer -> codecGetter.apply(self.decode(buffer)).decode(buffer));
    }

    @SuppressWarnings("unchecked")
    default <S extends B> StreamCodec<S, V> cast() {
        return (StreamCodec<S, V>) this;
    }

    /**
     * Turns a codec into another one, e.g. of a list of its values (1.21 {@code StreamCodec.CodecOperation}).
     */
    @FunctionalInterface
    interface CodecOperation<B, S, T> {
        StreamCodec<B, T> apply(StreamCodec<B, S> codec);
    }
}
