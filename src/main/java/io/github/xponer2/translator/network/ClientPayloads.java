/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

/**
 * The client half of {@link NetworkChannels}: only loaded on the client, where its packet events fire.
 */
final class ClientPayloads {
    private ClientPayloads() { }

    static void handle(IPayloadHandler<?> handler, CustomPacketPayload payload) {
        var minecraft = Minecraft.getMinecraft();
        var context = new NetworkChannels.Context(ClientPayloads::player, minecraft, p -> PacketDistributor.sendToServer(p));
        minecraft.addScheduledTask(() -> NetworkChannels.handle(handler, payload, context));
    }

    private static EntityPlayer player() {
        return Minecraft.getMinecraft().player;
    }
}
