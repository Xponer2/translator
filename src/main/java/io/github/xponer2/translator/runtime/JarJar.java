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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * NeoForge JarJar on 1.12.2: the jars a mod carries in META-INF/jarjar (libraries like Registrate, sometimes other
 * mods) are taken out and loaded like the jars of the mods folder; the mods are started after their dependencies.
 */
final class JarJar {
	private static final String DIR = "META-INF/jarjar/";

	private JarJar() {
	}

	/** The jars inside a mod jar, extracted once into {@code dir} (by entry name). */
	static List<File> extract(File jar, File dir) throws IOException {
		var out = new ArrayList<File>();
		try (var zip = new ZipFile(jar)) {
			// the jars metadata.json names (cristellib keeps jankson in META-INF/jars/), else those in META-INF/jarjar/
			var listed = new java.util.HashSet<String>();
			ZipEntry metadata = zip.getEntry(DIR + "metadata.json");
			if (metadata != null) {
				try (InputStream in = zip.getInputStream(metadata)) {
					var json = new com.google.gson.JsonParser().parse(new String(ModJar.readAll(in), java.nio.charset.StandardCharsets.UTF_8));
					for (var element : json.getAsJsonObject().getAsJsonArray("jars")) {
						if (element.getAsJsonObject().has("path")) {
							listed.add(element.getAsJsonObject().get("path").getAsString());
						}
					}
				} catch (RuntimeException e) {
					Conversion.LOGGER.warn("Can't read the JarJar metadata of {}: {}", jar.getName(), e.toString());
				}
			}
			for (ZipEntry entry : Collections.list(zip.entries())) {
				String name = entry.getName();
				boolean inDir = name.startsWith(DIR) && name.endsWith(".jar") && name.indexOf('/', DIR.length()) < 0;
				if (!inDir && !listed.contains(name)) {
					continue;
				}
				File file = new File(dir, name.substring(name.lastIndexOf('/') + 1));
				if (!file.isFile() || file.length() != entry.getSize()) {
					dir.mkdirs();
					try (InputStream in = zip.getInputStream(entry); OutputStream to = new FileOutputStream(file)) {
						to.write(ModJar.readAll(in));
					}
				}
				out.add(file);
			}
		}
		return out;
	}

	/** A library the game already has (Mixin, MixinExtras, a library of 1.12.2 or the Translator): not loaded twice. */
	static boolean alreadyPresent(ModJar library, TargetClasses target) {
		int checked = 0;
		for (String name : library.classes.keySet()) {
			if (name.endsWith("package-info") || name.endsWith("module-info")) {
				continue;
			}
			if (target.exists(name)) {
				return true;
			}
			if (++checked >= 20) {
				break;
			}
		}
		return false;
	}

	/** The mods with each one after the mods it depends on ([[dependencies]] of its mods.toml), else in the given order. */
	static List<ModJar> dependencyOrder(List<ModJar> mods) {
		var byId = new HashMap<String, ModJar>();
		for (ModJar jar : mods) {
			for (String id : jar.modIds()) {
				byId.put(id, jar);
			}
		}
		Set<ModJar> ordered = new LinkedHashSet<>();
		Set<ModJar> visiting = new HashSet<>();
		for (ModJar jar : mods) {
			visit(jar, byId, ordered, visiting);
		}
		return new ArrayList<>(ordered);
	}

	/**
	 * A mod that uses another mod's classes without declaring it (Twilight Forest's Cosmetic Armor support, behind
	 * ModList.isLoaded) depends on it here: converted before it, the other mod's classes are there to check its calls
	 * against, else they are stubbed as missing. Not when the other mod depends on this one already (no cycles).
	 */
	static void addUsedMods(List<ModJar> mods) {
		var owners = new HashMap<String, ModJar>();
		var byId = new HashMap<String, ModJar>();
		for (ModJar jar : mods) {
			for (String name : jar.classes.keySet()) {
				owners.putIfAbsent(name, jar);
			}
			for (String id : jar.modIds()) {
				byId.put(id, jar);
			}
		}
		for (ModJar jar : mods) {
			if (jar.modIds().isEmpty()) {
				continue;
			}
			var used = new LinkedHashSet<ModJar>();
			for (ClassInfo info : jar.classes.values()) {
				for (String type : info.supertypes()) {
					used.add(owners.get(type));
				}
				for (String call : info.calls) {
					used.add(owners.get(call.substring(0, call.indexOf(' '))));
				}
			}
			used.remove(null);
			used.remove(jar);
			for (ModJar other : used) {
				if (!other.modIds().isEmpty() && !jar.dependencies.contains(other.modIds().get(0))
						&& !dependsOn(other, jar, byId, new HashSet<>())) {
					jar.dependencies.add(other.modIds().get(0));
				}
			}
		}
	}

	private static boolean dependsOn(ModJar jar, ModJar target, Map<String, ModJar> byId, Set<ModJar> seen) {
		if (jar == target) {
			return true;
		}
		if (!seen.add(jar)) {
			return false;
		}
		for (String dependency : jar.dependencies) {
			ModJar other = byId.get(dependency);
			if (other != null && dependsOn(other, target, byId, seen)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The mods of {@code remaining} none of whose dependencies is still in it. When every one waits (a cycle), only the
	 * first (dependency order) goes: all of them at once converted mods before the mods they depend on (Sophisticated
	 * Backpacks before Sophisticated Core, its calls to Core stubbed as missing).
	 */
	static List<ModJar> ready(List<ModJar> remaining, Set<ModJar> done) {
		var pending = new HashSet<String>();
		for (ModJar jar : remaining) {
			pending.addAll(jar.modIds());
		}
		var wave = new ArrayList<ModJar>();
		for (ModJar jar : remaining) {
			boolean waits = false;
			for (String dependency : jar.dependencies) {
				waits |= pending.contains(dependency) && !jar.modIds().contains(dependency);
			}
			if (!waits) {
				wave.add(jar);
			}
		}
		if (wave.isEmpty() && !remaining.isEmpty()) {
			// a cycle: the first mod and the mods it waits for that wait for it too, together (a wave sees itself)
			ModJar first = remaining.get(0);
			wave.add(first);
			for (ModJar other : remaining) {
				if (other != first && reaches(first, other, remaining) && reaches(other, first, remaining)) {
					wave.add(other);
				}
			}
		}
		return wave;
	}

	/** Whether {@code from} depends on {@code to}, directly or through the other remaining mods. */
	private static boolean reaches(ModJar from, ModJar to, List<ModJar> remaining) {
		var todo = new java.util.ArrayDeque<ModJar>();
		var seen = new HashSet<ModJar>();
		todo.add(from);
		while (!todo.isEmpty()) {
			ModJar current = todo.poll();
			if (!seen.add(current)) {
				continue;
			}
			for (String dependency : current.dependencies) {
				for (ModJar other : remaining) {
					if (other.modIds().contains(dependency)) {
						if (other == to) {
							return true;
						}
						todo.add(other);
					}
				}
			}
		}
		return false;
	}

	private static void visit(ModJar jar, Map<String, ModJar> byId, Set<ModJar> ordered, Set<ModJar> visiting) {
		if (ordered.contains(jar) || !visiting.add(jar)) {
			return; // done, or a cycle: its place stays the given order
		}
		for (String dependency : jar.dependencies) {
			ModJar other = byId.get(dependency);
			if (other != null && other != jar) {
				visit(other, byId, ordered, visiting);
			}
		}
		visiting.remove(jar);
		ordered.add(jar);
	}
}
