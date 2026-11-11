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
package io.github.xponer2.translator.runtime.redirect;

import java.util.UUID;

import net.minecraft.network.play.server.SPacketUpdateBossInfo;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.BossInfo;

/**
 * 1.21 {@code ClientboundBossEventPacket.create*Packet}: the 1.12.2 boss bar packet. The carried factories made the
 * packet with a constructor 1.12.2 hasn't, so they gave null: a mod's own boss bar (Iron's Spells' fire boss) sent it
 * and the server crashed when a player came near.
 */
public final class BossPackets {
	private BossPackets() {
	}

	public static SPacketUpdateBossInfo createAddPacket(BossInfo event) {
		return new SPacketUpdateBossInfo(SPacketUpdateBossInfo.Operation.ADD, event);
	}

	public static SPacketUpdateBossInfo createRemovePacket(UUID id) {
		BossInfo removed = new BossInfo(id, new TextComponentString(""), BossInfo.Color.WHITE, BossInfo.Overlay.PROGRESS) {
		};
		return new SPacketUpdateBossInfo(SPacketUpdateBossInfo.Operation.REMOVE, removed);
	}

	public static SPacketUpdateBossInfo createUpdateProgressPacket(BossInfo event) {
		return new SPacketUpdateBossInfo(SPacketUpdateBossInfo.Operation.UPDATE_PCT, event);
	}

	public static SPacketUpdateBossInfo createUpdateNamePacket(BossInfo event) {
		return new SPacketUpdateBossInfo(SPacketUpdateBossInfo.Operation.UPDATE_NAME, event);
	}

	public static SPacketUpdateBossInfo createUpdateStylePacket(BossInfo event) {
		return new SPacketUpdateBossInfo(SPacketUpdateBossInfo.Operation.UPDATE_STYLE, event);
	}

	public static SPacketUpdateBossInfo createUpdatePropertiesPacket(BossInfo event) {
		return new SPacketUpdateBossInfo(SPacketUpdateBossInfo.Operation.UPDATE_PROPERTIES, event);
	}
}
