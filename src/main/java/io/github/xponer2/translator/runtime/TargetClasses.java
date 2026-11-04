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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import net.minecraft.launchwrapper.Launch;

/**
 * The classes a converted mod runs against: Minecraft 1.12.2, Forge and the Translator, read from the class path
 * without loading them, with the development environment's names (MCP): in the release game {@link Mappings} reads them so.
 */
final class TargetClasses {
	// read by several conversions at once
	private final Map<String, Optional<ClassInfo>> cache = new java.util.concurrent.ConcurrentHashMap<>();
	/** Class bytes by binary name ("a.b.C"), null when there is no such class. */
	private final ClassBytes source;
	/**
	 * Jars being converted together (libraries converted in parallel): read as they are until their converted copies
	 * are on the class path, so night-config's toml sees night-config's core while both are converted.
	 */
	private final java.util.List<java.util.zip.ZipFile> pending = new java.util.concurrent.CopyOnWriteArrayList<>();
	private final java.util.Set<String> fromPending = java.util.concurrent.ConcurrentHashMap.newKeySet();

	TargetClasses() {
		this(gameClasses());
	}

	/** The game's class path; in the release game read with MCP member names (see {@link Mappings}). */
	private static ClassBytes gameClasses() {
		ClassBytes raw = Launch.classLoader::getClassBytes;
		Mappings mappings = Mappings.instance();
		return mappings != null ? mappings.deobfuscating(raw) : raw;
	}

	/** Another class path than the game's (a conversion outside the game, for tests). */
	TargetClasses(ClassBytes source) {
		this.source = source;
	}

	interface ClassBytes {
		byte[] get(String binaryName) throws IOException;
	}

	ClassInfo get(String internalName) {
		Optional<ClassInfo> cached = this.cache.get(internalName);
		if (cached != null) {
			return cached.orElse(null);
		}
		try {
			byte[] bytes = this.source.get(internalName.replace('/', '.'));
			if (bytes == null) {
				bytes = this.pendingBytes(internalName);
			}
			Optional<ClassInfo> info = bytes == null ? Optional.empty() : Optional.of(ClassInfo.read(bytes));
			Optional<ClassInfo> before = this.cache.putIfAbsent(internalName, info);
			return (before != null ? before : info).orElse(null);
		} catch (IOException | RuntimeException | LinkageError e) {
			// a read that failed is not a missing class: not cached, tried again next time (the release game's classes
			// went through FML's patcher and deobfuscation, which threw when converter threads met there; a class
			// cached as missing then failed every method that named it: "not loading ..." in 946 stubbed methods)
			if (FAILED_READS.incrementAndGet() <= 20) {
				Conversion.LOGGER.warn("Translator: couldn't read the class {}: {}", internalName, e.toString());
			}
			if (this.failures.merge(internalName, 1, Integer::sum) >= 3) {
				// it fails every time: missing after all
				this.cache.putIfAbsent(internalName, Optional.empty());
			}
			return null;
		}
	}

	private final Map<String, Integer> failures = new java.util.concurrent.ConcurrentHashMap<>();

	private static final java.util.concurrent.atomic.AtomicInteger FAILED_READS = new java.util.concurrent.atomic.AtomicInteger();

	private final Map<String, byte[]> declarations = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * A class of the class path (or of a jar converted together with this one) without its code, for the downgrader's
	 * lookups of supertypes and members: it never reads their code, and parsing it was much of what downgrading cost.
	 * Those of the class path are kept for every jar; null when there is no such class.
	 */
	byte[] declarations(String internalName) {
		byte[] cached = this.declarations.get(internalName);
		if (cached != null) {
			return cached;
		}
		try {
			byte[] bytes = this.source.get(internalName.replace('/', '.'));
			if (bytes != null) {
				byte[] stripped = stripCode(bytes);
				this.declarations.put(internalName, stripped);
				return stripped;
			}
			bytes = this.pendingBytes(internalName);
			return bytes == null ? null : stripCode(bytes);
		} catch (IOException | RuntimeException e) {
			return null;
		}
	}

	static byte[] stripCode(byte[] bytes) {
		var writer = new xyz.wagyourtail.jvmdg.shade.asm.ClassWriter(0);
		new xyz.wagyourtail.jvmdg.shade.asm.ClassReader(bytes).accept(writer, xyz.wagyourtail.jvmdg.shade.asm.ClassReader.SKIP_CODE);
		return writer.toByteArray();
	}

