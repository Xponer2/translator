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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import xyz.wagyourtail.jvmdg.shade.asm.AnnotationVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;

/**
 * A NeoForge 1.21.1 mod jar: its mods.toml entries, its classes (names and supertypes) and the classes NeoForge
 * starts from it ({@code @Mod}, {@code @EventBusSubscriber}).
 */
final class ModJar {
	static final String TOML = "META-INF/neoforge.mods.toml";

	final File file;
	final List<Map<String, String>> mods = new ArrayList<>();
	/** mod ids of the [[dependencies.*]] tables (what has to be converted and loaded first). */
	final List<String> dependencies = new ArrayList<>();
	/** the ones of them the mod can't run without on this side (type "required", or the older mandatory=true). */
	final List<String> requiredDependencies = new ArrayList<>();
	/** A jar without mods.toml: a library (the 1.21.1 API, a JarJar library); its name for the log. */
	private String libraryName;
	final Map<String, ClassInfo> classes = new HashMap<>();
	/** @Mod class -> mod id */
	final Map<String, String> modClasses = new LinkedHashMap<>();
	/**
	 * Every annotation of its classes, fields and methods: {annotation type, element type (TYPE, FIELD, METHOD), class,
	 * member, values}: NeoForge's scan data, which mods read to find their own annotated classes.
	 */
	final List<Object[]> annotations = new ArrayList<>();
	/** The mixin configs of its [[mixins]] tables (config = "x.mixins.json"). */
	final List<String> mixinConfigs = new ArrayList<>();
	/** @Mod class -> the sides it is for ({@code dist = Dist.CLIENT}); no entry: both */
	final Map<String, List<String>> modDists = new HashMap<>();
	/** @EventBusSubscriber class -> its attributes (modid, bus, value = sides) */
	final Map<String, Map<String, Object>> subscribers = new LinkedHashMap<>();

	private ModJar(File file) {
		this.file = file;
	}

	static boolean isModJar(File file) {
		try (var zip = new ZipFile(file)) {
			return zip.getEntry(TOML) != null;
		} catch (IOException e) {
			return false;
		}
	}

	static ModJar read(File file) throws IOException {
		return read(file, null);
	}

	/** A library jar (no mods.toml, no @Mod): converted and put on the class path like a mod, never started. */
	static ModJar library(File file, String name) throws IOException {
		return read(file, name);
	}

	private static ModJar read(File file, String libraryName) throws IOException {
		var jar = new ModJar(file);
		jar.libraryName = libraryName;
		try (var zip = new ZipFile(file)) {
			if (libraryName == null) {
				try (InputStream in = zip.getInputStream(zip.getEntry(TOML))) {
					jar.readToml(new String(readAll(in), StandardCharsets.UTF_8));
				}
			}
			for (ZipEntry entry : java.util.Collections.list(zip.entries())) {
				if (!entry.getName().endsWith(".class") || entry.getName().startsWith("META-INF/")) {
					continue;
				}
				try (InputStream in = zip.getInputStream(entry)) {
					jar.readClass(readAll(in));
				}
			}
		}
		return jar;
	}

	static byte[] readAll(InputStream in) throws IOException {
		var out = new java.io.ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int n;
		while ((n = in.read(buffer)) > 0) {
			out.write(buffer, 0, n);
		}
		return out.toByteArray();
	}

	/** The [[mods]] tables: key = "value" lines (enough for modId, version, displayName). */
	private void readToml(String text) {
		var dependencyTables = new ArrayList<Map<String, String>>();
		var mixinTables = new ArrayList<Map<String, String>>();
		Map<String, String> current = null;
		for (String raw : text.split("\n")) {
			String line = raw.trim();
			if (line.startsWith("[") && line.indexOf('#') > 0) {
				// a table header with a comment: [[mods]] #mandatory
				line = line.substring(0, line.indexOf('#')).trim();
			}
			if (line.startsWith("[[")) {
				current = "[[mods]]".equals(line) || "[[mixins]]".equals(line) || line.startsWith("[[dependencies.") ? new HashMap<>() : null;
				if ("[[mods]]".equals(line)) {
					this.mods.add(current);
				} else if ("[[mixins]]".equals(line)) {
					mixinTables.add(current);
				} else if (current != null) {
					dependencyTables.add(current);
				}
				continue;
			}
			int eq = line.indexOf('=');
			if (current == null || eq < 0 || line.startsWith("#")) {
				continue;
			}
			String value = line.substring(eq + 1).trim();
			if (value.startsWith("\"") && value.lastIndexOf('"') > 0) {
				value = value.substring(1, value.lastIndexOf('"'));
			} else if (value.startsWith("'") && value.lastIndexOf('\'') > 0) {
				// a TOML literal string: config='x.mixins.json'
				value = value.substring(1, value.lastIndexOf('\''));
			}
			current.put(line.substring(0, eq).trim(), value);
		}
		for (Map<String, String> mixins : mixinTables) {
			if (mixins.get("config") != null) {
				this.mixinConfigs.add(mixins.get("config"));
			}
		}
		for (Map<String, String> dependency : dependencyTables) {
			String id = dependency.get("modId");
			if (id != null && !"neoforge".equals(id) && !"minecraft".equals(id)) {
				this.dependencies.add(id);
				// NeoForge: type, "required" when left out; Forge's older mods.toml: mandatory
				String type = dependency.containsKey("type") ? dependency.get("type")
						: "false".equals(dependency.get("mandatory")) ? "optional" : "required";
				String side = dependency.getOrDefault("side", "BOTH").toUpperCase(java.util.Locale.ROOT);
				boolean thisSide = "BOTH".equals(side) || side.equals(net.minecraftforge.fml.relauncher.FMLLaunchHandler.side().name());
				if ("required".equalsIgnoreCase(type) && thisSide) {
					this.requiredDependencies.add(id);
				}
			}
		}
	}

