/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.event;

import ladder.adapter.provided.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;

/** The client half of {@link TickBridge} (loaded on the client only). */
final class ClientTickBridge {
    private ClientTickBridge() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> NeoForge.EVENT_BUS.post(
                event.phase == TickEvent.Phase.START ? new ClientTickEvent.Pre() : new ClientTickEvent.Post()));
    }
}
