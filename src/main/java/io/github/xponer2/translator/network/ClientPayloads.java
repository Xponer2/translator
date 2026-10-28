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
