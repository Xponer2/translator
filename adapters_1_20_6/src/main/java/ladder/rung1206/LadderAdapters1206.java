/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/** The rung 1.21 -> 1.20.6's adapters: what they set up when the game starts. */
@Mod("ladder_adapters_1_20_6")
public final class LadderAdapters1206 {
    public LadderAdapters1206(IEventBus modBus) {
        ladder.rung1206.event.DamageEvents.register();
        if (FMLEnvironment.dist.isClient()) {
            // the frame's time for the trackers made of a partial tick
            ladder.rung1206.provided.DeltaTracker.Client.timer = ladder.rung1206.render.TimeAdapters.TIMER;
        }
    }
}
