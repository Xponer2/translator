/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.runtime.neoforge;

import java.util.ArrayList;
import java.util.List;

/** FML LoadingModList: the mods found (the runtime 1.21.1 mods). */
public final class LoadingModList {
	private static final LoadingModList INSTANCE = new LoadingModList();

	private LoadingModList() {
	}

	public static LoadingModList get() {
		return INSTANCE;
	}

	public ModFileInfo getModFileById(String modId) {
		ModContainer container = ModContainer.byId(modId);
		return container == null ? null : container.getModInfo().getOwningFile();
	}

	public List<ModInfo> getMods() {
		var mods = new ArrayList<ModInfo>();
		for (ModContainer container : ModContainer.all()) {
			mods.add(container.getModInfo());
		}
		return mods;
	}
}
