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
package io.github.xponer2.translator.runtime.redirect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.util.Codecs;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fluids.FluidStack;

/** The static fields NeoForge's {@code FluidStack} has and Forge's doesn't (runtime): its codecs. */
public final class FluidStackStatics {
	public static final Codec<FluidStack> CODEC = Codecs.FLUID_STACK.flatXmap(FluidStackStatics::nonEmpty, FluidStackStatics::nonEmpty);
	/** Empty is {@code null} on 1.12.2 (Forge's FluidStack has no empty stack). */
	public static final Codec<FluidStack> OPTIONAL_CODEC = Codecs.FLUID_STACK;
	public static final StreamCodec<ByteBuf, FluidStack> STREAM_CODEC = ByteBufCodecs.FLUID_STACK;
	public static final StreamCodec<ByteBuf, FluidStack> OPTIONAL_STREAM_CODEC = ByteBufCodecs.FLUID_STACK;

	private FluidStackStatics() {
	}

	private static DataResult<FluidStack> nonEmpty(FluidStack stack) {
		return stack == null || stack.amount <= 0 ? DataResult.error("Fluid stack must not be empty") : DataResult.success(stack);
	}
}
