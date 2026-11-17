/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.runtime.neoforge;

import java.nio.file.Path;

/**
 * cpw's {@code SecureJar} (the jar of a NeoForge mod as a module): its root path, the path of the jar file. Patchouli
 * walks its books from {@code getRootPath()}; without it the book registry failed before it told the model loading
 * that the books were read, and the game waited forever.
 */
public final class SecureJar {
	private final ModFile file;

	SecureJar(ModFile file) {
		this.file = file;
	}

	/** The root of the jar's contents. */
	public Path getRootPath() {
		return this.file.findResource("/");
	}

	public Path getPrimaryPath() {
		return this.file.getFilePath();
	}

	public String name() {
		String name = this.file.getFileName();
		return name.endsWith(".jar") ? name.substring(0, name.length() - 4) : name;
	}
}
