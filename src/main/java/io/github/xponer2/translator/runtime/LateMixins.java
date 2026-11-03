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

import java.util.List;

/**
 * The mixin configs of the converted mods, handed to Mixin (MixinBooter on 1.12.2) once their jars are on the class
 * path and before their classes load: the mixins into the mods' own classes and into each other's apply (GlitchCore's
 * NeoForge side replaces methods of its common classes this way). Minecraft's classes are loaded by then, so mixins
 * into them are left out (Mixin logs them). Without Mixin, nothing happens. By reflection: Mixin is optional.
 */
final class LateMixins {
	private LateMixins() {
	}

	/**
	 * Before Minecraft loads (the coremod): the configs are only added, Mixin selects them itself when its environment
	 * moves on, as for MixinBooter's early configs. False without Mixin.
	 */
	static boolean register(List<String> configs) {
		if (configs.isEmpty()) {
			return true;
		}
		try {
			Class<?> mixins = Class.forName("org.spongepowered.asm.mixin.Mixins", true, net.minecraft.launchwrapper.Launch.classLoader);
			var addConfiguration = mixins.getMethod("addConfiguration", String.class);
			addEach(addConfiguration, configs);
			return true;
		} catch (ClassNotFoundException e) {
			Conversion.LOGGER.warn("No Mixin (MixinBooter): the mixins of the 1.21.1 mods are left out ({})", configs);
			return false;
		} catch (Throwable t) {
			Conversion.LOGGER.error("Can't add the mixin configs {}", configs, t);
			return false;
		}
	}

	/** One config Mixin can't read (a missing file, a bad mixin) is left out, the others are added. */
	private static void addEach(java.lang.reflect.Method addConfiguration, List<String> configs) {
		for (String config : configs) {
			try {
				addConfiguration.invoke(null, config);
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				Throwable cause = e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null ? e.getCause() : e;
				Conversion.LOGGER.error("Mixin config {} left out: {}", config, cause.toString());
			}
		}
	}

	static void add(List<String> configs) {
		if (configs.isEmpty()) {
			return;
		}
		try {
			Class<?> mixins = Class.forName("org.spongepowered.asm.mixin.Mixins", true, net.minecraft.launchwrapper.Launch.classLoader);
			var addConfiguration = mixins.getMethod("addConfiguration", String.class);
			addEach(addConfiguration, configs);
			// what MixinBooter does for its late mixins: the processor selects the configs added since
			Class<?> environment = Class.forName("org.spongepowered.asm.mixin.MixinEnvironment", true, net.minecraft.launchwrapper.Launch.classLoader);
			Object current = environment.getMethod("getCurrentEnvironment").invoke(null);
			Class<?> proxy = Class.forName("org.spongepowered.asm.mixin.transformer.Proxy", true, net.minecraft.launchwrapper.Launch.classLoader);
			var transformerField = proxy.getDeclaredField("transformer");
			transformerField.setAccessible(true);
			Object transformer = transformerField.get(null);
			Object processor = transformer.getClass().getMethod("getProcessor").invoke(transformer);
			var select = processor.getClass().getDeclaredMethod("select", environment);
			select.setAccessible(true);
			for (int attempt = 1; ; attempt++) {
				try {
					select.invoke(processor, current);
					break;
				} catch (java.lang.reflect.InvocationTargetException e) {
					// the config set changed while Mixin went through it (configs that add configs): the configs not
					// selected yet are taken in the next round
					if (!(e.getCause() instanceof java.util.ConcurrentModificationException) || attempt == 5) {
						throw e;
					}
				}
			}
			Conversion.LOGGER.info("{} mixin configs of 1.21.1 mods added: {}", configs.size(), configs);
		} catch (ClassNotFoundException e) {
			Conversion.LOGGER.warn("No Mixin (MixinBooter): the mixins of the 1.21.1 mods are left out ({})", configs);
		} catch (Throwable t) {
			Conversion.LOGGER.error("Can't add the mixin configs {}", configs, t);
		}
	}
}
