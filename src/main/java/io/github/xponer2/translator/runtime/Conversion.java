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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.launchwrapper.Launch;

/**
 * The conversion of the 1.21.1 mods (tools/RUNTIME.md): the Minecraft 1.21.1 libraries, the 1.21.1 API, the libraries
 * inside the mods, then the mods in dependency waves, each converted jar put on the class path. No Minecraft or Forge
 * class is touched, so it can run before Minecraft's classes load (the coremod, {@code core.TranslatorCore}): then
 * the mods' mixins into Minecraft classes apply. Done once; {@link RuntimeMods} starts the mods in pre-init.
 */
public final class Conversion {
	static final Logger LOGGER = LogManager.getLogger("Translator Runtime");
	private static Result result;

	private Conversion() {
	}

	/** What the conversion gave: the mods in dependency order, their converted jars, their mixin configs. */
	static final class Result {
		final List<ModJar> mods;
		final Map<ModJar, File> converted;
		final List<String> mixinConfigs;
		final RemapTable table;
		/** The mixin configs were handed to Mixin before Minecraft loaded (the coremod). */
		boolean mixinsAdded;

		Result(List<ModJar> mods, Map<ModJar, File> converted, List<String> mixinConfigs, RemapTable table) {
			this.mods = mods;
			this.converted = converted;
			this.mixinConfigs = mixinConfigs;
			this.table = table;
		}
	}

	/** One step after another on a progress bar (Forge's in pre-init, none before Minecraft loads). */
	interface Steps {
		void step(String name);

		void done();
	}

	/**
	 * Before the game window opens (the coremod) the conversion has a small window of its own: the first start converts
	 * every mod, which takes minutes, and nothing else would show that the game is starting.
	 */
	static final class WindowSteps implements Steps {
		private javax.swing.JFrame frame;
		private javax.swing.JProgressBar bar;
		private javax.swing.JLabel label;
		private int step;

		WindowSteps(int total) {
			try {
				javax.swing.SwingUtilities.invokeAndWait(() -> {
					this.frame = new javax.swing.JFrame("Translator: preparing the 1.21.1 mods");
					this.label = new javax.swing.JLabel("Converting the 1.21.1 mods for Minecraft 1.12.2...");
					this.bar = new javax.swing.JProgressBar(0, Math.max(1, total));
					this.bar.setStringPainted(true);
					var panel = new javax.swing.JPanel(new java.awt.BorderLayout(8, 8));
					panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
					panel.add(this.label, java.awt.BorderLayout.NORTH);
					panel.add(this.bar, java.awt.BorderLayout.CENTER);
					this.frame.setContentPane(panel);
					this.frame.setSize(520, 110);
					this.frame.setLocationRelativeTo(null);
					this.frame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
					this.frame.setVisible(true);
				});
			} catch (Exception | LinkageError e) {
				LOGGER.warn("No progress window: {}", e.toString());
			}
		}

		@Override
		public void step(String name) {
			if (this.frame != null) {
				int value = ++this.step;
				javax.swing.SwingUtilities.invokeLater(() -> {
					this.bar.setValue(value);
					this.label.setText(name);
				});
			}
		}

		@Override
		public void done() {
			if (this.frame != null) {
				javax.swing.SwingUtilities.invokeLater(this.frame::dispose);
			}
		}
	}

	private static final Steps NO_STEPS = new Steps() {
		@Override
		public void step(String name) {
		}

		@Override
		public void done() {
		}
	};

	static synchronized Result result() {
		return result;
	}

	/**
	 * After the mods started: what only the conversion needed goes (the members of every class of every jar, the
	 * downgrader). The scan data the mods read later (their annotations and class hierarchy) is made first.
	 */
	static void release() {
		Result done = result();
		if (done == null) {
			return;
		}
		RuntimeMods.scanData();
		for (ModJar jar : done.mods) {
			jar.classes.clear();
		}
		JarConverter.release();
	}

	/**
	 * The conversion from the coremod, before Minecraft's classes load: the mods' mixin configs go to Mixin now, so
	 * their mixins into Minecraft classes apply. On failure the mods are converted in pre-init as without the coremod.
	 */
	public static void runEarly(File gameDir) {
		try {
			long start = System.currentTimeMillis();
			Result early = run(gameDir, java.awt.GraphicsEnvironment.isHeadless() ? null : WindowSteps::new);
			if (early != null) {
				early.mixinsAdded = LateMixins.register(early.mixinConfigs);
				LOGGER.info("1.21.1 mods converted before Minecraft loaded ({} ms), {} mixin configs added", System.currentTimeMillis() - start,
						early.mixinsAdded ? early.mixinConfigs.size() : 0);
			}
		} catch (Throwable t) {
			LOGGER.error("Can't convert the 1.21.1 mods before Minecraft loads: they are converted in pre-init (mixins into Minecraft classes are left out)", t);
		}
	}

