/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.runtime.neoforge;

import java.util.function.Supplier;

import io.github.xponer2.translator.config.ModConfigSpec;

/** FML: the mod being constructed (set by the runtime loader around each @Mod constructor). */
public final class ModLoadingContext {
	private static final ModLoadingContext INSTANCE = new ModLoadingContext();
	private static ModContainer active;

	private ModLoadingContext() {
	}

	public static ModLoadingContext get() {
		return INSTANCE;
	}

	public static void setActive(ModContainer container) {
		active = container;
	}

	public ModContainer getActiveContainer() {
		return active;
	}

	public String getActiveNamespace() {
		return active == null ? "minecraft" : active.getModId();
	}

	public <T extends IExtensionPoint> void registerExtensionPoint(Class<? extends T> point, Supplier<? extends T> extension) {
	}

	public void registerConfig(ModConfig.Type type, IConfigSpec spec) {
		if (active != null) {
			active.registerConfig(type, spec);
		}
	}

	public void registerConfig(ModConfig.Type type, IConfigSpec spec, String fileName) {
		if (active != null) {
			active.registerConfig(type, spec, fileName);
		}
	}

	public void registerConfig(ModConfig.Type type, ModConfigSpec spec) {
		if (active != null) {
			active.registerConfig(type, spec);
		}
	}

	public void registerConfig(ModConfig.Type type, ModConfigSpec spec, String fileName) {
		if (active != null) {
			active.registerConfig(type, spec, fileName);
		}
	}
}
