/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import ladder.rung121.attachment.AttachmentSync;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;

/** The rung 1.21.1 -> 1.21's adapters: what they set up when the game starts. */
@Mod("ladder_adapters_1_21")
public final class LadderAdapters121 {
    public LadderAdapters121(IEventBus modBus) {
        // NeoForge 21.1's attachment syncing: its payload, and the first sync of what a player starts seeing
        modBus.addListener(ladder.rung121.attachment.AttachmentPayloads::register);
        modBus.addListener(ladder.rung121.VillagerTypes::register);
        modBus.addListener(ladder.rung121.PoiExtensions::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(ChunkWatchEvent.Sent.class, AttachmentSync::onChunkSent);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.StartTracking.class, AttachmentSync::onStartTracking);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, AttachmentSync::onSelf);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, AttachmentSync::onSelf);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, AttachmentSync::onSelf);
    }
}
