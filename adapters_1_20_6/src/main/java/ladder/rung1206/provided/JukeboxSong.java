/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;

/**
 * 1.21's {@code JukeboxSong} (a P row): a song of the data pack registry {@code jukebox_song} (ladder.rung1206.jukebox.Jukebox
 * makes it on 1.20.6): its sound, the "now playing" text, its length and what a comparator reads from the jukebox.
 */
public record JukeboxSong(Holder<SoundEvent> soundEvent, Component description, float lengthInSeconds, int comparatorOutput) {
    public static final Codec<JukeboxSong> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
            SoundEvent.CODEC.fieldOf("sound_event").forGetter(JukeboxSong::soundEvent),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(JukeboxSong::description),
            ExtraCodecs.POSITIVE_FLOAT.fieldOf("length_in_seconds").forGetter(JukeboxSong::lengthInSeconds),
            Codec.intRange(0, 15).fieldOf("comparator_output").forGetter(JukeboxSong::comparatorOutput)).apply(i, JukeboxSong::new));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, JukeboxSong> DIRECT_STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.composite(SoundEvent.STREAM_CODEC, JukeboxSong::soundEvent,
                    ComponentSerialization.STREAM_CODEC, JukeboxSong::description,
                    net.minecraft.network.codec.ByteBufCodecs.FLOAT, JukeboxSong::lengthInSeconds,
                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT, JukeboxSong::comparatorOutput, JukeboxSong::new);
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Holder<JukeboxSong>> STREAM_CODEC =
            net.minecraft.network.codec.ByteBufCodecs.holder(ladder.rung1206.jukebox.Jukebox.JUKEBOX_SONG, DIRECT_STREAM_CODEC);

    /** the song a stack plays (its jukebox_playable's), from these registries */
    public static java.util.Optional<Holder<JukeboxSong>> fromStack(net.minecraft.core.HolderLookup.Provider registries, net.minecraft.world.item.ItemStack stack) {
        JukeboxPlayable playable = stack.get(ladder.rung1206.jukebox.Jukebox.JUKEBOX_PLAYABLE);
        return playable == null ? java.util.Optional.empty()
                : registries.lookup(ladder.rung1206.jukebox.Jukebox.JUKEBOX_SONG).flatMap(l -> l.get(playable.songKey())).map(h -> (Holder<JukeboxSong>) h);
    }

    public int lengthInTicks() {
        return net.minecraft.util.Mth.ceil(this.lengthInSeconds * 20.0F);
    }

    public boolean hasFinished(long ticksSinceSongStarted) {
        return ticksSinceSongStarted >= this.lengthInTicks() + 20;
    }
}
