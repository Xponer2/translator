package io.github.xponer2.translator.network;

/**
 * Writes a value to a buffer (1.21 {@code net.minecraft.network.codec.StreamEncoder}).
 */
@FunctionalInterface
public interface StreamEncoder<O, T> {
    void encode(O buffer, T value);
}
