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
package ladder.adapter.event;

import ladder.adapter.provided.event.LevelTickEvent;
import ladder.adapter.provided.event.PlayerTickEvent;
import ladder.adapter.provided.event.ServerTickEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;

/**
 * The newer versions' tick events, posted from 1.20.4's: NeoForge 20.5 split {@code TickEvent} (one event with a phase)
 * into a Pre and a Post event per kind. A converted mod's listeners of the new events run where 1.20.4 ticks.
 */
public final class TickBridge {
    private TickBridge() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(TickBridge::server);
        NeoForge.EVENT_BUS.addListener(TickBridge::level);
        NeoForge.EVENT_BUS.addListener(TickBridge::player);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientTickBridge.register();
        }
    }

    private static void server(TickEvent.ServerTickEvent event) {
        NeoForge.EVENT_BUS.post(event.phase == TickEvent.Phase.START ? new ServerTickEvent.Pre(event::haveTime, event.getServer())
                : new ServerTickEvent.Post(event::haveTime, event.getServer()));
    }

    private static void level(TickEvent.LevelTickEvent event) {
        NeoForge.EVENT_BUS.post(event.phase == TickEvent.Phase.START ? new LevelTickEvent.Pre(event::haveTime, event.level)
                : new LevelTickEvent.Post(event::haveTime, event.level));
    }

    private static void player(TickEvent.PlayerTickEvent event) {
        NeoForge.EVENT_BUS.post(event.phase == TickEvent.Phase.START ? new PlayerTickEvent.Pre(event.player) : new PlayerTickEvent.Post(event.player));
    }
}
