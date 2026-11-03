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

/** neoforgespi IModFileInfo / FML ModFileInfo: a mod jar and its mods. */
public final class ModFileInfo {
	private final ModFile file;
	final List<ModInfo> mods = new ArrayList<>();

	public ModFileInfo(ModFile file) {
		this.file = file;
	}

	public ModFile getFile() {
		return this.file;
	}

	public List<ModInfo> getMods() {
		return this.mods;
	}

	public String moduleName() {
		return this.mods.isEmpty() ? this.file.getFileName() : this.mods.get(0).getModId();
	}

	public String versionString() {
		return this.mods.isEmpty() ? "0" : this.mods.get(0).getVersion().toString();
	}

	public String getLicense() {
		return "";
	}
}
