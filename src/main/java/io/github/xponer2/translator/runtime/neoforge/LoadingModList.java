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
