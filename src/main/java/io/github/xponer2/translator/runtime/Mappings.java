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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.FieldVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.MethodVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.Type;
import xyz.wagyourtail.jvmdg.shade.asm.commons.ClassRemapper;
import xyz.wagyourtail.jvmdg.shade.asm.commons.Remapper;

import net.minecraft.launchwrapper.Launch;

/**
 * The release game (not the development environment): Minecraft's methods and fields are named by SRG (func_77658_a)
 * there, while the converter works with the MCP names its tables use (getTranslationKey). Here the game's classes are
 * read as the converter expects them (FML's deobfuscation, then SRG to MCP), and each converted jar is renamed back to
 * SRG, every member reference by the class that declares it (its supertypes followed, the targets of a mixin too), as
 * ForgeGradle's reobfuscation does for mods.
 * <p>
 * The SRG to MCP names are the MCP mappings (stable 39 for 1.12): not shipped with the Translator, they're taken once
 * from Forge's maven into {@code translator/mappings} of the game folder (or put there by hand).
 */
final class Mappings {
	static final String VERSION = "mcp_stable-39-1.12";
	static final String URL = "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_stable/39-1.12/" + VERSION + ".zip";

	private static volatile Mappings instance;
	private static volatile boolean checked;

	/** SRG name -> MCP name. */
	private final Map<String, String> methods = new HashMap<>();
	private final Map<String, String> fields = new HashMap<>();
	/** Class (internal name) -> its declared members "mcp desc" -> SRG name, recorded as the classes are read. */
	private final Map<String, Map<String, String>> methodSrg = new ConcurrentHashMap<>();
	private final Map<String, Map<String, String>> fieldSrg = new ConcurrentHashMap<>();

	private Mappings() {
	}

	/** The MCP name of an SRG field name (itself in development or when unknown): reflection in the release game. */
	static String mcpField(String srg) {
		Mappings mappings = instance();
		return mappings == null ? srg : mappings.fields.getOrDefault(srg, srg);
	}

	static String mcpMethod(String srg) {
		Mappings mappings = instance();
		return mappings == null ? srg : mappings.methods.getOrDefault(srg, srg);
	}

	/** The mappings of the release game; null in the development environment (the names are MCP already). */
	@Nullable
	static Mappings instance() {
		if (!checked) {
			synchronized (Mappings.class) {
				if (!checked) {
					if (io.github.xponer2.translator.util.FMLEnvironment.production) {
						instance = load(new File(Launch.minecraftHome != null ? Launch.minecraftHome : new File("."), "translator/mappings"));
					}
					checked = true;
				}
			}
		}
		return instance;
	}

	@Nullable
	private static Mappings load(File folder) {
		File zip = new File(folder, VERSION + ".zip");
		if (!zip.isFile()) {
			try {
				download(zip);
			} catch (IOException e) {
				Conversion.LOGGER.error("Translator: the MCP mappings ({}) are needed to run 1.21.1 mods in this game and couldn't be downloaded"
						+ " ({}): put {} into {}", VERSION, e.toString(), VERSION + ".zip", folder.getAbsolutePath());
				return null;
			}
		}
		var mappings = new Mappings();
		try (var file = new ZipFile(zip)) {
			mappings.read(file, "methods.csv", mappings.methods);
			mappings.read(file, "fields.csv", mappings.fields);
		} catch (IOException e) {
			Conversion.LOGGER.error("Translator: can't read the MCP mappings {}: {}", zip, e.toString());
			return null;
		}
		Conversion.LOGGER.info("Translator: release game, {} methods and {} fields of the MCP mappings", mappings.methods.size(), mappings.fields.size());
		return mappings;
	}

