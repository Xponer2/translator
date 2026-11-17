/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.runtime.neoforge;

import java.util.concurrent.CompletableFuture;

import net.minecraftforge.fml.common.eventhandler.Event;

/** NeoForge lifecycle events that run work "later on the main thread": 1.12.2 is on the main thread already. */
public class ParallelDispatchEvent extends Event {
	public CompletableFuture<Void> enqueueWork(Runnable work) {
		work.run();
		return CompletableFuture.completedFuture(null);
	}
}
