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

import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;

/**
 * 1.21 {@code NearestAttackableTargetGoal} for the goals of 1.21.1 mods that extend it (the table's E line: only
 * those): 1.21's constructors take any mob, the interval and a Java predicate, 1.12.2's a creature and a Guava one, so
 * none of the mods' own target goals could be made (a boss or a summon that picks its targets its own way had none).
 */
public class NearestAttackableTargetGoal<T extends EntityLivingBase> extends EntityAINearestAttackableTarget<T> {
	public NearestAttackableTargetGoal(EntityLiving mob, Class<T> targetType, boolean mustSee) {
		this(mob, targetType, 10, mustSee, false, null);
	}

	public NearestAttackableTargetGoal(EntityLiving mob, Class<T> targetType, boolean mustSee, @Nullable Predicate<EntityLivingBase> condition) {
		this(mob, targetType, 10, mustSee, false, condition);
	}

	public NearestAttackableTargetGoal(EntityLiving mob, Class<T> targetType, boolean mustSee, boolean mustReach) {
		this(mob, targetType, 10, mustSee, mustReach, null);
	}

	public NearestAttackableTargetGoal(EntityLiving mob, Class<T> targetType, int randomInterval, boolean mustSee, boolean mustReach,
			@Nullable Predicate<EntityLivingBase> condition) {
		super(creature(mob), targetType, randomInterval, mustSee, mustReach, condition == null ? null : condition::test);
	}

	// 1.21's goal methods keep their names in the classes that extend this one (this facade has them), and 1.12.2 calls
	// its own: each 1.12.2 method calls the 1.21 one, whose base here is 1.12.2's (a subclass's start() was never called,
	// its super.start() was left empty: the enderman's and the mods' own target goals did nothing when they started)

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

	@Override
	protected double getTargetDistance() {
		return this.getFollowDistance();
	}

	/** 1.21 {@code getFollowDistance()}. */
	protected double getFollowDistance() {
		return super.getTargetDistance();
	}

	/**
	 * 1.21 {@code findTarget()}: the nearest target of its type, kept as the target {@link #start} takes (1.12.2's search,
	 * without its random interval).
	 */
	protected void findTarget() {
		this.targetEntity = null;
		super.shouldExecute();
	}

	/** 1.21 {@code targetConditions} (a carried TargetingConditions): what a target must pass, set by a mod's goal. */
	@Nullable
	private Object targetConditionsValue;

	/** The field read (its type is carried: the converter reads it through this). */
	@Nullable
	public static Object targetConditions(NearestAttackableTargetGoal<?> goal) {
		return goal.targetConditionsValue;
	}

	/** The field written (Iron's Spells' FindSupportableTargetGoal sets its own). */
	public static void targetConditions(NearestAttackableTargetGoal<?> goal, @Nullable Object conditions) {
		goal.targetConditionsValue = conditions;
	}

	/** 1.12.2's search, then the mod's conditions on the target it found. */
	@Override
	public boolean shouldExecute() {
		boolean found = super.shouldExecute();
		if (found && this.targetConditionsValue != null
				&& !io.github.xponer2.translator.runtime.redirect.LevelGapBridges.matches(this.targetConditionsValue, this.taskOwner, this.targetEntity)) {
			this.targetEntity = null;
			return false;
		}
		return found;
	}

	/** 1.21 {@code setTarget(target)}: the target {@link #start} takes. */
	@SuppressWarnings("unchecked")
	public void setTarget(@Nullable EntityLivingBase target) {
		this.targetEntity = (T) target;
	}

	/** 1.21 {@code getTarget()}. */
	@Nullable
	public EntityLivingBase getTarget() {
		return this.targetEntity;
	}

	/** 1.12.2's target goals belong to creatures (mobs that walk); a 1.21 mob that isn't one can't have this goal here. */
	private static EntityCreature creature(EntityLiving mob) {
		if (mob instanceof EntityCreature) {
			return (EntityCreature) mob;
		}
		throw new IllegalArgumentException("Translator: a target goal of " + (mob == null ? null : mob.getClass().getName())
				+ ", which isn't a creature on 1.12.2 (only creatures have target goals there)");
	}
}