	private static void download(File zip) throws IOException {
		zip.getParentFile().mkdirs();
		File part = new File(zip.getPath() + ".part");
		var connection = (java.net.HttpURLConnection) new java.net.URL(URL).openConnection();
		connection.setConnectTimeout(15000);
		connection.setReadTimeout(30000);
		connection.setRequestProperty("User-Agent", "Translator");
		try (InputStream in = connection.getInputStream(); var out = new FileOutputStream(part)) {
			byte[] buffer = new byte[8192];
			for (int n; (n = in.read(buffer)) > 0; ) {
				out.write(buffer, 0, n);
			}
		}
		if (!part.renameTo(zip)) {
			throw new IOException("can't move " + part + " to " + zip);
		}
		Conversion.LOGGER.info("Translator: MCP mappings downloaded from {}", URL);
	}

	private void read(ZipFile zip, String name, Map<String, String> into) throws IOException {
		ZipEntry entry = zip.getEntry(name);
		if (entry == null) {
			throw new IOException("no " + name);
		}
		try (var reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
			reader.readLine();
			for (String line; (line = reader.readLine()) != null; ) {
				int first = line.indexOf(',');
				int second = first < 0 ? -1 : line.indexOf(',', first + 1);
				if (second > first) {
					into.put(line.substring(0, first), line.substring(first + 1, second));
				}
			}
		}
	}

	// --- reading the game's classes ---------------------------------------------------------------------------------------

	/**
	 * Class bytes as the converter reads them: a Minecraft class by its MCP name (the game jar has it under its
	 * obfuscated name: FML's deobfuscation gives the SRG members), every class with SRG members named by MCP.
	 */
	TargetClasses.ClassBytes deobfuscating(TargetClasses.ClassBytes raw) {
		return binaryName -> {
			byte[] bytes = raw.get(binaryName);
			String internal = binaryName.replace('.', '/');
			if (bytes == null && internal.startsWith("net/minecraft/")) {
				// only Minecraft's own classes are obfuscated in the game jar
				try {
					String obfuscated = net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper.INSTANCE.unmap(internal);
					if (obfuscated != null && !obfuscated.equals(internal)) {
						byte[] original = raw.get(obfuscated.replace('/', '.'));
						if (original != null) {
							// Forge's binary patches first (the vanilla class lacks everything Forge adds: its constructors,
							// methods, interfaces, the IForgeRegistryEntry superclass), then FML's deobfuscation; one
							// thread at a time: the patcher reuses its buffers and FML's remapper fills its maps lazily,
							// and the converter reads classes from all cores (a class read wrong or not at all failed every
							// method naming it)
							synchronized (Deobfuscation.LOCK) {
								byte[] patched = net.minecraftforge.fml.common.patcher.ClassPatchManager.INSTANCE.applyPatch(obfuscated.replace('/', '.'),
										binaryName, original);
								bytes = Deobfuscation.TRANSFORMER.transform(obfuscated.replace('/', '.'), binaryName, patched);
							}
						}
					}
				} catch (LinkageError e) {
					// no FML deobfuscation here (outside the game): the class isn't there
				} catch (RuntimeException e) {
					// the class is there and its patching or deobfuscation threw: a failed read (tried again), not a
					// missing class
					throw new IOException("reading " + binaryName, e);
				}
			}
			return bytes == null ? null : this.toMcp(bytes);
		};
	}

	/** FML's deobfuscation, made the first time an obfuscated class is read. */
	private static final class Deobfuscation {
		static final Object LOCK = new Object();

		static final net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer TRANSFORMER =
				new net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer();
	}

