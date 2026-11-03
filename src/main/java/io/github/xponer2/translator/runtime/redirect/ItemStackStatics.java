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

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.util.Codecs;

import net.minecraft.item.ItemStack;

/** The static fields 1.21 {@code ItemStack} has and 1.12.2's doesn't (runtime). */
public final class ItemStackStatics {
	public static final Codec<ItemStack> CODEC = Codecs.ITEM_STACK;
	public static final Codec<ItemStack> OPTIONAL_CODEC = Codecs.OPTIONAL_ITEM_STACK;
	/** 1.21 checks the components strictly; here the same as {@link #CODEC}. */
	public static final Codec<ItemStack> STRICT_CODEC = Codecs.ITEM_STACK;
	public static final Codec<ItemStack> SINGLE_ITEM_CODEC = Codecs.SINGLE_ITEM;
	public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> STREAM_CODEC = ByteBufCodecs.ITEM_STACK;
	public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_STREAM_CODEC = ByteBufCodecs.OPTIONAL_ITEM_STACK;

	private ItemStackStatics() {
	}
}
