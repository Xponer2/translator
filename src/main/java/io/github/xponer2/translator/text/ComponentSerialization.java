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
package io.github.xponer2.translator.text;

import java.util.Optional;

import com.mojang.serialization.Codec;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.util.Codecs;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 {@code ComponentSerialization} (neoforge-1.21.1-runtime.tsv): text in JSON and on the network, with 1.12.2's
 * text components. The carried 1.21 class builds its codecs from 1.21 parts 1.12.2's components lack (their contents,
 * the style map codec), so every text a mod reads with it was null (AE2's screen labels).
 */
public final class ComponentSerialization {
    public static final Codec<ITextComponent> CODEC = Codecs.COMPONENT;
    public static final Codec<ITextComponent> FLAT_CODEC = Codecs.COMPONENT;
    public static final StreamCodec<ByteBuf, ITextComponent> STREAM_CODEC = ByteBufCodecs.COMPONENT;
    public static final StreamCodec<ByteBuf, ITextComponent> TRUSTED_STREAM_CODEC = ByteBufCodecs.COMPONENT;
    public static final StreamCodec<ByteBuf, ITextComponent> TRUSTED_CONTEXT_FREE_STREAM_CODEC = ByteBufCodecs.COMPONENT;
    public static final StreamCodec<ByteBuf, Optional<ITextComponent>> OPTIONAL_STREAM_CODEC = ByteBufCodecs.optional(ByteBufCodecs.COMPONENT);
    public static final StreamCodec<ByteBuf, Optional<ITextComponent>> TRUSTED_OPTIONAL_STREAM_CODEC = ByteBufCodecs.optional(ByteBufCodecs.COMPONENT);

    private ComponentSerialization() {
    }

    /** A text at most that long once written (1.21 limits it; 1.12.2's text has no such check). */
    public static Codec<ITextComponent> flatRestrictedCodec(int maxSize) {
        return Codecs.COMPONENT;
    }
}
