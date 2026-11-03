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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;

/** neoforgespi IModFile: the jar of a mod (its resources). */
public final class ModFile {
	private final File file;
	private FileSystem fileSystem;

	public ModFile(File file) {
		this.file = file;
	}

	public Path getFilePath() {
		return this.file.toPath();
	}

	public String getFileName() {
		return this.file.getName();
	}

	/** The jar as cpw's SecureJar (its root path). */
	public SecureJar getSecureJar() {
		return new SecureJar(this);
	}

	/** NeoForge {@code getScanResult}: the annotations of this jar's classes. */
	public io.github.xponer2.translator.util.ModList.ModFileScanData getScanResult() {
		return io.github.xponer2.translator.runtime.RuntimeMods.scanData(this.file);
	}

	/** A path inside the jar (it may not exist). */
	public synchronized Path findResource(String... path) {
		try {
			if (this.fileSystem == null) {
				this.fileSystem = FileSystems.newFileSystem(URI.create("jar:" + this.file.toURI()), Collections.emptyMap());
			}
		} catch (IOException | RuntimeException e) {
			return this.file.toPath().resolve(String.join("/", path));
		}
		return this.fileSystem.getPath(String.join("/", path));
	}
}
