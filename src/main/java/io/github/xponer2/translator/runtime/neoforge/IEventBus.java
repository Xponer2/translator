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
package io.github.xponer2.translator.runtime.neoforge;

import java.util.function.Consumer;

import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;

/**
 * NeoForge {@code IEventBus} for original 1.21.1 mods. The mod bus and the game bus are both the 1.12.2 Forge event
 * bus: the Translator posts its 1.21 lifecycle events there. The overloads with a trailing {@code Class} are what
 * {@code addListener(Consumer)} calls become (the converter adds the event class from the bytecode).
 */
public interface IEventBus {
	void register(Object target);

	void unregister(Object target);

	<T extends Event> T post(T event);

	<T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Class<T> type, Consumer<T> consumer);

	default <T extends Event> void addListener(Class<T> type, Consumer<T> consumer) {
		this.addListener(EventPriority.NORMAL, false, type, consumer);
	}

	default <T extends Event> void addListener(Consumer<T> consumer, Class<T> type) {
		this.addListener(EventPriority.NORMAL, false, type, consumer);
	}

	default <T extends Event> void addListener(EventPriority priority, Consumer<T> consumer, Class<T> type) {
		this.addListener(priority, false, type, consumer);
	}

	default <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Consumer<T> consumer, Class<T> type) {
		this.addListener(priority, receiveCanceled, type, consumer);
	}

	default <T extends Event> void addListener(EventPriority priority, Class<T> type, Consumer<T> consumer) {
		this.addListener(priority, false, type, consumer);
	}

	/** A listener whose event type the converter couldn't find: logged, never called. */
	default <T extends Event> void addListener(Consumer<T> consumer) {
		ForgeBus.LOGGER.warn("A listener without a known event type was not added: {}", consumer);
	}
}
