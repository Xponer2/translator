/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.network;

/** The direction of a packet (1.21 {@code PacketFlow}, neoforge-1.21.1-runtime.tsv). */
public enum PacketFlow {
	SERVERBOUND,
	CLIENTBOUND;

	public PacketFlow getOpposite() {
		return this == CLIENTBOUND ? SERVERBOUND : CLIENTBOUND;
	}

	public boolean isClientbound() {
		return this == CLIENTBOUND;
	}

	public boolean isServerbound() {
		return this == SERVERBOUND;
	}

	public String id() {
		return this == CLIENTBOUND ? "clientbound" : "serverbound";
	}
}
