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
