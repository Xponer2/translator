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
