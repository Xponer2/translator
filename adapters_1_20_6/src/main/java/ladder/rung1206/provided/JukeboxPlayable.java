/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

/**
 * 1.21's {@code JukeboxPlayable} item component (a P row): the song an item plays in a jukebox, by its key (1.21 holds it
 * as a key or a holder, its codec writes the key) and whether the tooltip shows it.
 */
public record JukeboxPlayable(ResourceKey<JukeboxSong> songKey, boolean showInTooltip) {
    public static final Codec<JukeboxPlayable> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(ladder.rung1206.jukebox.Jukebox.JUKEBOX_SONG).fieldOf("song").forGetter(JukeboxPlayable::songKey),
            Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(JukeboxPlayable::showInTooltip)).apply(i, JukeboxPlayable::new));
    public static final StreamCodec<ByteBuf, JukeboxPlayable> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(ladder.rung1206.jukebox.Jukebox.JUKEBOX_SONG), JukeboxPlayable::songKey,
            ByteBufCodecs.BOOL, JukeboxPlayable::showInTooltip, JukeboxPlayable::new);

    /** 1.21's song: the key (a holder once looked up) */
    public ladder.carried_1_20_6.net.minecraft.world.item.EitherHolder<JukeboxSong> song() {
        return new ladder.carried_1_20_6.net.minecraft.world.item.EitherHolder<>(this.songKey);
    }

    public JukeboxPlayable withTooltip(boolean show) {
        return new JukeboxPlayable(this.songKey, show);
    }
}
