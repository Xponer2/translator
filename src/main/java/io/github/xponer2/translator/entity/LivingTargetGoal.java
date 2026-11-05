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
package io.github.xponer2.translator.entity;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 1.21 {@code NearestAttackableTargetGoal} for a mob that isn't a 1.12.2 creature (a flying or plain Mob): the
 * nearest living entity of the class in its follow range (seen, if it must), its attack target.
 */
public class LivingTargetGoal<T extends EntityLivingBase> extends EntityAINearestAttackableTarget<T> {
	private final EntityLiving owner;
	private final Class<T> type;
	private final int interval;
	private final boolean mustSee;
	@Nullable
	private final java.util.function.Predicate<EntityLivingBase> condition;
	@Nullable
	private EntityLivingBase found;

	public LivingTargetGoal(EntityLiving owner, Class<T> type, int interval, boolean mustSee, @Nullable java.util.function.Predicate<EntityLivingBase> condition) {
		super(null, type, interval, mustSee, false, null);
		this.owner = owner;
		this.type = type;
		this.interval = interval;
		this.mustSee = mustSee;
		this.condition = condition;
		this.setMutexBits(1);
	}

	private double range() {
		IAttributeInstance range = this.owner.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
		return range == null ? 16.0D : range.getAttributeValue();
	}

	private boolean suitable(@Nullable EntityLivingBase target) {
		if (target == null || target == this.owner || !target.isEntityAlive() || !this.owner.canAttackClass(target.getClass())) {
			return false;
		}
		if (target instanceof EntityPlayer player && (player.isCreative() || player.isSpectator())) {
			return false;
		}
		if (this.owner.getDistanceSq(target) > this.range() * this.range()) {
			return false;
		}
		return (!this.mustSee || this.owner.getEntitySenses().canSee(target)) && (this.condition == null || this.condition.test(target));
	}

	@Override
	public boolean shouldExecute() {
		if (this.interval > 0 && this.owner.getRNG().nextInt(this.interval) != 0) {
			return false;
		}
		double range = this.range();
		List<T> candidates = this.owner.world.getEntitiesWithinAABB(this.type, this.owner.getEntityBoundingBox().grow(range, 4.0D, range), this::suitable);
		EntityLivingBase nearest = null;
		double best = Double.MAX_VALUE;
		for (T candidate : candidates) {
			double distance = this.owner.getDistanceSq(candidate);
			if (distance < best) {
				best = distance;
				nearest = candidate;
			}
		}
		this.found = nearest;
		return nearest != null;
	}

	@Override
	public void startExecuting() {
		this.owner.setAttackTarget(this.found);
	}

	@Override
	public boolean shouldContinueExecuting() {
		EntityLivingBase target = this.owner.getAttackTarget();
		return target != null && target.isEntityAlive() && this.owner.getDistanceSq(target) <= this.range() * this.range();
	}

	@Override
	public void resetTask() {
		this.owner.setAttackTarget(null);
		this.found = null;
	}

	@Override
	public void updateTask() {
	}
}