	private byte[] pendingBytes(String internalName) throws IOException {
		for (var jar : this.pending) {
			var entry = jar.getEntry(internalName + ".class");
			if (entry != null) {
				try (var in = jar.getInputStream(entry)) {
					this.fromPending.add(internalName);
					return ModJar.readAll(in);
				}
			}
		}
		return null;
	}

	/** The jars of a batch converted together: each sees the others' classes. */
	void addPending(java.util.List<java.io.File> jars) {
		for (var jar : jars) {
			try {
				this.pending.add(new java.util.zip.ZipFile(jar));
			} catch (IOException e) {
				Conversion.LOGGER.warn("Can't read {}: {}", jar.getName(), e.toString());
			}
		}
		this.forgetMissing();
	}

	/** After the batch: its converted jars are on the class path (or failed), the originals aren't read any more. */
	void clearPending() {
		for (var jar : this.pending) {
			try {
				jar.close();
			} catch (IOException ignored) {
			}
		}
		this.pending.clear();
		this.cache.keySet().removeAll(this.fromPending);
		this.fromPending.clear();
		this.forgetMissing();
	}

	boolean exists(String internalName) {
		return this.get(internalName) != null;
	}

	/** After a jar joined the class path (the 1.21.1 API, a library, a mod): classes not found before may exist now. */
	void forgetMissing() {
		this.cache.values().removeIf(info -> !info.isPresent());
	}

	/**
	 * A method (with that many parameters) or field of that name declared by the class or a Translator supertype of
	 * it: the Translator classes are written with the 1.21 names, whatever the descriptor erases to.
	 */
	boolean hasTranslatorMember(String owner, String name, String desc, boolean method) {
		int params = method ? xyz.wagyourtail.jvmdg.shade.asm.Type.getArgumentTypes(desc).length : 0;
		var todo = new ArrayDeque<String>();
		var seen = new HashSet<String>();
		todo.add(owner);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			if (!seen.add(current) || !current.startsWith("io/github/xponer2/translator/")) {
				continue;
			}
			ClassInfo info = this.get(current);
			if (info == null) {
				continue;
			}
			for (String member : method ? info.methods : info.fields) {
				if (member.startsWith(name + " ") && (!method
						|| xyz.wagyourtail.jvmdg.shade.asm.Type.getArgumentTypes(member.substring(name.length() + 1)).length == params)) {
					return true;
				}
			}
			todo.addAll(info.supertypes());
		}
		return false;
	}

	/**
	 * A field of that name of the class or one of its supertypes whose type is a supertype of {@code desc}'s (the
	 * Translator's ModelPart is a ModelRenderer): read as that type, cast back.
	 */
	boolean hasFieldNamed(String owner, String name, String desc) {
		var todo = new ArrayDeque<String>();
		var seen = new HashSet<String>();
		todo.add(owner);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			if (!seen.add(current)) {
				continue;
			}
			ClassInfo info = this.get(current);
			if (info == null) {
				continue;
			}
			for (String field : info.fields) {
				if (field.startsWith(name + " ")) {
					String type = field.substring(name.length() + 1);
					return type.startsWith("L") && desc.startsWith("L")
							&& this.isSubclass(desc.substring(1, desc.length() - 1), type.substring(1, type.length() - 1));
				}
			}
			todo.addAll(info.supertypes());
		}
		return false;
	}

	/** Whether the class is {@code wanted} or has it among its supertypes (classes this lookup can read). */
	boolean isSubclass(String name, String wanted) {
		var todo = new ArrayDeque<String>();
		var seen = new HashSet<String>();
		todo.add(name);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			if (current.equals(wanted)) {
				return true;
			}
			if (!seen.add(current)) {
				continue;
			}
			ClassInfo info = this.get(current);
			if (info != null) {
				todo.addAll(info.supertypes());
			}
		}
		return false;
	}

	/** A method or field (name + descriptor) of the class or one of its supertypes. */
	boolean hasMember(String owner, String name, String desc, boolean method) {
		var todo = new ArrayDeque<String>();
		var seen = new HashSet<String>();
		todo.add(owner);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			if (!seen.add(current)) {
				continue;
			}
			ClassInfo info = this.get(current);
			if (info == null) {
				continue;
			}
			if (method ? info.methods.contains(name + " " + desc) : info.fields.contains(name + " " + desc)) {
				return true;
			}
			todo.addAll(info.supertypes());
		}
		return false;
	}
}
