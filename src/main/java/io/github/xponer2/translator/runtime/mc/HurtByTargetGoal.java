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

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIHurtByTarget;

/**
 * 1.21 {@code HurtByTargetGoal} for the goals of 1.21.1 mods that extend it (the table's E line: only those): 1.21's
 * constructor takes the mob and the types to leave alone, 1.12.2's also whether others are called for help. A mod's own
 * one couldn't be made: Iron's Spells' Dead King rising from its corpse crashed the server.
 */
public class HurtByTargetGoal extends EntityAIHurtByTarget {
	public HurtByTargetGoal(EntityCreature mob, Class<?>... ignored) {
		super(mob, false, ignored);
	}

	/** 1.21 {@code setAlertOthers(types)}: the mob calls others of its kind for help (but those types). */
	public HurtByTargetGoal setAlertOthers(Class<?>... types) {
		io.github.xponer2.translator.runtime.redirect.WorldGapBridges.setAlertOthers(this, types);
		return this;
	}

	// 1.21's goal methods keep their names in the classes that extend this one (this facade has them), and 1.12.2 calls
	// its own: each 1.12.2 method calls the 1.21 one, whose base here is 1.12.2's

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
