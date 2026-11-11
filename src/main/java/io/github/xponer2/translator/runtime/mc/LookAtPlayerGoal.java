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
package io.github.xponer2.translator.runtime.mc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIWatchClosest;

/**
 * 1.21 {@code LookAtPlayerGoal} for the goals of 1.21.1 mods that extend it (the E line: only those): 1.21's
 * constructor with {@code onlyHorizontal} isn't 1.12.2's, so the mods' own look goals couldn't be made (the Aether's Sun
 * Spirit, Ars Nouveau's conditional look, Supplementaries' plunderer). The goal methods as in the target goal facade.
 */
public class LookAtPlayerGoal extends EntityAIWatchClosest {
	public LookAtPlayerGoal(EntityLiving mob, Class<? extends Entity> type, float distance) {
		super(mob, type, distance);
	}

	public LookAtPlayerGoal(EntityLiving mob, Class<? extends Entity> type, float distance, float probability) {
		super(mob, type, distance, probability);
	}

	/** 1.21's with onlyHorizontal (1.12.2 looks up and down too). */
	public LookAtPlayerGoal(EntityLiving mob, Class<? extends Entity> type, float distance, float probability, boolean onlyHorizontal) {
		super(mob, type, distance, probability);
	}

	@Override
	public boolean shouldExecute() {
		return this.canUse();
	}

	/** 1.21 {@code canUse()}. */
	public boolean canUse() {
		return super.shouldExecute();
	}

	@Override
	public boolean shouldContinueExecuting() {
		return this.canContinueToUse();
	}

	/** 1.21 {@code canContinueToUse()}. */
	public boolean canContinueToUse() {
		return super.shouldContinueExecuting();
	}

	@Override
	public void startExecuting() {
		this.start();
	}

	/** 1.21 {@code start()}. */
	public void start() {
		super.startExecuting();
	}

	@Override
	public void resetTask() {
		this.stop();
	}

	/** 1.21 {@code stop()}. */
	public void stop() {
		super.resetTask();
	}

	@Override
	public void updateTask() {
		this.tick();
	}

	/** 1.21 {@code tick()}. */
	public void tick() {
		super.updateTask();
	}
}
