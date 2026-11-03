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
package io.github.xponer2.translator.runtime.neoforge;

import io.github.xponer2.translator.util.FMLEnvironment;

import net.minecraftforge.fml.relauncher.Side;

/** NeoForge {@code FMLLoader}: the physical side and the environment. */
public final class FMLLoader {
	private FMLLoader() {
	}

	public static Side getDist() {
		return FMLEnvironment.dist;
	}

	public static boolean isProduction() {
		return FMLEnvironment.production;
	}

	public static LoadingModList getLoadingModList() {
		return LoadingModList.get();
	}

	public static java.nio.file.Path getGamePath() {
		return FMLPaths.GAMEDIR.get();
	}
}
