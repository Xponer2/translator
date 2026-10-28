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
package io.github.xponer2.translator.util;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

/**
 * NeoForge FMLEnvironment: the physical side and whether this is a release (obfuscated) environment.
 */
public final class FMLEnvironment {
	/** False in the development environment. */
	public static final boolean production = !Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"));
	public static final Side dist = FMLCommonHandler.instance().getSide();

	private FMLEnvironment() {
	}
}
