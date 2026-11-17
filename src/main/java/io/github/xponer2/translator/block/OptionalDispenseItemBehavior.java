/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.block;

/**
 * 1.21 dispenser behavior that may fail (then the dispenser plays the fail sound): subclasses override
 * {@link #execute} and call {@link #setSuccess}.
 */
public class OptionalDispenseItemBehavior extends DefaultDispenseItemBehavior {
	private boolean success = true;

	public boolean isSuccess() {
		return this.success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	@Override
	protected void playSound(BlockSource source) {
		source.level().playEvent(this.success ? 1000 : 1001, source.pos(), 0);
	}
}
