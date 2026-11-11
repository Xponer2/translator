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
 * limb swing fields of the entity, read and written through it. A mod's own state (made with the 1.21 constructor, or
 * a subclass of it: Iron's Spells' frozen humanoid keeps a still pose) keeps its values itself, as 1.21's.
 */
public class WalkAnimationState {
	@javax.annotation.Nullable
	private final EntityLivingBase entity;
	private float speedOld;
	private float speed;
	private float position;

	public WalkAnimationState(EntityLivingBase entity) {
		this.entity = entity;
	}

	/** 1.21 {@code new WalkAnimationState()}: a state of its own. */
	public WalkAnimationState() {
		this.entity = null;
	}

	/** 1.21 {@code LivingEntity.walkAnimation}. */
	public static WalkAnimationState of(EntityLivingBase entity) {
		return new WalkAnimationState(entity);
	}

	public void setSpeed(float speed) {
		if (this.entity == null) {
			this.speed = speed;
		} else {
			this.entity.limbSwingAmount = speed;
		}
	}

	public void update(float movement, float multiplier) {
		if (this.entity == null) {
			this.speedOld = this.speed;
			this.speed += (movement - this.speed) * multiplier;
			this.position += this.speed;
			return;
		}
		this.entity.prevLimbSwingAmount = this.entity.limbSwingAmount;
		this.entity.limbSwingAmount += (movement - this.entity.limbSwingAmount) * multiplier;
		this.entity.limbSwing += this.entity.limbSwingAmount;
	}

	public float speed() {
		return this.entity == null ? this.speed : this.entity.limbSwingAmount;
	}

	public float speed(float partialTick) {
		float old = this.entity == null ? this.speedOld : this.entity.prevLimbSwingAmount;
		return Math.min(old + (this.speed() - old) * partialTick, 1.0F);
	}

	public float position() {
		return this.entity == null ? this.position : this.entity.limbSwing;
	}

	public float position(float partialTick) {
		return this.position() - this.speed() * (1.0F - partialTick);
	}

	public boolean isMoving() {
		return this.speed() > 1.0E-5F;
	}
}
