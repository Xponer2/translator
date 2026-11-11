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
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.compile.ApiShader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;

/**
 * Turns a NeoForge 1.21.1 jar into one the 1.12.2 game can load: names remapped (ModRemapper), bytecode downgraded
 * from Java 21 to Java 8 with the Java 9+ API it uses shaded in (JvmDowngrader). The result is cached by the hash of
 * the jar and of this converter's version, so it runs once per mod version.
 */
final class JarConverter {
	/** Raise when the conversion changes: older cached jars are converted again. */
	static final String VERSION = "182";
	private static final String SHADE_PREFIX = "translator_jvmdg/";
	private static final Flags FLAGS = new Flags();
	static {
		FLAGS.classVersion = 52;
		FLAGS.logAnsiColors = false;
		FLAGS.quiet = true;
	}
	/**
	 * One downgrader for all the jars: making one loads the stubs of every Java version (a second or more), and with
	 * one per jar the jars had to downgrade one at a time.
	 */
	private static ClassDowngrader downgrader;

	private JarConverter() {
	}

	static File convert(ModJar jar, File cacheDir, RemapTable table, TargetClasses target, Set<URL> classpath,
			Map<String, Integer> missingClasses) throws IOException {
		String name = jar.file.getName().replaceAll("\\.jar$", "");
		File out = new File(cacheDir, name + "-" + hash(jar.file) + ".jar");
		if (out.isFile()) {
			return out;
		}
		cacheDir.mkdirs();
		File remapped = new File(cacheDir, name + ".remapped.tmp");
		File downgraded = new File(cacheDir, name + ".downgraded.tmp");
		File shaded = new File(cacheDir, name + ".shaded.tmp");
		try {
			long t0 = System.currentTimeMillis();
			var remapper = new ModRemapper(table, jar.classes, target);
			remap(jar, remapped, remapper, table, target);
			missingClasses.putAll(remapper.missingClasses);
			long t1 = System.currentTimeMillis();

			downgrade(remapped, downgraded, jar.displayName(), target, new File(cacheDir, name + DOWNGRADE_CACHE_SUFFIX));
			long t2 = System.currentTimeMillis();
			// the Java 9+ API stubs go under a prefix of this jar: jars shade different subsets of the same stub classes,
			// and with one shared name the first one loaded would hide the methods of the others
			String prefix = SHADE_PREFIX + out.getName().replaceAll("\\.jar$", "").replaceAll("[^A-Za-z0-9_]", "_") + "/";
			shade(downgraded, shaded, prefix);
			long t3 = System.currentTimeMillis();

			var guard = new MethodGuard(target, table.redirects, table::namesOf);
			guard.constants = table.constants;
			guard.forced = table.forced;
			var facades = new java.util.HashMap<String, String>();
			table.supers.forEach((modern, facade) -> facades.putIfAbsent(table.classes.getOrDefault(modern, modern), facade));
			guard.facades = facades;
			guard.guard(shaded, out);
			// the release game names Minecraft's members by SRG: the jar is renamed so (no-op in development)
			Mappings mappings = Mappings.instance();
			if (mappings != null) {
				mappings.reobfuscate(out, target);
			}
			Conversion.LOGGER.info("{}: remap {} ms, downgrade {} ms, shade {} ms, guard {} ms", jar.displayName(), t1 - t0, t2 - t1, t3 - t2,
					System.currentTimeMillis() - t3);
			if (guard.bridged.get() > 0) {
				Conversion.LOGGER.info("{}: {} overrides and lambdas bridged (their types changed with the 1.12.2 names)", jar.displayName(), guard.bridged.get());
			}
			if (guard.softened.get() > 0) {
				Conversion.LOGGER.info("{}: {} methods run without their missing parts (null / 0 there)", jar.displayName(), guard.softened.get());
			}
			if (!guard.stubbed.isEmpty()) {
				var report = new java.util.ArrayList<>(guard.stubbed);
				Collections.sort(report);
				java.nio.file.Files.write(new File(cacheDir, out.getName().replaceAll("\\.jar$", ".stubbed.txt")).toPath(), report);
				Conversion.LOGGER.warn("{}: {} methods use what the Translator doesn't have yet: they do nothing or fall back to the 1.12.2 behaviour (list: {})",
						jar.displayName(), guard.stubbed.size(), out.getName().replaceAll("\\.jar$", ".stubbed.txt"));
			}
		} finally {
			remapped.delete();
			downgraded.delete();
			shaded.delete();
		}
		return out;
	}

