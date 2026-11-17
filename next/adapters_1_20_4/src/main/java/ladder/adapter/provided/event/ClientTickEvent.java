/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided.event;

import net.neoforged.bus.api.Event;

/** NeoForge 20.5's {@code ClientTickEvent} (Pre / Post), posted from 1.20.4's {@code TickEvent.ClientTickEvent} (TickBridge). */
public abstract class ClientTickEvent extends Event {
    public ClientTickEvent() {
    }

    public static class Pre extends ClientTickEvent {
        public Pre() {
        }
    }

    public static class Post extends ClientTickEvent {
        public Post() {
        }
    }
}
