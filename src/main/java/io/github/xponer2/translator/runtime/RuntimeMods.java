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
import java.lang.reflect.Constructor;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.xponer2.translator.runtime.neoforge.FMLClientSetupEvent;
import io.github.xponer2.translator.runtime.neoforge.FMLCommonSetupEvent;
import io.github.xponer2.translator.runtime.neoforge.ForgeBus;
import io.github.xponer2.translator.runtime.neoforge.IEventBus;
import io.github.xponer2.translator.runtime.neoforge.ModContainer;
import io.github.xponer2.translator.util.FMLEnvironment;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Runs original NeoForge 1.21.1 mod jars (tools/RUNTIME.md): finds them in {@code mods/} and {@code mods/1.21.1/}
 * (FML ignores them: it can't read Java 21 classes), converts them once into {@code translator/cache/}, adds them to
 * the class path and starts their mods like NeoForge (constructor with the event bus and the mod container,
 * {@code @EventBusSubscriber} classes, then the setup events in init).
 */
public final class RuntimeMods {
	static final Logger LOGGER = Conversion.LOGGER;
	private static final List<ModContainer> MODS = new ArrayList<>();

	private RuntimeMods() {
	}

	public static List<ModContainer> mods() {
		return MODS;
	}

	private static final Map<File, io.github.xponer2.translator.util.ModList.ModFileScanData> SCAN_DATA = new java.util.LinkedHashMap<>();

	/** The scan data of the original 1.21.1 mods, one per jar. */
	public static synchronized List<io.github.xponer2.translator.util.ModList.ModFileScanData> scanData() {
		Conversion.Result result = Conversion.result();
		if (result != null) {
			for (ModJar jar : result.mods) {
				scanData(jar.file);
			}
		}
		return new ArrayList<>(SCAN_DATA.values());
	}

	/** The scan data of one jar (the original jar of a 1.21.1 mod; empty for another file). */
	public static synchronized io.github.xponer2.translator.util.ModList.ModFileScanData scanData(File file) {
		return SCAN_DATA.computeIfAbsent(file, f -> {
			var annotations = new java.util.LinkedHashSet<io.github.xponer2.translator.util.ModList.AnnotationData>();
			var classes = new java.util.LinkedHashSet<io.github.xponer2.translator.util.ModList.ClassData>();
			Conversion.Result result = Conversion.result();
			for (ModJar jar : result == null ? java.util.Collections.<ModJar>emptyList() : result.mods) {
				if (!jar.file.equals(f)) {
					continue;
				}
				for (Object[] a : jar.annotations) {
					@SuppressWarnings("unchecked")
					var values = (Map<String, Object>) a[4];
					// the annotation type as the mods see it now (a NeoForge annotation is a Translator one), types in
					// the values as the game's ASM types
					String type = ((String) a[0]).substring(1, ((String) a[0]).length() - 1);
					String mapped = result.table.classes.getOrDefault(type, type);
					var converted = new java.util.HashMap<String, Object>();
					values.forEach((key, value) -> converted.put(key, asmValue(value)));
					annotations.add(new io.github.xponer2.translator.util.ModList.AnnotationData(mapped, (String) a[2], (String) a[3],
							java.lang.annotation.ElementType.valueOf((String) a[1]), converted));
				}
			}
			for (ModJar jar : result == null ? java.util.Collections.<ModJar>emptyList() : result.mods) {
				if (!jar.file.equals(f)) {
					continue;
				}
				for (ClassInfo info : jar.classes.values()) {
					var interfaces = new java.util.LinkedHashSet<org.objectweb.asm.Type>();
					for (String itf : info.interfaces) {
						interfaces.add(org.objectweb.asm.Type.getObjectType(result.table.classes.getOrDefault(itf, itf)));
					}
					String parent = info.superName == null ? "java/lang/Object" : info.superName;
					classes.add(new io.github.xponer2.translator.util.ModList.ClassData(org.objectweb.asm.Type.getObjectType(info.name),
							org.objectweb.asm.Type.getObjectType(result.table.classes.getOrDefault(parent, parent)), interfaces));
				}
			}
			var infos = new java.util.ArrayList<io.github.xponer2.translator.runtime.neoforge.ModFileInfo>();
			for (ModJar jar : result == null ? java.util.Collections.<ModJar>emptyList() : result.mods) {
				if (jar.file.equals(f)) {
					for (String id : jar.modIds()) {
						var container = io.github.xponer2.translator.runtime.neoforge.ModContainer.byId(id);
						var info = container == null ? null : container.getModInfo().getOwningFile();
						if (info != null && !infos.contains(info)) {
							infos.add(info);
						}
					}
				}
			}
			return new io.github.xponer2.translator.util.ModList.ModFileScanData(annotations, classes, infos);
		});
	}

	private static Object asmValue(Object value) {
		if (value instanceof ModJar.EnumValue constant) {
			// NeoForge's scan data gives enum constants as ModAnnotation.EnumHolder (the carried class): SuperMartijn642's
			// registry acceptors read theirs so, and no menu type, block or item of its mods was found without it
			try {
				return Class.forName("net.neoforged.fml.loading.modscan.ModAnnotation$EnumHolder", true, Launch.classLoader)
						.getConstructor(String.class, String.class).newInstance(constant.descriptor, constant.value);
			} catch (ReflectiveOperationException | LinkageError e) {
				return constant.value;
			}
		}
		if (value instanceof xyz.wagyourtail.jvmdg.shade.asm.Type type) {
			return org.objectweb.asm.Type.getType(type.getDescriptor());
		}
		if (value instanceof List<?> list) {
			var out = new ArrayList<Object>();
			for (Object element : list) {
				out.add(asmValue(element));
			}
			return out;
		}
		return value;
	}

	/** A namespace of an original 1.21.1 mod: its registrations are lenient (an entry that fails is skipped). */
	public static boolean isRuntimeNamespace(String namespace) {
		for (var mod : MODS) {
			if (mod.getModId().equals(namespace)) {
				return true;
			}
		}
		return false;
	}

	public static void skipped(Object id, Throwable error) {
		var reason = new StringBuilder(error.toString());
		// where it failed: the first frames (a NullPointerException says nothing else)
		StackTraceElement[] frames = error.getStackTrace();
		for (int i = 0; i < Math.min(3, frames.length); i++) {
			reason.append(i == 0 ? " at " : " < ").append(frames[i]);
		}
		for (Throwable cause = error.getCause(); cause != null && cause != cause.getCause(); cause = cause.getCause()) {
			reason.append(" <- ").append(cause);
			if (cause.getStackTrace().length > 0) {
				reason.append(" at ").append(cause.getStackTrace()[0]);
			}
		}
		LOGGER.warn("Skipped {}: {}", id, reason);
	}

	/** Pre-init of the Translator: before the 1.12.2 registry events, so the mods' DeferredRegisters take part. */
	public static void load(File gameDir) {
		// converted already when the coremod ran (before Minecraft loaded, so the mods' mixins apply), else now
		Conversion.Result result = Conversion.result();
		if (result == null) {
			result = Conversion.run(gameDir, steps -> {
				var bar = net.minecraftforge.fml.common.ProgressManager.push("Translator: 1.21.1 mods", steps);
				return new Conversion.Steps() {
					@Override
					public void step(String name) {
						bar.step(name);
					}

					@Override
					public void done() {
						while (bar.getStep() < bar.getSteps()) {
							bar.step("");
						}
						net.minecraftforge.fml.common.ProgressManager.pop(bar);
					}
				};
			});
		}
		if (result == null) {
			return;
		}
		io.github.xponer2.translator.runtime.neoforge.TickEvents.register();
		if (FMLEnvironment.dist == Side.CLIENT) {
			io.github.xponer2.translator.runtime.neoforge.ClientEvents.register();
		}
		detectVersion();
		// their mixins, before any of their classes loads (too late for Minecraft's classes without the coremod)
		if (!result.mixinsAdded) {
			LateMixins.add(result.mixinConfigs);
		}
		// 5. started in dependency order (their constructors may use each other)
		for (ModJar jar : result.mods) {
			if (result.converted.containsKey(jar)) {
				containers(jar);
			}
		}
		try (var bar = io.github.xponer2.translator.util.LoadingBar.push("Translator: starting the 1.21.1 mods", result.mods.size())) {
			if (FMLEnvironment.dist == Side.CLIENT && Minecraft1211.clientJar() != null) {
				try {
					io.github.xponer2.translator.runtime.client.RuntimeResourcePack.addVanilla(Minecraft1211.clientJar());
				} catch (Throwable t) {
					LOGGER.warn("Can't read the vanilla 1.21.1 models: {}", t.toString());
				}
			}
			for (ModJar jar : result.mods) {
				bar.step(jar.displayName());
				File file = result.converted.get(jar);
				if (file == null) {
					for (Map<String, String> mod : jar.mods) {
						FmlMods.FAILED.putIfAbsent(mod.get("modId"), "the jar couldn't be converted");
					}
					continue;
				}
				try {
					if (FMLEnvironment.dist == Side.CLIENT) {
						io.github.xponer2.translator.runtime.client.RuntimeResourcePack.add(file, jar.displayName());
					}
					start(jar);
				} catch (Throwable t) {
					LOGGER.error("Can't load the 1.21.1 mod {}", jar.file.getName(), t);
					for (Map<String, String> mod : jar.mods) {
						FmlMods.FAILED.putIfAbsent(mod.get("modId"), reason(t));
					}
				}
			}
		}
		// NeoForge: the mods' own registries, after construction and before the registry events
		try {
			MinecraftForge.EVENT_BUS.post(new io.github.xponer2.translator.registry.NewRegistryEvent());
		} catch (Throwable t) {
			LOGGER.error("NewRegistryEvent failed", t);
		}
		Conversion.release();
	}

	/**
	 * What the 1.21.1 Bootstrap does first: SharedConstants.getCurrentVersion() (pack formats, data versions) throws until
	 * the version is set. 1.12.2 has no version.json, so it is the version the 1.21.1 classes were built with.
	 */
	private static void detectVersion() {
		try {
			Class.forName(ApiCarry.SHADOW.replace('/', '.') + "net.minecraft.SharedConstants", true, Launch.classLoader)
					.getMethod("tryDetectVersion").invoke(null);
		} catch (Throwable t) {
			LOGGER.warn("Can't set the 1.21.1 game version: {}", t.toString());
		}
	}

	/**
	 * What the 1.21.1 client does when it starts: the 1.21.1 {@code RenderSystem} the mods draw with (JourneyMap's
	 * minimap) learns that this thread renders. Without it every call checks the thread and throws.
	 */
	private static void initRenderThread() {
		try {
			Class<?> renderSystem = Class.forName(ApiCarry.SHADOW.replace('/', '.') + "com.mojang.blaze3d.systems.RenderSystem", true, Launch.classLoader);
			if (!(Boolean) renderSystem.getMethod("isOnRenderThread").invoke(null)) {
				renderSystem.getMethod("initRenderThread").invoke(null);
			}
		} catch (ClassNotFoundException e) {
			// no mod draws with it
		} catch (Throwable t) {
			LOGGER.warn("Can't set the render thread of the 1.21.1 RenderSystem: {}", t.toString());
		}
	}

	/** The containers of a jar's mods: made for every mod before the first is started (ModList knows them all). */
	private static void containers(ModJar jar) {
		for (Map<String, String> mod : jar.mods) {
			String id = mod.get("modId");
			if (id != null && ModContainer.byId(id) == null) {
				String version = mod.getOrDefault("version", "?");
				MODS.add(new ModContainer(id, mod.getOrDefault("displayName", id), version.contains("$") ? "1.0" : version,
						mod.getOrDefault("description", ""), jar.file));
			}
		}
	}

	private static void start(ModJar jar) throws ReflectiveOperationException {
		for (var entry : jar.modClasses.entrySet()) {
			// an entry point for the other side only (@Mod(dist = Dist.DEDICATED_SERVER): AE2's AppEngServer)
			List<String> dists = jar.modDists.get(entry.getKey());
			if (dists != null && !dists.isEmpty() && !dists.contains(FMLEnvironment.dist == Side.CLIENT ? "CLIENT" : "DEDICATED_SERVER")) {
				continue;
			}
			ModContainer container = ModContainer.byId(entry.getValue());
			if (container == null) {
				container = new ModContainer(entry.getValue(), entry.getValue(), "1.0", "", jar.file);
				MODS.add(container);
			}
			Class<?> mainClass = Class.forName(entry.getKey().replace('/', '.'), true, Launch.classLoader);
			io.github.xponer2.translator.runtime.neoforge.ModLoadingContext.setActive(container);
			try {
				construct(mainClass, container);
			} finally {
				io.github.xponer2.translator.runtime.neoforge.ModLoadingContext.setActive(null);
			}
			LOGGER.info("Started {} ({})", container.getDisplayName(), container.getModId());
		}
		for (var entry : jar.subscribers.entrySet()) {
			Object sides = entry.getValue().get("value");
			if (sides instanceof List<?> list && !list.isEmpty()) {
				boolean client = FMLEnvironment.dist == Side.CLIENT;
				if (!list.contains(client ? "CLIENT" : "DEDICATED_SERVER")) {
					continue;
				}
			}
			try {
				Class<?> subscriber = Class.forName(entry.getKey().replace('/', '.'), true, Launch.classLoader);
				ForgeBus.FORGE.register(subscriber);
			} catch (Throwable t) {
				LOGGER.error("Can't register the event subscriber {}: {}", entry.getKey(), t.toString());
			}
		}
	}

	private static void construct(Class<?> mainClass, ModContainer container) throws ReflectiveOperationException {
		Constructor<?> best = null;
		for (Constructor<?> constructor : mainClass.getDeclaredConstructors()) {
			if (best == null || constructor.getParameterCount() > best.getParameterCount()) {
				best = constructor;
			}
		}
		if (best == null) {
			throw new NoSuchMethodException(mainClass.getName() + " has no constructor");
		}
		Object[] args = new Object[best.getParameterCount()];
		Class<?>[] types = best.getParameterTypes();
		for (int i = 0; i < types.length; i++) {
			if (types[i] == IEventBus.class) {
				args[i] = ForgeBus.FORGE;
			} else if (types[i] == ModContainer.class) {
				args[i] = container;
			} else if (types[i] == Side.class) {
				args[i] = FMLEnvironment.dist;
			} else {
				throw new NoSuchMethodException(mainClass.getName() + ": unknown constructor parameter " + types[i].getName());
			}
		}
		best.setAccessible(true);
		best.newInstance(args);
	}

	/** Init of the Translator: the NeoForge setup events. */
	/** Post-init of the Translator: the last NeoForge lifecycle event; then the mods join Forge's mod list. */
	public static void complete() {
		if (!MODS.isEmpty()) {
			try (var bar = io.github.xponer2.translator.util.LoadingBar.push("Translator: finishing the 1.21.1 mods", 1)) {
				bar.step("load complete");
				MinecraftForge.EVENT_BUS.post(new io.github.xponer2.translator.runtime.neoforge.FMLLoadCompleteEvent());
			}
			FmlMods.add(MODS);
		}
	}

	/** The first line of the deepest cause, for the mod list. */
	private static String reason(Throwable t) {
		Throwable root = t;
		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		String text = root.toString();
		int newline = text.indexOf('\n');
		text = newline < 0 ? text : text.substring(0, newline);
		return text.length() > 300 ? text.substring(0, 300) + "..." : text;
	}

	public static void setup() {
		if (MODS.isEmpty()) {
			return;
		}
		try (var bar = io.github.xponer2.translator.util.LoadingBar.push("Translator: setting up the 1.21.1 mods", 4)) {
			bar.step("common setup");
			MinecraftForge.EVENT_BUS.post(new FMLCommonSetupEvent());
			bar.step(FMLEnvironment.dist == Side.CLIENT ? "client setup" : "server setup");
			if (FMLEnvironment.dist == Side.CLIENT) {
				initRenderThread();
				io.github.xponer2.translator.runtime.neoforge.ClientEvents.setup();
				MinecraftForge.EVENT_BUS.post(new FMLClientSetupEvent());
			} else {
				MinecraftForge.EVENT_BUS.post(new io.github.xponer2.translator.runtime.neoforge.FMLDedicatedServerSetupEvent());
			}
			bar.step("messages between mods");
			MinecraftForge.EVENT_BUS.post(new io.github.xponer2.translator.runtime.neoforge.InterModEnqueueEvent());
			bar.step("processing the messages");
			MinecraftForge.EVENT_BUS.post(new io.github.xponer2.translator.runtime.neoforge.InterModProcessEvent());
		}
		for (var mod : MODS) {
			var items = new ArrayList<String>();
			for (var id : net.minecraft.item.Item.REGISTRY.getKeys()) {
				if (id.getNamespace().equals(mod.getModId())) {
					items.add(id.getPath());
				}
			}
			int blocks = 0;
			for (var id : net.minecraft.block.Block.REGISTRY.getKeys()) {
				if (id.getNamespace().equals(mod.getModId())) {
					blocks++;
				}
			}
			LOGGER.info("{}: {} items, {} blocks registered: {}", mod.getDisplayName(), items.size(), blocks, items);
		}
	}
}
