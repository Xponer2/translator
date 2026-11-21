/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.attachment;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;

/**
 * Where the attachment syncing hooks in (registered by FML from these annotations): the payload, and the first sync of
 * what a player starts seeing (21.1's initial sync).
 */
public final class AttachmentEvents {
    private AttachmentEvents() {
    }

    /** the payload (optional: a server or client without the adapters ignores it) */
    @Mod.EventBusSubscriber(modid = "ladder_adapters", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Payloads {
        private Payloads() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlerEvent event) {
            event.registrar("ladder_adapters").optional().play(SyncAttachmentsPayload.ID, SyncAttachmentsPayload::read,
                    handlers -> handlers.client((payload, context) -> context.workHandler().execute(
                            () -> context.player().ifPresent(player -> AttachmentSync.receive(payload, player.level())))));
        }
    }

    /** a chunk, an entity, the player itself: their synced attachments go to the player that starts seeing them */
    @Mod.EventBusSubscriber(modid = "ladder_adapters", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Game {
        private Game() {
        }

        @SubscribeEvent
        public static void chunkSent(ChunkWatchEvent.Sent event) {
            AttachmentSync.chunkSent(event.getChunk(), event.getPlayer());
        }

        @SubscribeEvent
        public static void startTracking(PlayerEvent.StartTracking event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                AttachmentSync.sendAll(event.getTarget(), player);
            }
        }

        @SubscribeEvent
        public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                AttachmentSync.self(player);
            }
        }

        @SubscribeEvent
        public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                AttachmentSync.self(player);
            }
        }

        @SubscribeEvent
        public static void changedLevel(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                AttachmentSync.self(player);
            }
        }
    }
}
