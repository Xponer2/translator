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
package io.github.xponer2.translator.client.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code EntityModelSet}: the layer definitions the mods registered (by their model layer location), baked into
 * model parts when a renderer asks ({@code context.bakeLayer(location)}). The definitions are the 1.21 classes as
 * they are; what they bake is the Translator's {@link ModelPart}.
 */
@SideOnly(Side.CLIENT)
public class EntityModelSet {
	public static final EntityModelSet INSTANCE = new EntityModelSet();
	private static final Map<Object, Supplier<?>> DEFINITIONS = new ConcurrentHashMap<>();
	private final Map<Object, Object> baked = new ConcurrentHashMap<>();

	/** {@code event.registerLayerDefinition(location, supplier)} of NeoForge's RegisterLayerDefinitions. */
	public static void register(Object location, Supplier<?> definition) {
		DEFINITIONS.put(location, definition);
	}

	/**
	 * NeoForge's {@code ClientHooks.registerLayerDefinition(location, supplier)} (what a multiloader library registers
	 * through: GlitchCore, so Biomes O' Plenty's boat layers): the same layers. They went to NeoForge's own map, which
	 * nothing reads here, and the boats' renderers found no layer ("Can't find part left_paddle").
	 */
	public static void registerLayerDefinition(Object location, Supplier<?> definition) {
		register(location, definition);
	}

	/** The model part of the layer: the definition's {@code bakeRoot()}, a part without cubes if there is none. */
	public ModelPart bakeLayer(Object location) {
		Supplier<?> registered = DEFINITIONS.get(location);
		Object vanillaLayer = registered == null ? vanillaDefinition(location) : null;
		Supplier<?> supplier = registered != null ? registered : vanillaLayer != null ? () -> vanillaLayer : null;
		if (supplier == null) {
			io.github.xponer2.translator.runtime.RuntimeStubs.missing("EntityModelSet.bakeLayer: no layer definition " + location);
			return new ModelPart(java.util.Collections.emptyList(), java.util.Collections.emptyMap());
		}
		Object definition = this.baked.computeIfAbsent(location, key -> supplier.get());
		try {
			return (ModelPart) definition.getClass().getMethod("bakeRoot").invoke(definition);
		} catch (ReflectiveOperationException | ClassCastException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the model layer " + location, e);
			return new ModelPart(java.util.Collections.emptyList(), java.util.Collections.emptyMap());
		}
	}

	@javax.annotation.Nullable
	private static volatile Map<?, ?> vanilla;
	private static volatile boolean vanillaTried;

	/**
	 * A vanilla layer (ModelLayers.ZOMBIE, the armor layers, ...) from 1.21's own {@code LayerDefinitions.createRoots()}
	 * (carried), made once: mod renderers built on vanilla models (zombies, illagers, humanoid armor) found no layer.
	 */
	@javax.annotation.Nullable
	private static Object vanillaDefinition(Object location) {
		if (!vanillaTried) {
			synchronized (EntityModelSet.class) {
				if (!vanillaTried) {
					vanillaTried = true;
					try {
						Object roots = Class.forName("net.minecraft.client.model.geom.LayerDefinitions", true, EntityModelSet.class.getClassLoader())
								.getMethod("createRoots").invoke(null);
						if (roots instanceof Map<?, ?> map) {
							vanilla = map;
						}
					} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
						io.github.xponer2.translator.runtime.RuntimeStubs.failed("the vanilla model layers",
								e instanceof java.lang.reflect.InvocationTargetException ite ? ite.getCause() : e);
					}
				}
			}
		}
		Map<?, ?> map = vanilla;
		return map != null ? map.get(location) : null;
	}

	public void onResourceManagerReload(Object resourceManager) {
		this.baked.clear();
	}
}