	/** Each class -> the SHA-1 of its family's remapped bytes (the top-level class and its nested classes, by name). */
	private static Map<String, String> familyHashes(Map<String, byte[]> classes) {
		var families = new java.util.TreeMap<String, java.util.List<String>>();
		for (String className : classes.keySet()) {
			int slash = className.lastIndexOf('/');
			int dollar = className.indexOf('$', slash + 1);
			families.computeIfAbsent(dollar < 0 ? className : className.substring(0, dollar), k -> new java.util.ArrayList<>()).add(className);
		}
		var hashes = new java.util.HashMap<String, String>();
		for (var members : families.values()) {
			Collections.sort(members);
			try {
				var digest = MessageDigest.getInstance("SHA-1");
				for (String member : members) {
					digest.update(member.getBytes(java.nio.charset.StandardCharsets.UTF_8));
					digest.update(classes.get(member));
				}
				var hex = new StringBuilder();
				for (byte b : digest.digest()) {
					hex.append(String.format("%02x", b));
				}
				for (String member : members) {
					hashes.put(member, hex.toString());
				}
			} catch (java.security.NoSuchAlgorithmException e) {
				throw new IllegalStateException(e);
			}
		}
		return hashes;
	}

	/** "family hash/class" -> the classes its downgrade made, from the cache zip (entries family/class/made.class). */
	private static Map<String, Map<String, byte[]>> readDowngradeCache(File cacheFile) {
		var cached = new java.util.HashMap<String, Map<String, byte[]>>();
		if (!cacheFile.isFile()) {
			return cached;
		}
		try (var zip = new ZipFile(cacheFile)) {
			for (ZipEntry entry : Collections.list(zip.entries())) {
				String path = entry.getName();
				int first = path.indexOf("//");
				if (entry.isDirectory() || first < 0 || !path.endsWith(".class")) {
					continue;
				}
				String key = path.substring(0, first);
				String made = path.substring(first + 2, path.length() - 6);
				try (InputStream in = zip.getInputStream(entry)) {
					cached.computeIfAbsent(key, k -> new java.util.LinkedHashMap<>()).put(made, ModJar.readAll(in));
				}
			}
		} catch (IOException | RuntimeException e) {
			// a broken cache only costs the time it would have saved
			Conversion.LOGGER.warn("Can't read the downgrade cache {}: {}", cacheFile.getName(), e.toString());
			cached.clear();
		}
		return cached;
	}

