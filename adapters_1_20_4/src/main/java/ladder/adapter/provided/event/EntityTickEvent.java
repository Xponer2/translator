/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided.event;

import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge 20.5's {@code EntityTickEvent} (P rows): around each entity's tick (a passenger's ride tick too), on both
 * sides; a cancelled Pre skips the tick and its Post. Posted where 20.5 posts it (mixin.EntityTickEventMixin).
 */
public abstract class EntityTickEvent extends Event {
    private final Entity entity;

    protected EntityTickEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public static class Pre extends EntityTickEvent implements ICancellableEvent {
        public Pre(Entity entity) {
            super(entity);
        }
    }

    public static class Post extends EntityTickEvent {
        public Post(Entity entity) {
            super(entity);
        }
    }

    /** the tick (or ride tick) with the events around it */
    public static void ticked(Entity entity, Runnable tick) {
        if (NeoForge.EVENT_BUS.post(new Pre(entity)).isCanceled()) {
            return;
        }
        tick.run();
        NeoForge.EVENT_BUS.post(new Post(entity));
    }
}
