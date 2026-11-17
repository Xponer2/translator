/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client;

import net.minecraft.client.audio.ISound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.21 {@code SoundInstance} getters that 1.12.2's sound has as floats ({@code getXPosF}). */
@SideOnly(Side.CLIENT)
public final class SoundBridges {
	private SoundBridges() {
	}

	public static double getX(ISound sound) {
		return sound.getXPosF();
	}

	public static double getY(ISound sound) {
		return sound.getYPosF();
	}

	public static double getZ(ISound sound) {
		return sound.getZPosF();
	}
}
