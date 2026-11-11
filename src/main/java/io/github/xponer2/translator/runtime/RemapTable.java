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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The names of NeoForge 1.21.1 classes and members (Mojang names) on Minecraft 1.12.2: Translator classes or 1.12.2
 * classes with their MCP names. Read from {@code assets/translator/runtime/*.tsv} (see tools/runtime/build_tables.py).
 */
final class RemapTable {
	final Map<String, String> classes = new HashMap<>();
	/** owner -> "name(params)" -> 1.12.2 name; params as dotted names, nested classes with $, arrays T[]. */
	final Map<String, Map<String, String>> methods = new HashMap<>();
	final Map<String, Map<String, String>> fields = new HashMap<>();
	final Map<String, String> methodNames = new HashMap<>();
	final Map<String, String> fieldNames = new HashMap<>();
	/** 1.21 class -> the Translator facade a mod class extends instead (neoforge-1.21.1-supers.tsv). */
	final Map<String, String> supers = new HashMap<>();
	/** 1.12.2 class -> Translator classes with static stand-ins for what it lacks (neoforge-1.21.1-redirects.tsv). */
	final Map<String, java.util.List<String>> redirects = new HashMap<>();
	/**
	 * "owner.name:desc" of a field -> the constant reads of it give (K lines of neoforge-1.21.1-redirects.tsv): a
	 * library switch the Translator needs on (Registrate's skipErrors: one entry that can't be made yet mustn't stop
	 * the registration of the others).
	 */
	final Map<String, Object> constants = new HashMap<>();
	/**
	 * X lines: a call of a member the 1.12.2 class has, but with another meaning than the 1.21 one the mods call
	 * ("owner.name desc" -> helper class): always made a static call of the helper's method of that name, taking the
	 * receiver first. EnumDyeColor.getName() is "silver" on 1.12.2, the 1.21 name is "light_gray".
	 */
	final Map<String, String> forced = new HashMap<>();
	/**
	 * I lines: {@code instanceof} of a 1.21 class that shares its 1.12.2 class with other 1.21 classes ("1.21 class" ->
	 * helper class, method): the check is the helper's {@code method(Object)}, which tells them apart. EntityHitResult and
	 * BlockHitResult are both 1.12.2's RayTraceResult: {@code hit instanceof EntityHitResult} was true for a block too.
	 */
	final Map<String, String[]> kinds = new HashMap<>();
	/**
	 * E lines: a facade (as an S line) only for the mod classes that extend the 1.21 class: their superclass and super
	 * calls are the facade's (its 1.21 constructors), while {@code new} of the class itself stays the 1.12.2 class's (its
	 * constructor helpers pick the right 1.12.2 class: an EntityDamageSource, a target goal for a mob that isn't a
	 * creature).
	 */
	final java.util.Set<String> extendsOnly = new java.util.HashSet<>();
	private Map<String, java.util.List<String>> reverse;

	static RemapTable load() throws IOException {
		var table = new RemapTable();
		table.read("/assets/translator/runtime/neoforge-1.21.1.tsv");
		table.read("/assets/translator/runtime/neoforge-1.21.1-runtime.tsv");
		table.read("/assets/translator/runtime/neoforge-1.21.1-carried.tsv");
		table.read("/assets/translator/runtime/neoforge-1.21.1-supers.tsv");
		table.read("/assets/translator/runtime/neoforge-1.21.1-redirects.tsv");
		return table;
	}

	private void read(String resource) throws IOException {
		try (InputStream in = RemapTable.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new IOException("Missing " + resource);
			}
			var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] cols = line.split("\t");
				switch (cols[0]) {
					case "C" -> this.classes.put(cols[1], cols[2]);
					case "M" -> this.methods.computeIfAbsent(cols[1], k -> new HashMap<>()).put(cols[2], cols[3]);
					case "F" -> this.fields.computeIfAbsent(cols[1], k -> new HashMap<>()).put(cols[2], cols[3]);
					case "m" -> this.methodNames.put(cols[1], cols[2]);
					case "f" -> this.fieldNames.put(cols[1], cols[2]);
					case "S" -> this.supers.put(cols[1], cols[2]);
					case "R" -> this.redirects.computeIfAbsent(cols[1], k -> new java.util.ArrayList<>()).add(cols[2]);
					// K owner name desc value: int-like fields (Z, B, C, S, I) as a number
					case "K" -> this.constants.put(cols[1] + "." + cols[2] + ":" + cols[3], Integer.valueOf(cols[4]));
					// X owner name desc helper [helperDesc]: the helper's descriptor when it takes wider types (Object for a
					// carried 1.21 class the Translator can't name)
					case "X" -> this.forced.put(cols[1] + "." + cols[2] + cols[3], cols.length > 5 && !cols[5].isEmpty() ? cols[4] + " " + cols[5] : cols[4]);
					case "I" -> this.kinds.put(cols[1], new String[] {cols[2], cols[3]});
					case "E" -> {
						this.supers.put(cols[1], cols[2]);
						this.extendsOnly.add(cols[1]);
					}
					default -> {
					}
				}
			}
		}
	}

	/** The 1.21 classes that map to a 1.12.2 / Translator class (the class itself when none does). */
	synchronized java.util.List<String> namesOf(String mapped) {
		if (this.reverse == null) {
			this.reverse = new HashMap<>();
			for (var entry : this.classes.entrySet()) {
				this.reverse.computeIfAbsent(entry.getValue(), k -> new java.util.ArrayList<>()).add(entry.getKey());
			}
		}
		// a 1.12.2 class nothing maps to whose name is another class in 1.21 (1.12.2 Gui, the base of every button and
		// screen, is the HUD in 1.21: GuiIngame) has no 1.21 name
		return this.reverse.getOrDefault(mapped, this.classes.containsKey(mapped) ? java.util.Collections.emptyList()
				: java.util.Collections.singletonList(mapped));
	}

	/** "name(params)" as the table writes method keys, from a method descriptor. */
	static String methodKey(String name, String desc) {
		var key = new StringBuilder(name).append('(');
		int i = 1;
		boolean first = true;
		while (desc.charAt(i) != ')') {
			if (!first) {
				key.append(',');
			}
			first = false;
			int dims = 0;
			while (desc.charAt(i) == '[') {
				dims++;
				i++;
			}
			char c = desc.charAt(i);
			if (c == 'L') {
				int end = desc.indexOf(';', i);
				key.append(desc, i + 1, end);
				i = end + 1;
			} else {
				key.append(switch (c) {
					case 'Z' -> "boolean";
					case 'B' -> "byte";
					case 'C' -> "char";
					case 'S' -> "short";
					case 'I' -> "int";
					case 'J' -> "long";
					case 'F' -> "float";
					default -> "double";
				});
				i++;
			}
			for (int d = 0; d < dims; d++) {
				key.append("[]");
			}
		}
		// the table writes dotted class names
		int open = key.indexOf("(");
		return key.substring(0, open + 1) + key.substring(open + 1).replace('/', '.') + ")";
	}
}
