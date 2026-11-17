/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter;

import net.neoforged.fml.common.Mod;

/**
 * The adapters' mod: its mixins put the newer versions' method splits into 1.20.4's call sites, and the event bridges
 * post the newer versions' events from 1.20.4's.
 */
@Mod("ladder_adapters")
public final class LadderAdapters {
    public LadderAdapters(net.neoforged.bus.api.IEventBus modBus) {
        ladder.adapter.event.TickBridge.register();
        ladder.adapter.component.ComponentRegistry.register(modBus);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            // the frame's time for the trackers made of a partial tick
            ladder.adapter.provided.DeltaTracker.Client.timer = ladder.adapter.render.RenderAdapters.TIMER;
        }
    }
}
