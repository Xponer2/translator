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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Full API carry (tools/RUNTIME.md "Direction"): the Minecraft 1.21.1 + NeoForge classes the Translator has no
 * 1.12.2 or Translator class for come from a 1.21.1 API jar on the player's machine (never shipped). They are converted
 * once like a mod (remapped, downgraded, guarded) and put on the class path before the mods, so a mod finds every API
 * class it was compiled against. The classes the tables map (1.12.2 classes, Translator facades) stay mapped: they are
 * where the 1.21 code meets the 1.12.2 game.
 */
final class ApiCarry {
	/** CONSTANT_Utf8 "1.21.1": tag 1, length 6. */
	private static final String VERSION_CONSTANT = "\u0001\0\u00061.21.1";

	private ApiCarry() {
	}

	/**
	 * The API jar: {@code -Dtranslator.api=<jar>}, else the Mojang-named Minecraft + NeoForge jar of the NeoForge
	 * development cache (what 1.21.1 mods are compiled against), else null.
	 */
	static File find() {
		String property = System.getProperty("translator.api");
		if (property != null) {
			File file = new File(property);
			return file.isFile() ? file : null;
		}
		File dir = new File(System.getProperty("user.home"), ".gradle/caches/neoformruntime/intermediate_results");
		File[] candidates = dir.listFiles((d, name) -> name.startsWith("compiledWithNeoForge_") && name.endsWith("_output.jar"));
		if (candidates == null) {
			return null;
		}
		Arrays.sort(candidates, Comparator.comparingLong(File::lastModified).reversed());
		for (File candidate : candidates) {
			if (isMinecraft1211(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	/** NeoForge in it, and "1.21.1" as a constant of SharedConstants (the version string; length-prefixed, not 1.21.10). */
	private static boolean isMinecraft1211(File jar) {
		try (var zip = new ZipFile(jar)) {
			ZipEntry constants = zip.getEntry("net/minecraft/SharedConstants.class");
			if (constants == null || zip.getEntry("net/neoforged/neoforge/common/NeoForge.class") == null) {
				return false;
			}
			try (InputStream in = zip.getInputStream(constants)) {
				byte[] bytes = ModJar.readAll(in);
				byte[] version = VERSION_CONSTANT.getBytes(StandardCharsets.ISO_8859_1);
				outer:
				for (int i = 0; i + version.length <= bytes.length; i++) {
					for (int k = 0; k < version.length; k++) {
						if (bytes[i + k] != version[k]) {
							continue outer;
						}
					}
					return true;
				}
				return false;
			}
		} catch (IOException e) {
			return false;
		}
	}

	/** The libraries of Minecraft 1.21.1 that 1.12.2 doesn't have at all (group, artifact, version). */
	private static final String[][] LIBRARIES = {
			{ "com.mojang", "datafixerupper", "8.0.16" },
			{ "com.mojang", "brigadier", "1.3.10" },
			{ "com.mojang", "logging", "1.2.7" },
			{ "org.joml", "joml", "1.10.5" },
			{ "org.slf4j", "slf4j-api", "2.0.9" },
			{ "com.electronwill.night-config", "core", "3.8.3" },
			{ "com.electronwill.night-config", "toml", "3.8.3" },
			// NeoForge's own libraries mods use directly (GlitchCore's events: TypeResolver)
			{ "net.jodah", "typetools", "0.6.3" },
			// newer versions of what 1.12.2 has: only their new parts are added (see gap)
			{ "it.unimi.dsi", "fastutil", "8.5.12" },
			{ "org.apache.commons", "commons-lang3", "3.14.0" },
			{ "com.google.guava", "guava", "32.1.2-jre" },
			{ "com.google.code.gson", "gson", "2.10.1" },
			{ "commons-io", "commons-io", "2.15.1" },
	};

	/** The FML loader of NeoForge 1.21.1 (carried with the API: its events and interfaces; the loader seam is the Translator's). */
	static File loaderJar() {
		var dirs = new java.util.ArrayList<File>();
		String appData = System.getenv("APPDATA");
		if (appData != null) {
			dirs.add(new File(appData, "PrismLauncher/libraries/net/neoforged/fancymodloader/loader"));
			dirs.add(new File(appData, ".minecraft/libraries/net/neoforged/fancymodloader/loader"));
		}
		dirs.add(new File(System.getProperty("user.home"), ".gradle/caches/modules-2/files-2.1/net.neoforged.fancymodloader/loader"));
		for (File dir : dirs) {
			File[] versions = dir.listFiles((d, name) -> name.startsWith("4.0."));
			if (versions == null) {
				continue;
			}
			Arrays.sort(versions, Comparator.comparing(File::getName).reversed());
			for (File version : versions) {
				File direct = new File(version, "loader-" + version.getName() + ".jar");
				if (direct.isFile()) {
					return direct;
				}
				File[] hashes = version.listFiles();
				for (File hash : hashes == null ? new File[0] : hashes) {
					File jar = new File(hash, "loader-" + version.getName() + ".jar");
					if (jar.isFile()) {
						return jar;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Those libraries from the player's machine: a launcher's libraries folder (Prism, the vanilla launcher) or the
	 * Gradle cache. Converted like libraries before the API (its classes use them: Codec, CommandDispatcher, ...).
	 */
	static java.util.List<File> libraries() {
		String appData = System.getenv("APPDATA");
		var roots = new java.util.ArrayList<File>();
		if (appData != null) {
			roots.add(new File(appData, "PrismLauncher/libraries"));
			roots.add(new File(appData, ".minecraft/libraries"));
		}
		var found = new java.util.ArrayList<File>();
		for (String[] library : LIBRARIES) {
			String file = library[1] + "-" + library[2] + ".jar";
			File jar = null;
			for (File root : roots) {
				File candidate = new File(root, library[0].replace('.', '/') + "/" + library[1] + "/" + library[2] + "/" + file);
				if (candidate.isFile()) {
					jar = candidate;
					break;
				}
			}
			if (jar == null) {
				File[] hashes = new File(System.getProperty("user.home"), ".gradle/caches/modules-2/files-2.1/" + library[0] + "/" + library[1]
						+ "/" + library[2]).listFiles();
				for (File hash : hashes == null ? new File[0] : hashes) {
					if (new File(hash, file).isFile()) {
						jar = new File(hash, file);
						break;
					}
				}
			}
			if (jar != null) {
				found.add(jar);
			} else {
				Conversion.LOGGER.warn("Minecraft 1.21.1 library {} not found: the classes using it won't run", file);
			}
		}
		return found;
	}

	private static final int ACCESS = xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PUBLIC | xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PRIVATE
			| xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PROTECTED;
	private static final int FINAL = xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_FINAL;

	/**
	 * What the access transformers of NeoForge and of the mods do, for all of the carried API at once: classes,
	 * methods and fields public and not final (mods extend classes that are final in vanilla, read private fields).
	 * Interface fields stay public static final; constructors are public too (the shadows build nested classes).
	 */
	private static byte[] open(byte[] bytes) {
		var reader = new xyz.wagyourtail.jvmdg.shade.asm.ClassReader(bytes);
		var writer = new xyz.wagyourtail.jvmdg.shade.asm.ClassWriter(0);
		reader.accept(new xyz.wagyourtail.jvmdg.shade.asm.ClassVisitor(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ASM9, writer) {
			private boolean isInterface;

			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				this.isInterface = (access & xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_INTERFACE) != 0;
				super.visit(version, (access & ~FINAL & ~ACCESS) | xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PUBLIC, name, signature, superName, interfaces);
			}

			@Override
			public void visitInnerClass(String name, String outerName, String innerName, int access) {
				super.visitInnerClass(name, outerName, innerName, (access & ~FINAL & ~ACCESS) | xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PUBLIC);
			}

			@Override
			public xyz.wagyourtail.jvmdg.shade.asm.FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				if (!this.isInterface) {
					access = (access & ~FINAL & ~ACCESS) | xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PUBLIC;
				}
				return super.visitField(access, name, descriptor, signature, value);
			}

			@Override
			public xyz.wagyourtail.jvmdg.shade.asm.MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				if (!name.startsWith("<")) {
					access = (access & ~FINAL & ~ACCESS) | xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PUBLIC;
				} else if ("<init>".equals(name)) {
					// public too: the shadows (another package) construct anonymous and nested classes
					access = (access & ~ACCESS) | xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PUBLIC;
				}
				return super.visitMethod(access, name, descriptor, signature, exceptions);
			}
		}, 0);
		return writer.toByteArray();
	}

	/** Package of the shadows: the static part of the 1.21.1 classes the tables map (see {@link #shadow}). */
	static final String SHADOW = "translator_api/";

	/**
	 * The shadow of a 1.21.1 class the tables map to a 1.12.2 / Translator class: its static methods (all of them, made
	 * public) and, when its static initializer only builds values, its static fields, in a class of its own. A static
	 * member the stand-in lacks (ByteBufCodecs.idMapper, Vec3.atLowerCornerOf, ResourceLocation.CODEC) is taken from
	 * here (MethodGuard): the 1.21.1 code, running on the 1.12.2 classes. No instances, so no instance members.
	 */
	static byte[] shadow(byte[] bytes) {
		return shadow(bytes, nested -> true);
	}

	/**
	 * {@code unavailable}: whether a nested class of the original isn't there for the shadow under its name. In a
	 * library gap those the game has aren't (the game's own Files$2 is another class); in the 1.21.1 API the nested classes the tables
	 * don't map are carried, opened (RecipeManager$1, what createCheck returns), and only those are kept out.
	 */
	static byte[] shadow(byte[] bytes, java.util.function.Predicate<String> unavailable) {
		return shadow(bytes, unavailable, false);
	}

	/**
	 * {@code defaults} (a library gap): the default methods of an interface become static bridges taking the receiver
	 * first. The game's older interface lacks the ones the newer library added (fastutil 8 Object2IntMap.getOrDefault,
	 * Long2ObjectMap.computeIfAbsent on the fastutil 7 of 1.12.2); MethodGuard sends those calls here. Their bodies call
	 * the interface's other methods on the receiver, as the default methods do.
	 */
	static byte[] shadow(byte[] bytes, java.util.function.Predicate<String> unavailable, boolean defaults) {
		var node = new xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode();
		new xyz.wagyourtail.jvmdg.shade.asm.ClassReader(bytes).accept(node, 0);
		String original = node.name;
		String shadow = SHADOW + original;
		// an enum's static initializer makes its constants: the shadow of one mapped to a 1.12.2 enum would construct
		// that enum's constants (Direction -> EnumFacing), so it keeps only the static methods
		// the shadow is in another package: the original's nested classes that aren't public (Files$2) are out of its
		// reach, so a method using one stays out of the shadow, and an initializer using one keeps the fields out too
		var hidden = new java.util.HashSet<String>();
		for (var inner : node.innerClasses) {
			if (inner.name.startsWith(original + "$") && (inner.access & xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PUBLIC) == 0
					&& unavailable.test(inner.name)) {
				hidden.add(inner.name);
			}
		}
		var unreachable = new java.util.HashSet<xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode>();
		if (!hidden.isEmpty()) {
			for (var m : node.methods) {
				if (usesAny(m, hidden)) {
					unreachable.add(m);
				}
			}
		}
		boolean pure = (node.access & xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_ENUM) == 0 && pureInitializer(node)
				&& unreachable.stream().noneMatch(m -> "<clinit>".equals(m.name));
		if (defaults && (node.access & xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_INTERFACE) != 0) {
			int skip = xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_STATIC | xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_ABSTRACT
					| xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PRIVATE | xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_BRIDGE
					| xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_SYNTHETIC;
			for (var m : node.methods) {
				// the receiver stays in local 0, so the code and its frames are the same; a super call (X.super.m())
				// can't be made from a static method
				if ((m.access & skip) == 0 && !m.name.startsWith("<") && !usesSuperCall(m)) {
					m.access |= xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_STATIC;
					m.desc = "(L" + original + ";" + m.desc.substring(1);
					m.signature = null;
				}
			}
		}
		var privateStatics = new java.util.HashSet<String>();
		var staticFields = new java.util.HashSet<String>();
		node.methods.removeIf(m -> (m.access & PUBLIC_STATIC_MASK) == 0 || "<clinit>".equals(m.name) && !pure || unreachable.contains(m));
		for (var m : node.methods) {
			if ((m.access & (xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PRIVATE | xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_SYNTHETIC)) != 0) {
				privateStatics.add(m.name + m.desc);
			}
			if (!"<clinit>".equals(m.name)) {
				m.access = (m.access & ~ACCESS & ~xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_ABSTRACT) | xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PUBLIC;
			}
		}
		node.fields.removeIf(f -> (f.access & PUBLIC_STATIC_MASK) == 0 || !pure);
		for (var f : node.fields) {
			f.access = (f.access & ~ACCESS & ~FINAL) | xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PUBLIC;
			staticFields.add(f.name + " " + f.desc);
		}
		// the shadow's own fields and private helpers (lambda bodies) are the shadow's; the rest of its calls go to the
		// stand-in first (one game state), then to a shadow when the stand-in lacks the member
		for (var m : node.methods) {
			for (var insn : m.instructions) {
				if (insn instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.FieldInsnNode field && original.equals(field.owner)
						&& staticFields.contains(field.name + " " + field.desc)) {
					field.owner = shadow;
				} else if (insn instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode call && original.equals(call.owner)
						&& call.getOpcode() == xyz.wagyourtail.jvmdg.shade.asm.Opcodes.INVOKESTATIC && privateStatics.contains(call.name + call.desc)) {
					call.owner = shadow;
					call.itf = false;
				} else if (insn instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.InvokeDynamicInsnNode indy) {
					for (int i = 0; i < indy.bsmArgs.length; i++) {
						if (indy.bsmArgs[i] instanceof xyz.wagyourtail.jvmdg.shade.asm.Handle handle && original.equals(handle.getOwner())
								&& handle.getTag() == xyz.wagyourtail.jvmdg.shade.asm.Opcodes.H_INVOKESTATIC
								&& privateStatics.contains(handle.getName() + handle.getDesc())) {
							indy.bsmArgs[i] = new xyz.wagyourtail.jvmdg.shade.asm.Handle(handle.getTag(), shadow, handle.getName(), handle.getDesc(), false);
						}
					}
				}
			}
		}
		for (var m : node.methods) {
			if ("<clinit>".equals(m.name)) {
				// the stand-in first (Class.forName of the class this name maps to): the shadow's fields use the stand-in's
				// classes, and a stand-in initialized from the middle of the shadow's initializer would see its own
				// nested classes half done (ShapedRecipePattern and its Data)
				var first = new xyz.wagyourtail.jvmdg.shade.asm.tree.InsnList();
				first.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.LdcInsnNode(xyz.wagyourtail.jvmdg.shade.asm.Type.getObjectType(original)));
				first.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.INVOKEVIRTUAL,
						"java/lang/Class", "getName", "()Ljava/lang/String;", false));
				first.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.INVOKESTATIC,
						"java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false));
				first.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.POP));
				m.instructions.insert(first);
				m.maxStack = Math.max(m.maxStack, 1);
			}
		}
		node.name = shadow;
		node.superName = "java/lang/Object";
		node.interfaces.clear();
		node.access = xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PUBLIC | xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_SUPER;
		node.signature = null;
		node.innerClasses.clear();
		node.outerClass = null;
		node.outerMethod = null;
		node.outerMethodDesc = null;
		node.nestHostClass = null;
		node.nestMembers = null;
		node.permittedSubclasses = null;
		node.recordComponents = null;
		node.visibleAnnotations = null;
		node.invisibleAnnotations = null;
		var writer = new xyz.wagyourtail.jvmdg.shade.asm.ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}

	private static final int PUBLIC_STATIC_MASK = xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_STATIC;

	private static boolean usesSuperCall(xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode method) {
		for (var insn : method.instructions) {
			if (insn instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode call
					&& call.getOpcode() == xyz.wagyourtail.jvmdg.shade.asm.Opcodes.INVOKESPECIAL && !"<init>".equals(call.name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A static initializer that only builds values (codecs, constants): one that registers something (Items, Blocks,
	 * the registries) would register it twice when the shadow is initialized.
	 */
	/** Whether a method's code names one of the classes (as an owner, a type, a constant or a handle). */
	private static boolean usesAny(xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode method, java.util.Set<String> classes) {
		for (var insn : method.instructions) {
			if (insn instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.TypeInsnNode type && classes.contains(type.desc)
					|| insn instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.FieldInsnNode field && (classes.contains(field.owner)
							|| classes.contains(xyz.wagyourtail.jvmdg.shade.asm.Type.getType(field.desc).getInternalName()))
					|| insn instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode call && classes.contains(call.owner)
					|| insn instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.LdcInsnNode ldc && ldc.cst instanceof xyz.wagyourtail.jvmdg.shade.asm.Type t
							&& classes.contains(t.getInternalName())) {
				return true;
			}
			if (insn instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.InvokeDynamicInsnNode indy) {
				for (Object arg : indy.bsmArgs) {
					if (arg instanceof xyz.wagyourtail.jvmdg.shade.asm.Handle handle && classes.contains(handle.getOwner())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean pureInitializer(xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode node) {
		for (var m : node.methods) {
			if (!"<clinit>".equals(m.name)) {
				continue;
			}
			for (var insn : m.instructions) {
				// reading registry keys (Registries.DIMENSION) is fine, calling into registries is not
				if (insn instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode call
						&& (call.name.toLowerCase(java.util.Locale.ROOT).contains("register") || isRegistry(call.owner))) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isRegistry(String owner) {
		return owner.equals("net/minecraft/core/Registry") || owner.equals("net/minecraft/core/registries/BuiltInRegistries")
				|| owner.startsWith("net/neoforged/neoforge/registries/") || owner.equals("net/minecraft/core/MappedRegistry");
	}


	/**
	 * The parts of a newer library the game has an older version of (fastutil 8 next to the fastutil 7 of 1.12.2, DFU 8
	 * next to the Translator's DFU 4): its classes the game lacks, as they are, and shadows of the static members of the
	 * classes it has. The game keeps its own classes (1.12.2 mods use them), a 1.21.1 mod gets the rest. Null when
	 * there is nothing to add. Made once per library and converter version.
	 */
	static File gap(File library, File cacheDir, TargetClasses target) throws IOException {
		cacheDir.mkdirs();
		File out = new File(cacheDir, library.getName().replaceAll("\\.jar$", "") + "-gap-" + JarConverter.VERSION + ".jar");
		if (out.isFile() && out.lastModified() >= library.lastModified()) {
			return out.length() > 22 ? out : null;
		}
		int added = 0;
		try (var zip = new ZipFile(library); var jar = new ZipOutputStream(new FileOutputStream(out))) {
			for (ZipEntry entry : Collections.list(zip.entries())) {
				String path = entry.getName();
				if (!path.endsWith(".class") || path.startsWith("META-INF/") || path.endsWith("module-info.class") || path.endsWith("package-info.class")) {
					continue;
				}
				byte[] bytes;
				try (InputStream in = zip.getInputStream(entry)) {
					bytes = ModJar.readAll(in);
				}
				boolean present = target.exists(path.substring(0, path.length() - 6));
				if (present && path.startsWith("com/mojang/")) {
					// DataFixerUpper: its anonymous and nested classes have the same names as those of the game's DFU 4
					// (Codec$9): a shadow would build the old ones. Only its new classes; DfuRedirects has the methods
					continue;
				}
				var copy = new ZipEntry(present ? SHADOW + path : path);
				copy.setTime(0);
				jar.putNextEntry(copy);
				// a nested class of the original the game lacks is added here under its name (opened), so the shadow's
				// methods may use it (fastutil 8 Object2LongMaps.fastIterable makes an Object2LongMaps$1); one the game has
				// is the game's own class
				jar.write(present ? shadow(bytes, target::exists, true) : open(bytes));
				jar.closeEntry();
				added++;
			}
		}
		Conversion.LOGGER.info("{}: the game has an older version, {} classes and shadows of it added", library.getName(), added);
		return added == 0 ? null : out;
	}

	/** The converted jar of the carried classes (cached by the API jar and the converter version). */
	static File prepare(File api, File cacheDir, RemapTable table, TargetClasses target, Set<URL> classpath) throws IOException {
		cacheDir.mkdirs();
		// a later start with the same API jar and converter takes the converted jar at once (no selection, no hashing)
		File marker = new File(cacheDir, "minecraft-1.21.1-api.key");
		File loader = loaderJar();
		String key = api.getAbsolutePath() + "|" + api.length() + "|" + api.lastModified() + "|" + JarConverter.VERSION
				+ (loader == null ? "" : "|" + loader.getAbsolutePath() + "|" + loader.length());
		if (marker.isFile()) {
			String[] saved = new String(java.nio.file.Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).split("\n");
			File cached = saved.length == 2 ? new File(cacheDir, saved[1]) : null;
			if (saved[0].equals(key) && cached != null && cached.isFile()) {
				return cached;
			}
		}
		File input = new File(cacheDir, "minecraft-1.21.1-api.jar");
		var probe = new ModRemapper(table, Collections.emptyMap(), target);
		int carried = 0;
		int shadows = 0;
		var written = new java.util.HashSet<String>();
		try (var out = new ZipOutputStream(new FileOutputStream(input))) {
			for (File source : loader == null ? new File[] { api } : new File[] { api, loader }) {
			try (var zip = new ZipFile(source)) {
			for (ZipEntry entry : Collections.list(zip.entries())) {
				String path = entry.getName();
				if (!path.endsWith(".class") || path.startsWith("META-INF/")) {
					continue;
				}
				String name = path.substring(0, path.length() - 6);
				// mapped by the tables (a 1.12.2 class, a Translator facade, the Forge class under the old package) or the
				// name of an existing 1.12.2 class: stays that class, its static members come along as a shadow
				if (!probe.map(name).equals(name) || target.exists(name) || !written.add(path)) {
					if (source == api && !probe.map(name).equals(name) && written.add(SHADOW + path)) {
						byte[] shadow;
						try (InputStream in = zip.getInputStream(entry)) {
							shadow = shadow(ModJar.readAll(in), nested -> !probe.map(nested).equals(nested) || target.exists(nested));
						}
						var copy = new ZipEntry(SHADOW + path);
						copy.setTime(0);
						out.putNextEntry(copy);
						out.write(shadow);
						out.closeEntry();
						shadows++;
					}
					continue;
				}
				// a fixed timestamp keeps the input (and its hash, the cache key) the same from run to run
				var copy = new ZipEntry(path);
				copy.setTime(0);
				out.putNextEntry(copy);
				try (InputStream in = zip.getInputStream(entry)) {
					out.write(open(ModJar.readAll(in)));
				}
				out.closeEntry();
				carried++;
			}
			}
			}
		}
		Conversion.LOGGER.info("Minecraft 1.21.1 API: {} classes carried, {} shadows of mapped classes, from {}", carried, shadows, api.getName());
		ModJar jar = ModJar.library(input, "Minecraft 1.21.1 API");
		File converted = JarConverter.convert(jar, cacheDir, table, target, classpath, new TreeMap<>());
		java.nio.file.Files.write(marker.toPath(), (key + "\n" + converted.getName()).getBytes(StandardCharsets.UTF_8));
		return converted;
	}
}
