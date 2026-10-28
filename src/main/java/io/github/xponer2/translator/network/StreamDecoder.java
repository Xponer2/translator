package io.github.xponer2.translator.network;

/**
 * Reads a value from a buffer (1.21 {@code net.minecraft.network.codec.StreamDecoder}).
 */
@FunctionalInterface
public interface StreamDecoder<I, T> {
    T decode(I buffer);
}
