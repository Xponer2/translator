/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.network;

/**
 * Writes a value to a buffer, value first (1.21 {@code net.minecraft.network.codec.StreamMemberEncoder}), for
 * {@code StreamCodec.ofMember(Payload::write, Payload::new)}.
 */
@FunctionalInterface
public interface StreamMemberEncoder<O, T> {
    void encode(T value, O buffer);
}
