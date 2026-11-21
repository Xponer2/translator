/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.jukebox;

import java.util.Optional;

import ladder.rung1206.provided.JukeboxPlayable;
import ladder.rung1206.provided.JukeboxSong;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 1.21's jukebox songs on 1.20.6: the data pack registry {@code jukebox_song} (synced to the clients), the item
 * component {@code jukebox_playable}, and the jukebox playing such an item as 1.20.6 plays a music disc
 * (mixin.Jukebox*Mixin, client.JukeboxMusicMixin).
 */
public final class Jukebox {
    public static final ResourceKey<Registry<JukeboxSong>> JUKEBOX_SONG = ResourceKey.createRegistryKey(new ResourceLocation("jukebox_song"));
    /** made before the items (their properties take it), registered with the component types */
    public static final DataComponentType<JukeboxPlayable> JUKEBOX_PLAYABLE = DataComponentType.<JukeboxPlayable>builder().persistent(JukeboxPlayable.CODEC)
            .networkSynchronized(JukeboxPlayable.STREAM_CODEC).build();

    private Jukebox() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((DataPackRegistryEvent.NewRegistry event) -> event.dataPackRegistry(JUKEBOX_SONG, JukeboxSong.DIRECT_CODEC, JukeboxSong.DIRECT_CODEC));
        modBus.addListener((RegisterEvent event) -> event.register(Registries.DATA_COMPONENT_TYPE, new ResourceLocation("jukebox_playable"), () -> JUKEBOX_PLAYABLE));
    }

    // ---------------------------------------------------------------- the rows' adapters

    /** {@code Registries.JUKEBOX_SONG} (an F row) */
    public static ResourceKey<Registry<JukeboxSong>> jukeboxSongRegistry() {
        return JUKEBOX_SONG;
    }

    /** {@code DataComponents.JUKEBOX_PLAYABLE} (an F row) */
    public static DataComponentType<JukeboxPlayable> jukeboxPlayable() {
        return JUKEBOX_PLAYABLE;
    }

    /** {@code properties.jukeboxPlayable(song)} (an X row) */
    public static Item.Properties jukeboxPlayable(Item.Properties properties, ResourceKey<JukeboxSong> song) {
        return properties.component(JUKEBOX_PLAYABLE, new JukeboxPlayable(song, true));
    }

    // ---------------------------------------------------------------- the jukebox

    public static boolean playable(ItemStack stack) {
        return stack.has(JUKEBOX_PLAYABLE);
    }

    /** the song a stack plays, from the level's registries */
    public static Optional<Holder.Reference<JukeboxSong>> song(RegistryAccess registries, ItemStack stack) {
        JukeboxPlayable playable = stack.get(JUKEBOX_PLAYABLE);
        return playable == null ? Optional.empty() : registries.registry(JUKEBOX_SONG).flatMap(r -> r.getHolder(playable.songKey()));
    }
}
