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
