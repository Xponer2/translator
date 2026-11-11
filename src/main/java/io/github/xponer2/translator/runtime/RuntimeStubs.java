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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Called by the methods MethodGuard replaced: logs each once (what the Translator can't run yet). */
public final class RuntimeStubs {
	private static final Set<String> REPORTED = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private RuntimeStubs() {
	}

	/** Told about every call (the self-test attributes them to what it tests); null when nothing listens. */
	public static volatile java.util.function.Consumer<String> listener;

	/** What failed since the game started ("failed in where: error"), for the self-test's record of each mod. */
	private static final Set<String> FAILED = Collections.newSetFromMap(new ConcurrentHashMap<>());

	/** Every call reported since the game started: the missing ones as logged, the failed ones as "failed in ...". */
	public static Set<String> reported() {
		var all = new java.util.TreeSet<String>();
		for (String entry : REPORTED) {
			// a failure's key is its place only: its text with the error is in FAILED
			if (entry.indexOf(' ') > 0 && entry.indexOf('(') > 0) {
				all.add(entry);
			}
		}
		all.addAll(FAILED);
		return all;
	}

	public static void missing(String what) {
		var current = listener;
		if (current != null) {
			current.accept(what);
		}
		if (REPORTED.add(what)) {
			RuntimeMods.LOGGER.warn("Not run on 1.12.2 yet: {}", what);
		}
	}

	/**
	 * A 1.21.1 mod's code failed where the game calls it outside any guard (a tooltip while the search tree is
	 * rebuilt, ...): logged once, the game goes on (an exception there would stop the server thread).
	 */
	public static void failed(String where, Throwable error) {
		var current = listener;
		if (current != null) {
			current.accept("failed in " + where + ": " + error);
		}
		if (REPORTED.add(where)) {
			String text = String.valueOf(error);
			int line = text.indexOf(10);
			FAILED.add("failed in " + where + ": " + (line < 0 ? text : text.substring(0, line)));
			RuntimeMods.LOGGER.error("A 1.21.1 mod failed in {} (further errors there are not logged)", where, error);
		}
	}
}
