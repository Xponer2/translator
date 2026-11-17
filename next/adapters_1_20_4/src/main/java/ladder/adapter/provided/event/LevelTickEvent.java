/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided.event;

import java.util.function.BooleanSupplier;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/** NeoForge 20.5's {@code LevelTickEvent} (Pre / Post), posted from 1.20.4's {@code TickEvent.LevelTickEvent} (TickBridge). */
public abstract class LevelTickEvent extends Event {
    private final BooleanSupplier hasTime;
    private final Level level;

    protected LevelTickEvent(BooleanSupplier hasTime, Level level) {
        this.hasTime = hasTime;
        this.level = level;
    }

    public boolean hasTime() {
        return this.hasTime.getAsBoolean();
    }

    public Level getLevel() {
        return this.level;
    }

    public static class Pre extends LevelTickEvent {
        public Pre(BooleanSupplier hasTime, Level level) {
            super(hasTime, level);
        }
    }

    public static class Post extends LevelTickEvent {
        public Post(BooleanSupplier hasTime, Level level) {
            super(hasTime, level);
        }
    }
}
