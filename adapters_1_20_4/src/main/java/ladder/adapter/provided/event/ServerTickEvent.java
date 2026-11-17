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

import java.util.function.BooleanSupplier;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/** NeoForge 20.5's {@code ServerTickEvent} (Pre / Post), posted from 1.20.4's {@code TickEvent.ServerTickEvent} (TickBridge). */
public abstract class ServerTickEvent extends Event {
    private final BooleanSupplier hasTime;
    private final MinecraftServer server;

    protected ServerTickEvent(BooleanSupplier hasTime, MinecraftServer server) {
        this.hasTime = hasTime;
        this.server = server;
    }

    public boolean hasTime() {
        return this.hasTime.getAsBoolean();
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public static class Pre extends ServerTickEvent {
        public Pre(BooleanSupplier hasTime, MinecraftServer server) {
            super(hasTime, server);
        }
    }

    public static class Post extends ServerTickEvent {
        public Post(BooleanSupplier hasTime, MinecraftServer server) {
            super(hasTime, server);
        }
    }
}