	/**
	 * The conversion, once: the 1.21.1 mod jars of {@code mods/} and {@code mods/1.21.1/}, null when there are none.
	 * {@code progress} makes the progress bar for that many steps (null: none).
	 */
	static synchronized Result run(File gameDir, IntFunction<Steps> progress) {
		if (result != null) {
			return result;
		}
		var jars = new ArrayList<File>();
		for (File dir : new File[] { new File(gameDir, "mods"), new File(gameDir, "mods/1.21.1") }) {
			File[] files = dir.listFiles((d, name) -> name.endsWith(".jar"));
			if (files != null) {
				Arrays.sort(files);
				for (File file : files) {
					if (ModJar.isModJar(file)) {
						jars.add(file);
					}
				}
			}
		}
		if (jars.isEmpty()) {
			return null;
		}
		LOGGER.info("Found {} NeoForge 1.21.1 mod jar(s): {}", jars.size(), jars);
		RemapTable table;
		try {
			table = RemapTable.load();
		} catch (Exception e) {
			LOGGER.error("Can't read the remap table, the 1.21.1 mods are not loaded", e);
			return null;
		}
		var target = new TargetClasses();
		Set<URL> classpath = java.util.Collections.synchronizedSet(new HashSet<>(Launch.classLoader.getSources()));
		File cache = new File(gameDir, "translator/cache");
		long begin = System.currentTimeMillis();

		// what is in the jars: mods, and the libraries and mods they carry inside them (NeoForge JarJar)
		var mods = new ArrayList<ModJar>();
		var libraries = new java.util.LinkedHashMap<String, File>();
		var seenIds = new HashSet<String>();
		var queue = new java.util.ArrayDeque<File>(jars);
		while (!queue.isEmpty()) {
			File file = queue.poll();
			try {
				if (!ModJar.isModJar(file)) {
					libraries.putIfAbsent(file.getName(), file);
					continue;
				}
				ModJar jar = ModJar.read(file);
				if (!jar.modIds().isEmpty() && !seenIds.addAll(jar.modIds())) {
					continue; // the same mod twice (standalone and inside another mod)
				}
				mods.add(jar);
				queue.addAll(JarJar.extract(file, new File(cache, "jarjar")));
			} catch (Throwable t) {
				LOGGER.error("Can't read {}", file.getName(), t);
			}
		}
		mods = new ArrayList<>(JarJar.withDependencies(mods));
		File api = ApiCarry.find();
		var mojang = api == null ? new ArrayList<File>() : ApiCarry.libraries();
		Steps bar = progress == null ? NO_STEPS : progress.apply(mojang.size() + (api == null ? 0 : 1) + libraries.size() + mods.size());
		var pool = java.util.concurrent.Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() - 2));
		var converted = new HashMap<ModJar, File>();
		try {
			// 1. the libraries of Minecraft 1.21.1 (Codec, CommandDispatcher, JOML, ...), 2. the full 1.21.1 API
			// (tools/RUNTIME.md "Direction"): every API class the mods were compiled against
			convertLibraries(mojang, true, bar, pool, cache, table, target, classpath);
			if (api == null) {
				LOGGER.warn("No Minecraft 1.21.1 + NeoForge API jar found (-Dtranslator.api): the mods only get the classes the Translator has");
			} else {
				bar.step("Minecraft 1.21.1 API");
				try {
					long start = System.currentTimeMillis();
					File carried = ApiCarry.prepare(api, cache, table, target, classpath);
					addToClassPath(carried, classpath, target);
					LOGGER.info("Minecraft 1.21.1 API ready in {} ms -> {}", System.currentTimeMillis() - start, carried.getName());
				} catch (Throwable t) {
					LOGGER.error("Can't prepare the Minecraft 1.21.1 API", t);
				}
			}
			// 3. the libraries inside the mods, 4. the mods: in waves, each mod once the mods it depends on are there
			convertLibraries(new ArrayList<>(libraries.values()), false, bar, pool, cache, table, target, classpath);
			JarJar.addUsedMods(mods);
			var remaining = new ArrayList<>(JarJar.dependencyOrder(mods));
			while (!remaining.isEmpty()) {
				var wave = JarJar.ready(remaining, converted.keySet());
				remaining.removeAll(wave);
				// the mods of one wave see each other's classes (as they are, until converted): mods that use each
				// other (Xaero's minimap and world map read each other's version fields) are in one wave
				var together = new ArrayList<File>();
				for (ModJar jar : wave) {
					together.add(jar.file);
				}
				if (together.size() > 1) {
					target.addPending(together);
				}
				var futures = new ArrayList<java.util.concurrent.Future<File>>();
				for (ModJar jar : wave) {
					futures.add(pool.submit(() -> convertMod(jar, cache, table, target, classpath)));
				}
				for (int i = 0; i < wave.size(); i++) {
					ModJar jar = wave.get(i);
					bar.step(jar.displayName());
					try {
						File file = futures.get(i).get();
						converted.put(jar, file);
					} catch (Throwable t) {
						LOGGER.error("Can't convert the 1.21.1 mod {}", jar.file.getName(), t.getCause() != null ? t.getCause() : t);
					}
				}
				if (together.size() > 1) {
					target.clearPending();
				}
				for (ModJar jar : wave) {
					File file = converted.get(jar);
					if (file != null) {
						try {
							addToClassPath(file, classpath, target);
						} catch (java.io.IOException e) {
							LOGGER.error("Can't add {} to the class path", file.getName(), e);
						}
					}
				}
			}
			LOGGER.info("{} NeoForge 1.21.1 mods converted in {} ms", converted.size(), System.currentTimeMillis() - begin);
		} finally {
			pool.shutdown();
			bar.done();
		}
		enumExtensions(converted.keySet(), table, target);
		var ordered = JarJar.dependencyOrder(mods);
		var mixinConfigs = new ArrayList<String>();
		for (ModJar jar : ordered) {
			if (converted.containsKey(jar)) {
				mixinConfigs.addAll(jar.mixinConfigs);
			}
		}
		result = new Result(ordered, converted, mixinConfigs, table);
		return result;
	}

	/**
	 * The enum constants the mods add (mods.toml {@code enumExtensions}, NeoForge's enumextensions.json), with the
	 * enums and constructors under their 1.12.2 / Translator names: made when the enum loads (core.EnumExtender).
	 */
	private static void enumExtensions(Collection<ModJar> jars, RemapTable table, TargetClasses target) {
		var names = new ModRemapper(table, java.util.Collections.emptyMap(), target);
		for (ModJar jar : jars) {
			for (Map<String, String> mod : jar.mods) {
				String path = mod.get("enumExtensions");
				if (path == null) {
					continue;
				}
				try (var zip = new java.util.zip.ZipFile(jar.file)) {
					var entry = zip.getEntry(path);
					if (entry == null) {
						continue;
					}
					com.google.gson.JsonObject json;
					try (var in = zip.getInputStream(entry)) {
						json = new com.google.gson.JsonParser().parse(new String(ModJar.readAll(in), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
					}
					for (var element : json.getAsJsonArray("entries")) {
						var e = element.getAsJsonObject();
						var parameters = e.get("parameters");
						List<Object> constants = null;
						String[] source = null;
						boolean proxy = false;
						if (parameters.isJsonArray()) {
							constants = new ArrayList<>();
							for (var p : parameters.getAsJsonArray()) {
								if (p.isJsonNull()) {
									constants.add(null);
								} else if (p.isJsonObject()) {
									constants.add(new String[] { p.getAsJsonObject().get("class").getAsString(), p.getAsJsonObject().get("field").getAsString() });
								} else if (p.getAsJsonPrimitive().isNumber()) {
									constants.add(p.getAsNumber());
								} else if (p.getAsJsonPrimitive().isBoolean()) {
									constants.add(p.getAsBoolean());
								} else {
									constants.add(p.getAsString());
								}
							}
						} else {
							var reference = parameters.getAsJsonObject();
							proxy = reference.has("field");
							source = new String[] { reference.get("class").getAsString(), reference.get(proxy ? "field" : "method").getAsString() };
						}
						io.github.xponer2.translator.core.EnumExtender.register(new io.github.xponer2.translator.core.EnumExtender.Extension(
								names.map(e.get("enum").getAsString()), e.get("name").getAsString(), names.mapMethodDesc(e.get("constructor").getAsString()),
								constants, source, proxy));
					}
				} catch (java.io.IOException | RuntimeException e) {
					LOGGER.warn("Can't read the enum extensions {} of {}: {}", path, jar.displayName(), e.toString());
				}
			}
		}
	}

	/** Libraries (no mods.toml): converted in parallel, put on the class path (unless the game has them already). */
	private static void convertLibraries(List<File> files, boolean gapFill, Steps bar, java.util.concurrent.ExecutorService pool, File cache,
			RemapTable table, TargetClasses target, Set<URL> classpath) {
		// which of them the game already has (a library a 1.12.2 mod brings): those aren't converted
		var jars = new ArrayList<ModJar>();
		var todo = new ArrayList<File>();
		for (File file : files) {
			try {
				ModJar jar = ModJar.library(file, file.getName());
				if (JarJar.alreadyPresent(jar, target)) {
					// an older version is there: of a Minecraft 1.21.1 library, its newer parts are added
					File gap = gapFill ? ApiCarry.gap(file, cache, target) : null;
					jar = gap == null ? null : ModJar.library(gap, file.getName() + " (newer parts)");
				}
				jars.add(jar);
				if (jar != null) {
					todo.add(jar.file);
				}
			} catch (Throwable t) {
				jars.add(null);
				LOGGER.error("Can't read the library {}", file.getName(), t);
			}
		}
		// libraries depend on each other (night-config toml on core): each sees the others while they convert
		target.addPending(todo);
		try {
			var futures = new ArrayList<java.util.concurrent.Future<File>>();
			for (ModJar jar : jars) {
				futures.add(jar == null ? null : pool.submit(() -> JarConverter.convert(jar, cache, table, target, classpath, new TreeMap<>())));
			}
			for (int i = 0; i < files.size(); i++) {
				bar.step(files.get(i).getName());
				if (futures.get(i) == null) {
					continue;
				}
				try {
					File done = futures.get(i).get();
					addToClassPath(done, classpath, target);
					LOGGER.info("Library {} -> {}", files.get(i).getName(), done.getName());
				} catch (Throwable t) {
					LOGGER.error("Can't load the library {}", files.get(i).getName(), t.getCause() != null ? t.getCause() : t);
				}
			}
		} finally {
			target.clearPending();
		}
	}

	private static File convertMod(ModJar jar, File cache, RemapTable table, TargetClasses target, Set<URL> classpath) throws java.io.IOException {
		long start = System.currentTimeMillis();
		var missing = new TreeMap<String, Integer>();
		File converted = JarConverter.convert(jar, cache, table, target, classpath, missing);
		LOGGER.info("{}: converted in {} ms -> {}", jar.displayName(), System.currentTimeMillis() - start, converted.getName());
		if (!missing.isEmpty()) {
			File report = new File(cache, converted.getName().replaceAll("\\.jar$", ".missing.txt"));
			java.nio.file.Files.write(report.toPath(), missing.keySet());
			LOGGER.warn("{} uses {} 1.21.1 classes the Translator doesn't have yet (list: {}): {}", jar.displayName(), missing.size(),
					report.getName(), summary(missing.keySet()));
		}
		return converted;
	}

	private static synchronized void addToClassPath(File jar, Set<URL> classpath, TargetClasses target) throws java.net.MalformedURLException {
		URL url = jar.toURI().toURL();
		Launch.classLoader.addURL(url);
		classpath.add(url);
		addToParent(jar, url);
		// LaunchClassLoader remembers the classes it didn't find (the converter asked for API classes before their jar
		// was added; Netty probed org.slf4j.LoggerFactory at startup): forgotten, or the new jar's classes stay
		// "missing" for the game too
		try {
			for (String name : new String[] { "negativeResourceCache", "invalidClasses" }) {
				java.lang.reflect.Field cache = net.minecraft.launchwrapper.LaunchClassLoader.class.getDeclaredField(name);
				cache.setAccessible(true);
				((Set<?>) cache.get(Launch.classLoader)).clear();
			}
		} catch (ReflectiveOperationException e) {
			LOGGER.warn("Can't clear the negative class cache of LaunchClassLoader: {}", e.toString());
		}
		target.forgetMissing();
	}

	/**
	 * A jar with classes of packages LaunchClassLoader leaves to the class loader above it (FML: org.apache.,
	 * com.google.common.): that loader gets the jar too, else the newer commons-lang3 / guava classes (TriFunction) are
	 * never found.
	 */
	@SuppressWarnings("unchecked")
	private static void addToParent(File jar, URL url) {
		try {
			var exceptions = net.minecraft.launchwrapper.LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
			exceptions.setAccessible(true);
			var prefixes = new ArrayList<String>();
			for (String prefix : (Set<String>) exceptions.get(Launch.classLoader)) {
				prefixes.add(prefix.replace('.', '/'));
			}
			boolean needed = false;
			try (var zip = new java.util.zip.ZipFile(jar)) {
				for (var e = zip.entries(); e.hasMoreElements() && !needed; ) {
					String name = e.nextElement().getName();
					for (String prefix : prefixes) {
						if (name.endsWith(".class") && name.startsWith(prefix)) {
							needed = true;
							break;
						}
					}
				}
			}
			ClassLoader parent = Launch.class.getClassLoader();
			if (needed && parent instanceof java.net.URLClassLoader) {
				var addURL = java.net.URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
				addURL.setAccessible(true);
				addURL.invoke(parent, url);
				LOGGER.info("{}: also on the system class path (packages LaunchClassLoader leaves to it)", jar.getName());
			}
		} catch (ReflectiveOperationException | java.io.IOException | RuntimeException e) {
			LOGGER.warn("Can't add {} to the system class path: {}", jar.getName(), e.toString());
		}
	}

	static String summary(Collection<String> names) {
		var list = new ArrayList<>(names);
		return list.size() <= 25 ? list.toString() : list.subList(0, 25) + " and " + (list.size() - 25) + " more";
	}
}
