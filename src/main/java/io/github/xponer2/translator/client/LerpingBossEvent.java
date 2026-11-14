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
package io.github.xponer2.translator.client;

import java.util.UUID;

import net.minecraft.client.gui.BossInfoClient;
import net.minecraft.network.play.server.SPacketUpdateBossInfo;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.BossInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code LerpingBossEvent} (S row) for the client boss bars mods make themselves (Twilight Forest's
 * boss bars, added by its own packets): 1.12.2's client boss bar with the 1.21 constructor. 1.12.2 makes one only from
 * the server's boss bar packet: the bar is made from the packet those values would have been, so the bosses of the
 * Twilight had no bar at all.
 */
@SideOnly(Side.CLIENT)
public class LerpingBossEvent extends BossInfoClient {
	public LerpingBossEvent(UUID id, ITextComponent name, float progress, BossInfo.Color color, BossInfo.Overlay overlay, boolean darkenScreen,
			boolean playMusic, boolean createWorldFog) {
		super(packet(id, name, progress, color, overlay, darkenScreen, playMusic, createWorldFog));
	}

	private static SPacketUpdateBossInfo packet(UUID id, ITextComponent name, float progress, BossInfo.Color color, BossInfo.Overlay overlay,
			boolean darkenScreen, boolean playMusic, boolean createWorldFog) {
		BossInfo info = new BossInfo(id, name, color == null ? BossInfo.Color.PURPLE : color, overlay == null ? BossInfo.Overlay.PROGRESS : overlay) {
		};
		info.setPercent(progress);
		info.setDarkenSky(darkenScreen);
		info.setPlayEndBossMusic(playMusic);
		info.setCreateFog(createWorldFog);
		return new SPacketUpdateBossInfo(SPacketUpdateBossInfo.Operation.ADD, info);
	}
}
