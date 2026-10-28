package io.github.xponer2.translator.network;

/**
 * Writes a value to a buffer, value first (1.21 {@code net.minecraft.network.codec.StreamMemberEncoder}), for
 * {@code StreamCodec.ofMember(Payload::write, Payload::new)}.
 */
@FunctionalInterface
public interface StreamMemberEncoder<O, T> {
    void encode(T value, O buffer);
}
