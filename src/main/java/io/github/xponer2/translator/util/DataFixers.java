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
package io.github.xponer2.translator.util;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

/**
 * 1.21 {@code DataFixers}: the data a 1.21.1 mod saves on 1.12.2 is already its own version, so its fixer changes
 * nothing. The carried class builds all of 1.21's fixers and failed (Sophisticated Backpacks' placed backpacks
 * couldn't be read).
 */
public final class DataFixers {
	private static final DataFixer NONE = new DataFixer() {
		@Override
		public <T> Dynamic<T> update(DSL.TypeReference type, Dynamic<T> input, int version, int newVersion) {
			return input;
		}

		@Override
		public Schema getSchema(int key) {
			return null;
		}
	};

	private DataFixers() {
	}

	public static DataFixer getDataFixer() {
		return NONE;
	}
}
