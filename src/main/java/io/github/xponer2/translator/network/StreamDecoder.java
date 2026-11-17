/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.network;

/**
 * Reads a value from a buffer (1.21 {@code net.minecraft.network.codec.StreamDecoder}).
 */
@FunctionalInterface
public interface StreamDecoder<I, T> {
    T decode(I buffer);
}
