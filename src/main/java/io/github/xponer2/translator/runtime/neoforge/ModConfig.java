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

/** NeoForge {@code ModConfig}: only its config types are used by mods directly. */
public final class ModConfig {
	private final Type type;
	private final IConfigSpec spec;
	private final ModContainer container;
	private final String fileName;

	public ModConfig(Type type, IConfigSpec spec, ModContainer container, String fileName) {
		this.type = type;
		this.spec = spec;
		this.container = container;
		this.fileName = fileName;
	}

	public Type getType() {
		return this.type;
	}

	public IConfigSpec getSpec() {
		return this.spec;
	}

	public String getModId() {
		return this.container.getModId();
	}

	public String getFileName() {
		return this.fileName;
	}

	/** 1.21 {@code getFullPath()}: where the file is (config/ with its name). */
	public java.nio.file.Path getFullPath() {
		return FMLPaths.CONFIGDIR.get().resolve(this.fileName);
	}

	public void save() {
	}

	/** The config as it was last read (NeoForge's {@code loadedConfig}), null before the first read. */
	@javax.annotation.Nullable
	private IConfigSpec.ILoadedConfig loadedConfig;

	/** NeoForge {@code getLoadedConfig()} (a mod reading its own file's values: Moonlight). */
	@javax.annotation.Nullable
	public IConfigSpec.ILoadedConfig getLoadedConfig() {
		return this.loadedConfig;
	}

	/**
	 * NeoForge's package-private {@code setConfig(loadedConfig, eventConstructor)}: the config a mod read itself is given
	 * to the spec (Moonlight calls it by reflection with FML's carried LoadedConfig, which is an ILoadedConfig; without
	 * the method its config class failed to start and every config of the mods built on it with it).
	 */
	void setConfig(@javax.annotation.Nullable IConfigSpec.ILoadedConfig config, @javax.annotation.Nullable java.util.function.Function<?, ?> event) {
		this.loadedConfig = config;
		if (config != null) {
			this.spec.acceptConfig(config);
		}
	}

	/** What the loader read. */
	void loaded(IConfigSpec.ILoadedConfig config) {
		this.loadedConfig = config;
	}

	public enum Type {
		COMMON,
		CLIENT,
		SERVER,
		STARTUP;

		public String extension() {
			return this.name().toLowerCase(java.util.Locale.ROOT);
		}
	}
}