	private static void writeDowngradeCache(File cacheFile, Map<String, Map<String, byte[]>> current) {
		File temp = new File(cacheFile.getPath() + ".tmp");
		try (var out = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(temp))) {
			out.setLevel(java.util.zip.Deflater.BEST_SPEED);
			for (var entry : current.entrySet()) {
				for (var made : entry.getValue().entrySet()) {
					out.putNextEntry(new ZipEntry(entry.getKey() + "//" + made.getKey() + ".class"));
					out.write(made.getValue());
					out.closeEntry();
				}
			}
		} catch (IOException e) {
			Conversion.LOGGER.warn("Can't write the downgrade cache {}: {}", cacheFile.getName(), e.toString());
			temp.delete();
			return;
		}
		cacheFile.delete();
		if (!temp.renameTo(cacheFile)) {
			temp.delete();
		}
	}

	private static synchronized ClassDowngrader downgrader() {
		if (downgrader == null) {
			downgrader = ClassDowngrader.downgradeTo(FLAGS);
		}
		return downgrader;
	}

	/**
	 * The Java 9+ API stubs a jar uses, copied into it under its prefix. Each jar scans the downgraded Java API again:
	 * shading strips the scanned stub classes down to what that jar uses, so a scan can't be shared. Jars shade in
	 * parallel (each opens its own zip file systems).
	 */
	private static void shade(File input, File output, String prefix) throws IOException {
		// what ApiShader.shadeApis(flags, prefix, input, output, null) does, with the downgraded API found once: given
		// as files, it would be downgraded again for every jar (its jar doesn't say it is downgraded)
		var open = new java.util.ArrayList<java.nio.file.FileSystem>();
		try {
			var roots = new java.util.ArrayList<java.nio.file.Path>();
			for (var api : downgradedApi()) {
				var fs = xyz.wagyourtail.jvmdg.util.Utils.openZipFileSystem(api, false);
				open.add(fs);
				roots.add(fs.getPath("/"));
			}
			var scanned = ApiShader.scanApis(FLAGS, roots);
			output.delete();
			var in = xyz.wagyourtail.jvmdg.util.Utils.openZipFileSystem(input.toPath(), false);
			open.add(in);
			var out = xyz.wagyourtail.jvmdg.util.Utils.openZipFileSystem(output.toPath(), true);
			open.add(out);
			ApiShader.shadeApis(FLAGS, prefix, in.getPath("/"), out.getPath("/"), roots, scanned.getFirst(), scanned.getSecond());
		} finally {
			for (int i = open.size() - 1; i >= 0; i--) {
				open.get(i).close();
			}
		}
	}

	private static Set<java.nio.file.Path> downgradedApi;

	/**
	 * The downgrader (its stubs of every Java version) and the class caches let go once every jar is converted: they
	 * held about a gigabyte for the rest of the game.
	 */
	static synchronized void release() {
		downgrader = null;
		downgradedApi = null;
		JDK_DECLARATIONS.clear();
	}

	/** JvmDowngrader's Java API downgraded to Java 8, made once (several jars making it at once would collide). */
	private static synchronized Set<java.nio.file.Path> downgradedApi() throws IOException {
		if (downgradedApi == null) {
			downgradedApi = ApiShader.resolveDowngradedApi(FLAGS, null);
		}
		return downgradedApi;
	}

	/**
	 * Java 21 bytecode to Java 8, class by class in memory (what JvmDowngrader's ZipDowngrader does, without opening
	 * the whole class path again for every jar): the classes a downgrade looks up (supertypes, for frames and stubs)
	 * come from the jar itself, then the game's class path. Files under META-INF (multi-release versions) stay as
	 * they are; Java 8 doesn't read them.
	 */
	/**
	 * The downgraded classes of this mod's last conversion, kept across converter versions: the downgrade is most of a
	 * conversion (about 60%), and a new converter version remaps most classes the same way. Keyed by the SHA-1 of the
	 * remapped bytes of the class's family (its top-level class and nested classes: a downgrade reads its nest-mates),
	 * so a class is downgraded again only when it or a nest-mate changed. The suffix names the format: change it with
	 * JvmDowngrader's version or FLAGS.
	 */
	private static final String DOWNGRADE_CACHE_SUFFIX = ".downgrade-cache-1.zip";

	private static void downgrade(File input, File output, String name, TargetClasses target, File cacheFile) throws IOException {
		var classes = new java.util.LinkedHashMap<String, byte[]>();
		var resources = new java.util.LinkedHashMap<String, byte[]>();
		try (var zip = new ZipFile(input)) {
			for (ZipEntry entry : Collections.list(zip.entries())) {
				if (entry.isDirectory()) {
					continue;
				}
				byte[] bytes;
				try (InputStream in = zip.getInputStream(entry)) {
					bytes = ModJar.readAll(in);
				}
				String path = entry.getName();
				if (path.endsWith(".class") && !path.startsWith("META-INF/")) {
					classes.put(path.substring(0, path.length() - 6), bytes);
				} else {
					resources.put(path, bytes);
				}
			}
		}
		ClassDowngrader downgrader = downgrader();
		xyz.wagyourtail.jvmdg.util.Function<String, byte[]> lookup = className -> {
			// the jar's own classes whole: a downgrade reads its nest-mates' code (which private members they reach)
			byte[] own = classes.get(className);
			if (own != null) {
				return own;
			}
			byte[] bytes = target.declarations(className);
			if (bytes != null) {
				return bytes;
			}
			return JDK_DECLARATIONS.computeIfAbsent(className, JarConverter::jdkDeclarations).orElse(null);
		};
		var downgraded = new java.util.concurrent.ConcurrentHashMap<String, byte[]>();
		var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
		Map<String, String> families = familyHashes(classes);
		Map<String, Map<String, byte[]>> previous = readDowngradeCache(cacheFile);
		var current = new java.util.concurrent.ConcurrentHashMap<String, Map<String, byte[]>>();
		classes.entrySet().parallelStream().forEach(entry -> {
			String key = families.get(entry.getKey()) + "/" + entry.getKey();
			Map<String, byte[]> reused = previous.get(key);
			if (reused != null) {
				downgraded.putAll(reused);
				current.put(key, reused);
				return;
			}
			try {
				Map<String, byte[]> result = downgrader.downgrade(new java.util.concurrent.atomic.AtomicReference<>(entry.getKey()), entry.getValue(),
						false, lookup);
				if (result == null) {
					result = Collections.singletonMap(entry.getKey(), entry.getValue());
				}
				downgraded.putAll(result);
				current.put(key, result);
			} catch (Throwable t) {
				// a class the downgrader can't handle stays as it is (MethodGuard reports it if it doesn't verify)
				failure.compareAndSet(null, t);
				downgraded.put(entry.getKey(), entry.getValue());
			}
		});
		if (failure.get() != null) {
			Conversion.LOGGER.warn("{}: some classes couldn't be downgraded, e.g. {}", name, failure.get().toString());
		}
		int reusedCount = 0;
		for (String key : current.keySet()) {
			if (previous.containsKey(key)) {
				reusedCount++;
			}
		}
		if (reusedCount > 0) {
			Conversion.LOGGER.info("{}: {} of {} classes' downgrade reused from the last conversion", name, reusedCount, classes.size());
		}
		writeDowngradeCache(cacheFile, current);
		byte[] manifest = resources.get("META-INF/MANIFEST.MF");
		if (manifest != null) {
			// marked like JvmDowngrader marks what it downgrades
			var parsed = new java.util.jar.Manifest(new java.io.ByteArrayInputStream(manifest));
			parsed.getMainAttributes().putValue("JvmDowngrader-Downgrade-Target", Integer.toString(FLAGS.classVersion));
			var bytes = new java.io.ByteArrayOutputStream();
			parsed.write(bytes);
			resources.put("META-INF/MANIFEST.MF", bytes.toByteArray());
		}
		try (var out = new ZipOutputStream(new FileOutputStream(output))) {
			for (var entry : resources.entrySet()) {
				out.putNextEntry(new ZipEntry(entry.getKey()));
				out.write(entry.getValue());
				out.closeEntry();
			}
			for (var entry : new java.util.TreeMap<>(downgraded).entrySet()) {
				out.putNextEntry(new ZipEntry(entry.getKey() + ".class"));
				out.write(entry.getValue());
				out.closeEntry();
			}
		}
	}

	private static final Map<String, java.util.Optional<byte[]>> JDK_DECLARATIONS = new java.util.concurrent.ConcurrentHashMap<>();

	/** A class of the running Java (java.lang.Object), without its code: the downgrader's lookups ask for them too. */
	private static java.util.Optional<byte[]> jdkDeclarations(String className) {
		try (InputStream in = ClassLoader.getSystemResourceAsStream(className + ".class")) {
			return in == null ? java.util.Optional.empty() : java.util.Optional.of(TargetClasses.stripCode(ModJar.readAll(in)));
		} catch (IOException | RuntimeException e) {
			return java.util.Optional.empty();
		}
	}

	private static void remap(ModJar jar, File output, ModRemapper remapper, RemapTable table, TargetClasses target) throws IOException {
		try (var zip = new ZipFile(jar.file); var out = new ZipOutputStream(new FileOutputStream(output))) {
			// accessor mixins into Translator classes: by reflection (they can't be applied there)
			var accessors = AccessorMixins.find(jar, zip, remapper, MIXABLE::contains, target);
			// a jar may hold the same path twice (Ars Nouveau: two META-INF/LICENSE.txt): the first one is kept, as a
			// class loader reads it; writing the second threw "duplicate entry" and the whole mod wasn't converted
			var written = new HashSet<String>();
			for (ZipEntry entry : Collections.list(zip.entries())) {
				String path = entry.getName();
				if (entry.isDirectory() || path.matches("META-INF/[^/]+\\.(SF|RSA|DSA|EC)")) {
					continue;
				}
				if (!path.endsWith(".class") && !written.add(path)) {
					continue;
				}
				byte[] bytes;
				try (InputStream in = zip.getInputStream(entry)) {
					bytes = ModJar.readAll(in);
				}
				if (jar.mixinConfigs.contains(path)) {
					bytes = mixinConfig(bytes, jar, remapper, accessors);
				}
				if (path.endsWith(".class") && !path.startsWith("META-INF/")) {
					var reader = new ClassReader(bytes);
					var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					reader.accept(new ListenerTypes(new FacadeRemapper(writer, remapper, table.supers, target, table.kinds, table.extendsOnly)), 0);
					bytes = writer.toByteArray();
					path = remapper.map(reader.getClassName()) + ".class";
					if (!accessors.isEmpty()) {
						String className = reader.getClassName();
						bytes = accessors.rewrite(className, bytes);
						String moved = accessors.name(className);
						if (!moved.equals(className)) {
							path = moved + ".class";
						}
					}
					// calls through 1.21 interfaces the 1.12.2 classes don't implement (DataComponentHolder on ItemStack)
					bytes = InterfaceDispatch.rewrite(reader.getClassName(), bytes);
					if (!written.add(path)) {
						// the same class twice, or two classes the tables give one name: the first one stays
						Conversion.LOGGER.warn("{}: {} is in the jar twice (or two classes got this name), the first one is kept",
								jar.displayName(), path);
						continue;
					}
				}
				out.putNextEntry(new ZipEntry(path));
				out.write(bytes);
				out.closeEntry();
			}
		}
	}

	/**
	 * Translator classes built in the 1.21 shape (the 1.21 class's members under its names), so a mod's mixin into
	 * the 1.21 class works on them: JourneyMap adds its TextureAccess interface to DynamicTexture (and reads the image
	 * through a shadow of getPixels); without the mixin its textures fail a cast and its minimap is never built.
	 */
	private static final java.util.Set<String> MIXABLE = new java.util.HashSet<>(java.util.Arrays.asList(
			"io/github/xponer2/translator/client/DynamicTexture",
			"io/github/xponer2/translator/client/NativeImage"));

	/**
	 * A mixin config for Mixin on 1.12.2 (MixinBooter): Java 8 like the converted classes, no refmap (the annotations
	 * carry the 1.12.2 names now), and nothing required: a mixin that finds no target is left out, not the game.
	 */
	private static byte[] mixinConfig(byte[] bytes, ModJar jar, ModRemapper remapper, AccessorMixins accessors) {
		try {
			var config = new com.google.gson.JsonParser().parse(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
			// mixins into classes that are Translator classes now (BuiltInRegistries, BlockEntityType) are left out: the
			// Translator class is built differently, an injection there breaks it for every mod
			String pkg = config.has("package") ? config.get("package").getAsString().replace('.', '/') + "/" : "";
			for (String side : new String[] { "mixins", "client", "server" }) {
				if (!config.has(side) || !config.get(side).isJsonArray()) {
					continue;
				}
				var kept = new com.google.gson.JsonArray();
				for (var entry : config.getAsJsonArray(side)) {
					ClassInfo mixin = jar.classes.get(pkg + entry.getAsString().replace('.', '/'));
					boolean translatorOnly = mixin != null && !mixin.mixinTargets.isEmpty();
					for (String target : mixin == null ? java.util.Collections.<String>emptyList() : mixin.mixinTargets) {
						String mapped = remapper.map(target);
						translatorOnly &= mapped.startsWith("io/github/xponer2/translator/") && !MIXABLE.contains(mapped);
					}
					if (translatorOnly) {
						Conversion.LOGGER.info("{}: mixin {} left out (its target is a Translator class)", jar.displayName(), entry.getAsString());
					} else if (accessors.isMoved(pkg + entry.getAsString().replace('.', '/'))) {
						Conversion.LOGGER.info("{}: mixin {} left out (an accessor that works through reflection)", jar.displayName(), entry.getAsString());
					} else {
						kept.add(entry);
					}
				}
				config.add(side, kept);
			}
			config.addProperty("compatibilityLevel", "JAVA_8");
			config.addProperty("required", false);
			config.addProperty("minVersion", "0.8");
			config.remove("refmap");
			var injectors = config.has("injectors") ? config.getAsJsonObject("injectors") : new com.google.gson.JsonObject();
			injectors.addProperty("defaultRequire", 0);
			config.add("injectors", injectors);
			return config.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
		} catch (RuntimeException e) {
			Conversion.LOGGER.warn("Can't read the mixin config: {}", e.toString());
			return bytes;
		}
	}

	private static String hash(File file) throws IOException {
		try {
			var digest = MessageDigest.getInstance("SHA-1");
			digest.update(Files.readAllBytes(file.toPath()));
			digest.update(VERSION.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			var hex = new StringBuilder();
			for (byte b : digest.digest()) {
				hex.append(String.format("%02x", b));
			}
			return hex.substring(0, 12);
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}
}
