/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.util;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.Side;

/**
 * NeoForge FMLEnvironment: the physical side and whether this is a release (obfuscated) environment.
 */
public final class FMLEnvironment {
	/** False in the development environment. */
	public static final boolean production = !Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"));
	/**
	 * From the launch handler: known from the start (a mixin plugin reads it while the mixin configs are chosen, before
	 * FMLCommonHandler knows the side: its NullPointerException left this class broken and the Translator failed).
	 */
	public static final Side dist = net.minecraftforge.fml.relauncher.FMLLaunchHandler.side();

	private FMLEnvironment() {
	}
}
