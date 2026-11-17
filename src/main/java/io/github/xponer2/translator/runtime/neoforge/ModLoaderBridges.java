/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.runtime.neoforge;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * NeoForge's {@code ModLoader.postEvent} and its kin (X lines: the carried ModLoader's own post through each mod's
 * container in order, a ModList method the Translator lacks, and every carried 1.21 class that posts a mod bus event
 * posted nothing: skull models, client extensions, named render types). Every mod's bus is the one Forge bus here, so
 * an event is posted once, not once per mod.
 */
public final class ModLoaderBridges {
	private ModLoaderBridges() {
	}

	public static void postEvent(Event event) {
		ForgeBus.FORGE.post(event);
	}

	public static Event postEventWithReturn(Event event) {
		ForgeBus.FORGE.post(event);
		return event;
	}

	public static void postEventWrapContainerInModOrder(Event event) {
		ForgeBus.FORGE.post(event);
	}

	/** The wrappers run around each mod's listeners in NeoForge; one bus here: posted once, the wrappers left out. */
	public static void postEventWithWrapInModOrder(Event event, @Nullable BiConsumer<?, ?> before, @Nullable BiConsumer<?, ?> after) {
		ForgeBus.FORGE.post(event);
	}

	/** An event made for each mod (a mod's config events), each posted once. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void runEventGenerator(Function generator) {
		for (ModContainer container : new java.util.ArrayList<>(ModContainer.all())) {
			Object event = generator.apply(container);
			if (event instanceof Event posted) {
				ForgeBus.FORGE.post(posted);
			}
		}
	}
}
