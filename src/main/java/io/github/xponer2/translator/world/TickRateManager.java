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
package io.github.xponer2.translator.world;

import net.minecraft.entity.Entity;

/**
 * 1.21 {@code TickRateManager} (the /tick command's state): 1.12.2 always runs at 20 ticks a second and is never
 * frozen. Create's kinetic block entities ask it for their speed.
 */
public class TickRateManager {
	public static final TickRateManager INSTANCE = new TickRateManager();
	public static final float MIN_TICKRATE = 1.0F;

	public float tickrate() {
		return 20.0F;
	}

	public long nanosecondsPerTick() {
		return 50_000_000L;
	}

	public int frozenTicksToRun() {
		return 0;
	}

	public boolean runsNormally() {
		return true;
	}

	public boolean isSteppingForward() {
		return false;
	}

	public boolean isFrozen() {
		return false;
	}

	public boolean isEntityFrozen(Entity entity) {
		return false;
	}

	public void setFrozen(boolean frozen) {
	}

	public void setTickRate(float rate) {
	}

	public void tick() {
	}
}
