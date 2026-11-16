/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
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
