/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** NeoForge 20.5's {@code PlayerTickEvent} (Pre / Post), posted from 1.20.4's {@code TickEvent.PlayerTickEvent} (TickBridge). */
public abstract class PlayerTickEvent extends PlayerEvent {
    protected PlayerTickEvent(Player player) {
        super(player);
    }

    public static class Pre extends PlayerTickEvent {
        public Pre(Player player) {
            super(player);
        }
    }

    public static class Post extends PlayerTickEvent {
        public Post(Player player) {
            super(player);
        }
    }
}
