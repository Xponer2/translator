/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.util;

import javax.annotation.Nullable;

import net.minecraftforge.fml.common.ProgressManager;

/**
 * A bar of Forge's loading screen for a long step of the Translator ("Translator: starting 1.21.1 mods", 12/65,
 * Create), so the screen tells what the game is doing instead of standing on "Initializing mods Phase 1". Never
 * throws: a bar that can't be shown is skipped, one closed early is filled up (Forge refuses to pop an unfinished bar).
 */
public final class LoadingBar implements AutoCloseable {
	@Nullable
	private final ProgressManager.ProgressBar bar;

	private LoadingBar(@Nullable ProgressManager.ProgressBar bar) {
		this.bar = bar;
	}

	public static LoadingBar push(String title, int steps) {
		try {
			return new LoadingBar(steps > 0 ? ProgressManager.push(title, steps) : null);
		} catch (RuntimeException e) {
			return new LoadingBar(null);
		}
	}

	public void step(String message) {
		if (this.bar != null && this.bar.getStep() < this.bar.getSteps()) {
			try {
				this.bar.step(message);
			} catch (RuntimeException e) {
				// the screen only
			}
		}
	}

	@Override
	public void close() {
		if (this.bar == null) {
			return;
		}
		try {
			while (this.bar.getStep() < this.bar.getSteps()) {
				this.bar.step("");
			}
			ProgressManager.pop(this.bar);
		} catch (RuntimeException e) {
			// the screen only
		}
	}
}
