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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/** neoforgespi IModInfo / FML ModInfo: what a mod knows about a mod (from its mods.toml). */
public final class ModInfo {
	private final String modId;
	private final String displayName;
	private final String description;
	private final ArtifactVersion version;
	private final ModFileInfo owningFile;

	public ModInfo(String modId, String displayName, String description, String version, ModFileInfo owningFile) {
		this.modId = modId;
		this.displayName = displayName;
		this.description = description;
		this.version = new DefaultArtifactVersion(version);
		this.owningFile = owningFile;
	}

	public String getModId() {
		return this.modId;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public String getDescription() {
		return this.description;
	}

	public ArtifactVersion getVersion() {
		return this.version;
	}

	public String getNamespace() {
		return this.modId;
	}

	public ModFileInfo getOwningFile() {
		return this.owningFile;
	}

	public Optional<String> getLogoFile() {
		return Optional.empty();
	}

	public Optional<java.net.URL> getModURL() {
		return Optional.empty();
	}

	public List<Object> getDependencies() {
		return Collections.emptyList();
	}
}
