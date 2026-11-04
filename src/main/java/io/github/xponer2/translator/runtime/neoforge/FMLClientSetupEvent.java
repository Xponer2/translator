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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;

/**
 * NeoForge client setup, posted by the Translator after the common setup. That can be in 1.12.2's pre-init (the
 * mods are set up before the models are registered), where the game has no textures, font or item renderer yet,
 * which NeoForge's client setup has: the work a mod queues then runs at the start of 1.12.2's init.
 */
public class FMLClientSetupEvent extends ParallelDispatchEvent {
	private static final List<Runnable> DEFERRED = new ArrayList<>();

	@Override
	public CompletableFuture<Void> enqueueWork(Runnable work) {
		if (net.minecraft.client.Minecraft.getMinecraft().fontRenderer != null) {
			return super.enqueueWork(work);
		}
		var done = new CompletableFuture<Void>();
		synchronized (DEFERRED) {
			DEFERRED.add(() -> {
				try {
					work.run();
					done.complete(null);
				} catch (RuntimeException | LinkageError e) {
					// one mod's setup failing leaves the others theirs (NeoForge reports it and goes on loading)
					LogManager.getLogger("Translator Runtime").error("The client setup of a 1.21.1 mod failed", e);
					done.completeExceptionally(e);
				}
			});
		}
		return done;
	}

	/** The client setup work queued before the game had its textures and font (1.12.2's init calls it). */
	public static void runDeferred() {
		List<Runnable> work;
		synchronized (DEFERRED) {
			work = new ArrayList<>(DEFERRED);
			DEFERRED.clear();
		}
		work.forEach(Runnable::run);
	}
}
