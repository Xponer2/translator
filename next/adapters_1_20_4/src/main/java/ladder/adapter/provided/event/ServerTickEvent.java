/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
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
