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
import java.util.function.Function;

import javax.annotation.Nullable;

/**
 * FML 1.21 {@code ConfigTracker}: the configs a mod registers through it instead of its container (Moonlight's config
 * holders: Supplementaries and the mods built on it) go to the Translator's {@link ConfigLoader} like the others, so they
 * are read from config/ when registered. The carried FML tracker kept them to itself and never loaded them: Moonlight
 * then force-loaded them through its private loadConfig, which failed, and Supplementaries registered nothing.
 */
public final class ConfigTracker {
	public static final ConfigTracker INSTANCE = new ConfigTracker();

	private ConfigTracker() {
	}

	public ModConfig registerConfig(ModConfig.Type type, IConfigSpec spec, ModContainer container) {
		return this.registerConfig(type, spec, container, container.getModId() + "-" + type.extension() + ".toml");
	}

	public ModConfig registerConfig(ModConfig.Type type, IConfigSpec spec, ModContainer container, @Nullable String fileName) {
		var modConfig = new ModConfig(type, spec, container,
				fileName != null ? fileName : container.getModId() + "-" + type.extension() + ".toml");
		ConfigLoader.load(modConfig);
		return modConfig;
	}

	/** FML's private loadConfig (Moonlight calls it by reflection to load a config at once): read now. */
	void loadConfig(ModConfig modConfig, Path path, @Nullable Function<?, ?> builder) {
		ConfigLoader.loadNow(modConfig);
	}

	/** A server's synced config on the client: 1.12.2 has no config sync, the client keeps its own file. */
	public static void acceptSyncedConfig(ModConfig modConfig, byte[] bytes) {
	}

	public void loadConfigs(ModConfig.Type type, Path directory) {
		if (type == ModConfig.Type.SERVER) {
			ConfigLoader.loadServerConfigs();
		}
	}

	public void unloadConfigs(ModConfig.Type type) {
	}
}
