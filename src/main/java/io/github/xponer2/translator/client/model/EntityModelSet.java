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

	/** The model part of the layer: the definition's {@code bakeRoot()}, a part without cubes if there is none. */
	public ModelPart bakeLayer(Object location) {
		Supplier<?> supplier = DEFINITIONS.get(location);
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

	public void onResourceManagerReload(Object resourceManager) {
		this.baked.clear();
	}
}