	private void readClass(byte[] bytes) {
		ClassInfo info = ClassInfo.readWithCalls(bytes);
		this.classes.put(info.name, info);
		String className = info.name.replace('/', '.');
		new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
			@Override
			public xyz.wagyourtail.jvmdg.shade.asm.FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				return new xyz.wagyourtail.jvmdg.shade.asm.FieldVisitor(Opcodes.ASM9) {
					@Override
					public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
						return ModJar.this.record(annotation, "FIELD", className, name);
					}
				};
			}

			@Override
			public xyz.wagyourtail.jvmdg.shade.asm.MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				return new xyz.wagyourtail.jvmdg.shade.asm.MethodVisitor(Opcodes.ASM9) {
					@Override
					public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
						return ModJar.this.record(annotation, "METHOD", className, name + descriptor);
					}
				};
			}

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				AnnotationVisitor recorded = ModJar.this.record(descriptor, "TYPE", className, className);
				if ("Lnet/neoforged/fml/common/Mod;".equals(descriptor)) {
					return new AnnotationVisitor(Opcodes.ASM9) {
						@Override
						public void visit(String name, Object value) {
							if ("value".equals(name)) {
								ModJar.this.modClasses.put(info.name, (String) value);
							}
						}

						@Override
						public AnnotationVisitor visitArray(String name) {
							if (!"dist".equals(name)) {
								return null;
							}
							var dists = new ArrayList<String>();
							ModJar.this.modDists.put(info.name, dists);
							return new AnnotationVisitor(Opcodes.ASM9) {
								@Override
								public void visitEnum(String n, String descriptor, String value) {
									dists.add(value);
								}
							};
						}
					};
				}
				if ("Lnet/neoforged/fml/common/EventBusSubscriber;".equals(descriptor)) {
					var attributes = new HashMap<String, Object>();
					ModJar.this.subscribers.put(info.name, attributes);
					return new AnnotationVisitor(Opcodes.ASM9) {
						@Override
						public void visit(String name, Object value) {
							attributes.put(name, value);
						}

						@Override
						public void visitEnum(String name, String descriptor, String value) {
							attributes.put(name, value);
						}

						@Override
						public AnnotationVisitor visitArray(String name) {
							var values = new ArrayList<String>();
							attributes.put(name, values);
							return new AnnotationVisitor(Opcodes.ASM9) {
								@Override
								public void visitEnum(String n, String descriptor, String value) {
									values.add(value);
								}
							};
						}
					};
				}
				return recorded;
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}

	/** Records an annotation for the scan data; its values as they are (enums by name, arrays as lists). */
	private AnnotationVisitor record(String descriptor, String elementType, String className, String member) {
		var values = new HashMap<String, Object>();
		this.annotations.add(new Object[] { descriptor, elementType, className, member, values });
		return new AnnotationVisitor(Opcodes.ASM9) {
			@Override
			public void visit(String name, Object value) {
				values.put(name, value);
			}

			@Override
			public void visitEnum(String name, String enumDescriptor, String value) {
				values.put(name, new EnumValue(enumDescriptor, value));
			}

			@Override
			public AnnotationVisitor visitArray(String name) {
				var list = new ArrayList<Object>();
				values.put(name, list);
				return new AnnotationVisitor(Opcodes.ASM9) {
					@Override
					public void visit(String ignored, Object value) {
						list.add(value);
					}

					@Override
					public void visitEnum(String ignored, String enumDescriptor, String value) {
						list.add(new EnumValue(enumDescriptor, value));
					}
				};
			}
		};
	}

	/** An enum constant in an annotation of the scan data: NeoForge gives it as a ModAnnotation.EnumHolder. */
	static final class EnumValue {
		final String descriptor;
		final String value;

		EnumValue(String descriptor, String value) {
			this.descriptor = descriptor;
			this.value = value;
		}
	}

	boolean isLibrary() {
		return this.libraryName != null;
	}

	List<String> modIds() {
		var ids = new ArrayList<String>();
		for (Map<String, String> mod : this.mods) {
			if (mod.get("modId") != null) {
				ids.add(mod.get("modId"));
			}
		}
		return ids;
	}

	String displayName() {
		if (this.libraryName != null) {
			return this.libraryName;
		}
		return this.mods.isEmpty() ? this.file.getName() : this.mods.get(0).getOrDefault("displayName", this.mods.get(0).get("modId"));
	}
}