	/** SRG members named by MCP; the class's own members recorded for the way back. */
	private byte[] toMcp(byte[] bytes) {
		var reader = new ClassReader(bytes);
		String owner = reader.getClassName();
		var writer = new ClassWriter(0);
		var remapper = new Remapper() {
			@Override
			public String mapMethodName(String methodOwner, String name, String descriptor) {
				return name.startsWith("func_") ? Mappings.this.methods.getOrDefault(name, name) : name;
			}

			@Override
			public String mapInvokeDynamicMethodName(String name, String descriptor) {
				return name.startsWith("func_") ? Mappings.this.methods.getOrDefault(name, name) : name;
			}

			@Override
			public String mapFieldName(String fieldOwner, String name, String descriptor) {
				return name.startsWith("field_") ? Mappings.this.fields.getOrDefault(name, name) : name;
			}
		};
		reader.accept(new ClassRemapper(writer, remapper) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				if (name.startsWith("func_")) {
					String mcp = Mappings.this.methods.get(name);
					if (mcp != null) {
						Mappings.this.methodSrg.computeIfAbsent(owner, k -> new ConcurrentHashMap<>()).put(mcp + " " + descriptor, name);
					}
				}
				return super.visitMethod(access, name, descriptor, signature, exceptions);
			}

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				if (name.startsWith("field_")) {
					String mcp = Mappings.this.fields.get(name);
					if (mcp != null) {
						Mappings.this.fieldSrg.computeIfAbsent(owner, k -> new ConcurrentHashMap<>()).put(mcp, name);
					}
				}
				return super.visitField(access, name, descriptor, signature, value);
			}
		}, 0);
		return writer.toByteArray();
	}

	// --- the converted jar back to SRG -------------------------------------------------------------------------------------

	/** The converted jar with its references to the game's members (and their overrides) named by SRG. */
	void reobfuscate(File jar, TargetClasses target) throws IOException {
		var classes = new HashMap<String, ClassInfo>();
		var entries = new java.util.LinkedHashMap<String, byte[]>();
		try (var zip = new ZipFile(jar)) {
			for (ZipEntry entry : Collections.list(zip.entries())) {
				if (entry.isDirectory()) {
					continue;
				}
				byte[] bytes;
				try (InputStream in = zip.getInputStream(entry)) {
					bytes = ModJar.readAll(in);
				}
				entries.put(entry.getName(), bytes);
				if (entry.getName().endsWith(".class") && !entry.getName().startsWith("META-INF/")) {
					try {
						ClassInfo info = ClassInfo.read(bytes);
						classes.put(new ClassReader(bytes).getClassName(), info);
					} catch (RuntimeException e) {
						// left as it is
					}
				}
			}
		}
		var remapper = new Remapper() {
			@Override
			public String mapMethodName(String owner, String name, String descriptor) {
				if (name.startsWith("<") || owner.startsWith("[")) {
					return name;
				}
				String srg = Mappings.this.find(owner, name + " " + descriptor, true, classes, target);
				return srg != null ? srg : name;
			}

			@Override
			public String mapFieldName(String owner, String name, String descriptor) {
				String srg = Mappings.this.find(owner, name, false, classes, target);
				return srg != null ? srg : name;
			}

			@Override
			public String mapInvokeDynamicMethodName(String name, String descriptor) {
				// the functional interface's method (a lambda of a Minecraft interface): by its name there
				Type returned = Type.getReturnType(descriptor);
				if (returned.getSort() != Type.OBJECT) {
					return name;
				}
				Map<String, String> declared = Mappings.this.declaredMethods(returned.getInternalName(), target);
				String found = null;
				for (var entry : declared.entrySet()) {
					if (entry.getKey().startsWith(name + " ")) {
						if (found != null && !found.equals(entry.getValue())) {
							return name;
						}
						found = entry.getValue();
					}
				}
				return found != null ? found : name;
			}
		};
		File temp = new File(jar.getPath() + ".srg");
		try (var out = new ZipOutputStream(new FileOutputStream(temp))) {
			for (var entry : entries.entrySet()) {
				byte[] bytes = entry.getValue();
				if (entry.getKey().endsWith(".class") && !entry.getKey().startsWith("META-INF/")) {
					try {
						var reader = new ClassReader(bytes);
						ClassInfo info = classes.get(reader.getClassName());
						var writer = new ClassWriter(0);
						if (info != null && !info.mixinTargets.isEmpty()) {
							// a mixin: its annotations name the targets' members in strings (method selectors, @At targets)
							var node = new xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode();
							reader.accept(new ClassRemapper(node, remapper), 0);
							this.remapMixinStrings(node, info.mixinTargets, classes, target);
							node.accept(writer);
						} else {
							reader.accept(new ClassRemapper(writer, remapper), 0);
						}
						bytes = writer.toByteArray();
					} catch (RuntimeException e) {
						Conversion.LOGGER.warn("Translator: {} of {} keeps its MCP names: {}", entry.getKey(), jar.getName(), e.toString());
					}
				}
				var copy = new ZipEntry(entry.getKey());
				copy.setTime(0);
				out.putNextEntry(copy);
				out.write(bytes);
				out.closeEntry();
			}
		}
		if (!jar.delete() || !temp.renameTo(jar)) {
			throw new IOException("can't replace " + jar);
		}
	}

	/** The SRG name of a member by the class that declares it: the owner, its supertypes, a mixin's targets. */
	@Nullable
	private String find(String owner, String key, boolean method, Map<String, ClassInfo> classes, TargetClasses target) {
		var todo = new ArrayDeque<String>();
		var seen = new HashSet<String>();
		todo.add(owner);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			if (current == null || !seen.add(current)) {
				continue;
			}
			ClassInfo info = classes.get(current);
			if (info == null) {
				info = target.get(current);
				Map<String, String> declared = (method ? this.methodSrg : this.fieldSrg).get(current);
				if (declared != null && declared.containsKey(key)) {
					return declared.get(key);
				}
			}
			if (info == null) {
				continue;
			}
			todo.addAll(info.supertypes());
			todo.addAll(info.mixinTargets);
		}
		return null;
	}

	// --- mixins ------------------------------------------------------------------------------------------------------------

	private static final String AT = "Lorg/spongepowered/asm/mixin/injection/At;";
	private static final String ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
	private static final String INVOKER = "Lorg/spongepowered/asm/mixin/gen/Invoker;";

	/** The strings of a mixin's annotations that name members of the targets: by SRG (Mixin reads them as they are). */
	private void remapMixinStrings(xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode node, java.util.List<String> targets, Map<String, ClassInfo> classes,
			TargetClasses target) {
		for (var method : node.methods) {
			this.remapAnnotations(method.visibleAnnotations, targets, classes, target);
			this.remapAnnotations(method.invisibleAnnotations, targets, classes, target);
		}
		for (var field : node.fields) {
			this.remapAnnotations(field.visibleAnnotations, targets, classes, target);
			this.remapAnnotations(field.invisibleAnnotations, targets, classes, target);
		}
	}

	private void remapAnnotations(@Nullable java.util.List<xyz.wagyourtail.jvmdg.shade.asm.tree.AnnotationNode> annotations, java.util.List<String> targets,
			Map<String, ClassInfo> classes, TargetClasses target) {
		if (annotations == null) {
			return;
		}
		for (var annotation : annotations) {
			this.remapAnnotation(annotation, targets, classes, target);
		}
	}

	private void remapAnnotation(xyz.wagyourtail.jvmdg.shade.asm.tree.AnnotationNode annotation, java.util.List<String> targets, Map<String, ClassInfo> classes,
			TargetClasses target) {
		if (annotation.values == null) {
			return;
		}
		for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
			String key = (String) annotation.values.get(i);
			Object value = annotation.values.get(i + 1);
			if (value instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.AnnotationNode nested) {
				this.remapAnnotation(nested, targets, classes, target);
			} else if (value instanceof java.util.List<?> list) {
				var mapped = new java.util.ArrayList<Object>(list.size());
				for (Object element : list) {
					if (element instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.AnnotationNode nested) {
						this.remapAnnotation(nested, targets, classes, target);
						mapped.add(nested);
					} else if (element instanceof String text && "method".equals(key)) {
						mapped.add(this.selector(text, targets, classes, target));
					} else {
						mapped.add(element);
					}
				}
				annotation.values.set(i + 1, mapped);
			} else if (value instanceof String text) {
				if ("method".equals(key)) {
					annotation.values.set(i + 1, this.selector(text, targets, classes, target));
				} else if ("target".equals(key) && AT.equals(annotation.desc)) {
					annotation.values.set(i + 1, this.reference(text, classes, target));
				} else if ("value".equals(key) && (ACCESSOR.equals(annotation.desc) || INVOKER.equals(annotation.desc)) && !text.startsWith("<")) {
					String srg = ACCESSOR.equals(annotation.desc) ? this.firstOf(targets, text, false, classes, target)
							: this.byName(targets, text, classes, target);
					if (srg != null) {
						annotation.values.set(i + 1, srg);
					}
				}
			}
		}
	}

	/** A method selector of the targets: "name", "name(desc)ret" or "Lowner;name(desc)ret". */
	private String selector(String text, java.util.List<String> targets, Map<String, ClassInfo> classes, TargetClasses target) {
		if (text.startsWith("L") && text.contains(";")) {
			return this.reference(text, classes, target);
		}
		int paren = text.indexOf('(');
		String name = paren < 0 ? text : text.substring(0, paren);
		if (name.isEmpty() || name.startsWith("<") || name.contains("*")) {
			return text;
		}
		String srg = paren < 0 ? this.byName(targets, name, classes, target) : this.firstOf(targets, name + " " + text.substring(paren), true, classes, target);
		return srg == null ? text : srg + (paren < 0 ? "" : text.substring(paren));
	}

	/** An @At target: "Lowner;name(desc)ret" or "Lowner;name:desc". */
	private String reference(String text, Map<String, ClassInfo> classes, TargetClasses target) {
		int semicolon = text.indexOf(';');
		if (!text.startsWith("L") || semicolon < 0) {
			return text;
		}
		String owner = text.substring(1, semicolon);
		String rest = text.substring(semicolon + 1);
		int paren = rest.indexOf('(');
		int colon = rest.indexOf(':');
		String srg;
		if (paren > 0) {
			srg = this.find(owner, rest.substring(0, paren) + " " + rest.substring(paren), true, classes, target);
			return srg == null ? text : "L" + owner + ";" + srg + rest.substring(paren);
		}
		if (colon > 0) {
			srg = this.find(owner, rest.substring(0, colon), false, classes, target);
			return srg == null ? text : "L" + owner + ";" + srg + rest.substring(colon);
		}
		return text;
	}

	@Nullable
	private String firstOf(java.util.List<String> owners, String key, boolean method, Map<String, ClassInfo> classes, TargetClasses target) {
		for (String owner : owners) {
			String srg = this.find(owner, key, method, classes, target);
			if (srg != null) {
				return srg;
			}
		}
		return null;
	}

	/** A method by its name only (a selector without descriptor): its SRG name when all its overloads share one. */
	@Nullable
	private String byName(java.util.List<String> owners, String name, Map<String, ClassInfo> classes, TargetClasses target) {
		String found = null;
		var todo = new ArrayDeque<String>(owners);
		var seen = new HashSet<String>();
		while (!todo.isEmpty()) {
			String current = todo.poll();
			if (!seen.add(current)) {
				continue;
			}
			ClassInfo info = classes.get(current);
			if (info == null) {
				info = target.get(current);
				Map<String, String> declared = this.methodSrg.get(current);
				if (declared != null) {
					for (var entry : declared.entrySet()) {
						if (entry.getKey().startsWith(name + " ")) {
							if (found != null && !found.equals(entry.getValue())) {
								return found;
							}
							found = entry.getValue();
						}
					}
				}
			}
			if (info != null) {
				todo.addAll(info.supertypes());
			}
		}
		return found;
	}

	private Map<String, String> declaredMethods(String owner, TargetClasses target) {
		target.get(owner);
		Map<String, String> declared = this.methodSrg.get(owner);
		return declared != null ? declared : Collections.emptyMap();
	}
}
