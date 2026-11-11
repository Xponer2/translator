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

import io.github.xponer2.translator.config.ModConfigSpec;

/** NeoForge {@code ModContainer} of an original 1.21.1 mod run by the Translator. */
public class ModContainer {
	/** The containers of the runtime 1.21.1 mods by mod id (all of them, before the first one is constructed). */
	private static final java.util.Map<String, ModContainer> BY_ID = new java.util.LinkedHashMap<>();

	private final String modId;
	private final String displayName;
	private final String version;
	private ModInfo modInfo;

	public ModContainer(String modId, String displayName, String version) {
		this(modId, displayName, version, "", null);
	}

	public ModContainer(String modId, String displayName, String version, String description, java.io.File jar) {
		this.modId = modId;
		this.displayName = displayName;
		this.version = version;
		java.io.File own = jar;
		if (own == null && "minecraft".equals(modId)) {
			// the files of Minecraft 1.21.1 itself (its data: structure sets, tags, loot tables) are in the player's 1.21.1
			// jar; a mod reading them through the "minecraft" mod file found nothing at "minecraft.jar" (Cristel Lib read
			// the vanilla structure sets so, failed, and the mods built on it failed with it)
			own = io.github.xponer2.translator.runtime.Minecraft1211.clientJar();
		}
		var file = new ModFileInfo(new ModFile(own == null ? new java.io.File(modId + ".jar") : own));
		this.modInfo = new ModInfo(modId, displayName, description, version, file);
		file.mods.add(this.modInfo);
		BY_ID.put(modId, this);
	}

	public static ModContainer byId(String modId) {
		return BY_ID.get(modId);
	}

	public static java.util.Collection<ModContainer> all() {
		return BY_ID.values();
	}

	public ModInfo getModInfo() {
		return this.modInfo;
	}

	public String getNamespace() {
		return this.modId;
	}

	/** Config screens, display tests, ...: nothing on 1.12.2 asks for them. */
	public <T extends IExtensionPoint> void registerExtensionPoint(Class<? extends T> point, T extension) {
	}

	public <T extends IExtensionPoint> void registerExtensionPoint(Class<? extends T> point, java.util.function.Supplier<? extends T> extension) {
	}

	public <T extends IExtensionPoint> java.util.Optional<T> getCustomExtension(Class<? extends T> point) {
		return java.util.Optional.empty();
	}

	public String getModId() {
		return this.modId;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public String getVersion() {
		return this.version;
	}

	public IEventBus getEventBus() {
		return ForgeBus.FORGE;
	}

	/** An original 1.21.1 mod's NeoForge config (carried ModConfigSpec): config/<modid>-<type>.toml, loaded now. */
	public void registerConfig(ModConfig.Type type, IConfigSpec spec) {
		this.registerConfig(type, spec, this.modId + "-" + type.extension() + ".toml");
	}

	public void registerConfig(ModConfig.Type type, IConfigSpec spec, String fileName) {
		ConfigLoader.load(new ModConfig(type, spec, this, fileName));
	}

	/** Loads the spec from {@code config/<modid>-<type>.cfg} (1.21 uses .toml files of that name). */
	public void registerConfig(ModConfig.Type type, ModConfigSpec spec) {
		this.registerConfig(type, spec, this.modId + "-" + type.extension() + ".toml");
	}

	public void registerConfig(ModConfig.Type type, ModConfigSpec spec, String fileName) {
		spec.register(fileName.replaceAll("\\.toml$", ".cfg"));
	}
}
