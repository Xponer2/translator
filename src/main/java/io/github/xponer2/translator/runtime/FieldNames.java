/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.runtime;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * The 1.12.2 names of the fields 1.21.1 mods look up by reflection with their 1.21 names (a string the converter
 * can't see): the F lines of the remap table, read once when first asked.
 */
public final class FieldNames {
	@Nullable
	private static volatile RemapTable table;
	private static volatile boolean failed;

	private FieldNames() {
	}

	/** The 1.12.2 name of the 1.21 field {@code name} of that class (its 1.21 names' F lines), null when none maps it. */
	@Nullable
	public static String of(Class<?> owner, String name) {
		RemapTable remap = table();
		if (remap == null) {
			return null;
		}
		for (String older : remap.namesOf(owner.getName().replace('.', '/'))) {
			Map<String, String> fields = remap.fields.get(older);
			String mapped = fields == null ? null : fields.get(name);
			if (mapped != null) {
				return mapped;
			}
		}
		return null;
	}

	@Nullable
	private static RemapTable table() {
		RemapTable remap = table;
		if (remap == null && !failed) {
			synchronized (FieldNames.class) {
				if (table == null && !failed) {
					try {
						table = RemapTable.load();
					} catch (java.io.IOException | RuntimeException e) {
						failed = true;
						RuntimeStubs.failed("reading the remap table for a field name", e);
					}
				}
				remap = table;
			}
		}
		return remap;
	}
}
