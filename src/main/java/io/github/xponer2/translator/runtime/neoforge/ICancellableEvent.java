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

/**
 * NeoForge {@code ICancellableEvent}. The 1.12.2 events have {@code setCanceled}/{@code isCanceled} themselves (on
 * {@code @Cancelable} events), so mapped events need nothing; this is for events a mod declares itself.
 */
public interface ICancellableEvent {
	/** Cancels a Forge event that can be cancelled (the 1.21 events that are 1.12.2 ones). */
	default void setCanceled(boolean canceled) {
		if (this instanceof net.minecraftforge.fml.common.eventhandler.Event event && event.isCancelable()) {
			event.setCanceled(canceled);
		}
	}

	default boolean isCanceled() {
		return this instanceof net.minecraftforge.fml.common.eventhandler.Event event && event.isCanceled();
	}
}
