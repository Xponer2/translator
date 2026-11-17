/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.runtime.redirect;

import com.mojang.serialization.Codec;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.util.Codecs;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;

/**
 * The static fields and methods 1.21 {@code BlockPos} has and 1.12.2's doesn't (runtime, a missing GETSTATIC or
 * INVOKESTATIC goes here).
 */
public final class BlockPosStatics {
	public static final Codec<BlockPos> CODEC = Codecs.BLOCK_POS;
	public static final StreamCodec<ByteBuf, BlockPos> STREAM_CODEC = ByteBufCodecs.BLOCK_POS;

	private BlockPosStatics() {
	}

	/** The positions of the box, each its own object (1.21 reuses one mutable position, callers copy it). */
	public static java.util.stream.Stream<BlockPos> betweenClosedStream(BlockPos from, BlockPos to) {
		return java.util.stream.StreamSupport.stream(BlockPos.getAllInBox(from, to).spliterator(), false);
	}

	public static Iterable<BlockPos> betweenClosed(BlockPos from, BlockPos to) {
		return BlockPos.getAllInBox(from, to);
	}
}
