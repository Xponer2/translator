/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.network;

import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

/**
 * 1.21 {@code ServerboundCustomPayloadPacket}: a mod's payload packet a client sends itself through the connection (Common Networking: JourneyMap's
 * handshake and teleports). The carried 1.21 packet reached the other side as a packet 1.12.2 can't process and was
 * dropped, so JourneyMap never learned its server has it (no teleport, client-only waypoints). It is an FML packet of
 * the payload's channel now, read on the server by the payload's registered codec and handler.
 */
public class ServerboundCustomPayloadPacket extends FMLProxyPacket {
	public ServerboundCustomPayloadPacket(CustomPacketPayload payload) {
		super(NetworkChannels.encoded(payload, false), NetworkChannels.channelNameOf(payload));
	}
}
