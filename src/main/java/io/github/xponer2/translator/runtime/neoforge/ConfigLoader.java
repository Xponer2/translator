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

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;

import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * What FML ConfigTracker does for a mod config: reads config/<file>.toml (made with the defaults when missing or
 * incomplete), hands it to the spec and posts ModConfigEvent.Loading. Client and common configs load when the mod
 * registers them, server configs after registration (no per-world server configs on 1.12.2).
 */
public final class ConfigLoader {
	private static final java.util.List<ModConfig> SERVER_CONFIGS = new java.util.ArrayList<>();
	private static boolean registered;

	private ConfigLoader() {
	}

	static synchronized void load(ModConfig modConfig) {
		if (modConfig.getType() == ModConfig.Type.SERVER && !registered) {
			// NeoForge loads them with a world, after registration: their defaults may name items (Sophisticated
			// Backpacks' stack upgrade list), which don't exist yet while the mods are constructed
			SERVER_CONFIGS.add(modConfig);
			return;
		}
		read(modConfig);
	}

	/** The server configs registered while the mods were constructed: once the registries are filled (init). */
	public static synchronized void loadServerConfigs() {
		registered = true;
		for (ModConfig modConfig : SERVER_CONFIGS) {
			read(modConfig);
		}
		SERVER_CONFIGS.clear();
	}

	private static void read(ModConfig modConfig) {
		Path path = FMLPaths.CONFIGDIR.get().resolve(modConfig.getFileName());
		try {
			java.nio.file.Files.createDirectories(path.getParent());
			CommentedFileConfig config = CommentedFileConfig.builder(path, TomlFormat.instance()).sync().preserveInsertionOrder()
					.writingMode(WritingMode.REPLACE).build();
			config.load();
			IConfigSpec spec = modConfig.getSpec();
			if (!spec.isCorrect(config)) {
				spec.correct(config);
				config.save();
			}
			spec.acceptConfig(new IConfigSpec.ILoadedConfig() {
				@Override
				public CommentedConfig config() {
					return config;
				}

				@Override
				public void save() {
					config.save();
				}
			});
			post("net.neoforged.fml.event.config.ModConfigEvent$Loading", modConfig);
		} catch (Throwable t) {
			ForgeBus.LOGGER.error("Can't load the config {} of {}", modConfig.getFileName(), modConfig.getModId(), t);
		}
	}

	/** The config event classes come with the 1.21.1 API (carried), not with the Translator. */
	private static void post(String eventClass, ModConfig modConfig) throws ReflectiveOperationException {
		Class<?> type = Class.forName(eventClass, true, net.minecraft.launchwrapper.Launch.classLoader);
		ForgeBus.FORGE.post((Event) type.getConstructor(ModConfig.class).newInstance(modConfig));
	}
}
