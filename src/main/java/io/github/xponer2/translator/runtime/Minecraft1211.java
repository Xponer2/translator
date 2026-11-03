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
package io.github.xponer2.translator.runtime;

import java.io.File;

import javax.annotation.Nullable;

/**
 * The player's own Minecraft 1.21.1 client jar (never shipped with the Translator): the vanilla 1.21 data the converted
 * mods expect (tags such as {@code minecraft:dirt}) comes from it. {@code -Dtranslator.minecraft=<jar>}, else the jar of
 * the NeoForge development cache, the official launcher or Prism Launcher.
 */
public final class Minecraft1211 {
	private static File jar;
	private static boolean looked;

	private Minecraft1211() {
	}

	@Nullable
	public static synchronized File clientJar() {
		if (!looked) {
			looked = true;
			jar = find();
		}
		return jar;
	}

	@Nullable
	private static File find() {
		String property = System.getProperty("translator.minecraft");
		if (property != null) {
			File file = new File(property);
			return file.isFile() ? file : null;
		}
		String home = System.getProperty("user.home");
		String appData = System.getenv("APPDATA");
		var candidates = new java.util.ArrayList<File>();
		candidates.add(new File(home, ".gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"));
		if (appData != null) {
			candidates.add(new File(appData, ".minecraft/versions/1.21.1/1.21.1.jar"));
			candidates.add(new File(appData, "PrismLauncher/libraries/com/mojang/minecraft/1.21.1/minecraft-1.21.1-client.jar"));
		}
		candidates.add(new File(home, ".minecraft/versions/1.21.1/1.21.1.jar"));
		candidates.add(new File(home, "Library/Application Support/minecraft/versions/1.21.1/1.21.1.jar"));
		for (File candidate : candidates) {
			if (candidate.isFile()) {
				return candidate;
			}
		}
		return null;
	}
}
