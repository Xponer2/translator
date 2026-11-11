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
package io.github.xponer2.translator.runtime;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Files of the conversion cache are written under a name of this game alone and appear under their own name whole.
 * Two games of one folder (a client and a server, two instances) converting at once wrote the same jar and the same
 * temporary files: the second game took a jar the first was still writing, and classes in it were missing for good
 * (Supplementaries found no Moonlight class it extends; Night Config's ConfigFormat was missing).
 */
final class CacheFiles {
	/** This game's own part of a temporary name. */
	private static final String OWN = Long.toHexString(System.nanoTime()) + "-"
			+ java.lang.management.ManagementFactory.getRuntimeMXBean().getName().replaceAll("[^A-Za-z0-9]", "_");

	private CacheFiles() {
	}

	/** A file next to {@code target} only this game writes. */
	static File partial(File target) {
		return new File(target.getParentFile(), target.getName() + "." + OWN + ".part");
	}

	/** A temporary file of the cache only this game uses ({@code name} as the conversion called it). */
	static File temporary(File dir, String name) {
		return new File(dir, name + "." + OWN);
	}

	/**
	 * The written file under its own name, in one step. When another game was first (its file may be open there and
	 * can't be replaced), the one already there is kept: both made it from the same input.
	 */
	static void publish(File partial, File target) throws IOException {
		try {
			Files.move(partial.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			return;
		} catch (AtomicMoveNotSupportedException e) {
			// a file system without one-step moves: replaced below
		} catch (IOException e) {
			if (target.isFile()) {
				partial.delete();
				return;
			}
		}
		try {
			Files.move(partial.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			if (!target.isFile()) {
				throw e;
			}
			partial.delete();
		}
	}
}
