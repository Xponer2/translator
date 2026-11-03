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

import net.minecraft.entity.EntityLivingBase;

/**
 * 1.21 {@code WalkAnimationState} (a living entity's {@code walkAnimation}, neoforge-1.21.1-runtime.tsv): the 1.12.2
 * limb swing fields of the entity, read and written through it.
 */
public final class WalkAnimationState {
	private final EntityLivingBase entity;

	public WalkAnimationState(EntityLivingBase entity) {
		this.entity = entity;
	}

	/** 1.21 {@code LivingEntity.walkAnimation}. */
	public static WalkAnimationState of(EntityLivingBase entity) {
		return new WalkAnimationState(entity);
	}

	public void setSpeed(float speed) {
		this.entity.limbSwingAmount = speed;
	}

	public void update(float movement, float multiplier) {
		this.entity.prevLimbSwingAmount = this.entity.limbSwingAmount;
		this.entity.limbSwingAmount += (movement - this.entity.limbSwingAmount) * multiplier;
		this.entity.limbSwing += this.entity.limbSwingAmount;
	}

	public float speed() {
		return this.entity.limbSwingAmount;
	}

	public float speed(float partialTick) {
		return Math.min(this.entity.prevLimbSwingAmount + (this.entity.limbSwingAmount - this.entity.prevLimbSwingAmount) * partialTick, 1.0F);
	}

	public float position() {
		return this.entity.limbSwing;
	}

	public float position(float partialTick) {
		return this.entity.limbSwing - this.entity.limbSwingAmount * (1.0F - partialTick);
	}

	public boolean isMoving() {
		return this.entity.limbSwingAmount > 1.0E-5F;
	}
}
