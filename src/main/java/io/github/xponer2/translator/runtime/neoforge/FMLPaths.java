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

import java.nio.file.Path;

import net.minecraft.launchwrapper.Launch;

/** FML: the game folders. */
public enum FMLPaths {
	GAMEDIR(""),
	MODSDIR("mods"),
	CONFIGDIR("config"),
	FMLCONFIG("config/fml.toml");

	private final String relative;

	FMLPaths(String relative) {
		this.relative = relative;
	}

	public Path get() {
		Path game = (Launch.minecraftHome == null ? new java.io.File(".") : Launch.minecraftHome).toPath().toAbsolutePath().normalize();
		return this.relative.isEmpty() ? game : game.resolve(this.relative);
	}

	public Path relative() {
		return java.nio.file.Paths.get(this.relative);
	}
}
