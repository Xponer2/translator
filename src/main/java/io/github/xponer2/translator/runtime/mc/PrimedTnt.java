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

import io.github.xponer2.translator.entity.SynchedDataBuilder;
import io.github.xponer2.translator.registry.EntityType;

import net.minecraft.world.World;

/**
 * 1.21 {@code PrimedTnt} for the mods' primed explosives that extend it (AE2's tiny TNT): made with their EntityType (1.12.2's
 * EntityTNTPrimed has no such constructor). The table maps the class to 1.12.2's; a mod's own one extends this instead (the E
 * line: calls on PrimedTnt itself stay 1.12.2's). Without it the mod's constructor couldn't run and no such entity existed. Its update calls {@link #explode()}, which the mod overrides (1.12.2's is private: the mod's
 * explosion never went off, a full TNT blast did instead).
 */
public abstract class PrimedTnt extends net.minecraft.entity.item.EntityTNTPrimed {
	private final EntityType<?> type;

	protected PrimedTnt(EntityType<?> type, World world) {
		super(world);
		this.type = type;
		if (type != null) {
			this.setSize(type.getWidth(), type.getHeight());
		}
	}

	protected PrimedTnt(World world, double x, double y, double z, @javax.annotation.Nullable net.minecraft.entity.EntityLivingBase igniter) {
		super(world, x, y, z, igniter);
		this.type = null;
	}

	public EntityType<?> getType() {
		return MobFacades.type(this.type, this);
	}

	/** 1.21 {@code defineSynchedData(builder)}: the mod's data, after 1.12.2's. */
	protected void defineSynchedData(SynchedDataBuilder builder) {
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.defineSynchedData(SynchedDataBuilder.of(this));
	}

	/** 1.12.2's update of primed TNT, going off through {@link #explode()}. */
	@Override
	public void onUpdate() {
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		if (!this.hasNoGravity()) {
			this.motionY -= 0.04D;
		}
		this.move(net.minecraft.entity.MoverType.SELF, this.motionX, this.motionY, this.motionZ);
		this.motionX *= 0.98D;
		this.motionY *= 0.98D;
		this.motionZ *= 0.98D;
		if (this.onGround) {
			this.motionX *= 0.7D;
			this.motionZ *= 0.7D;
			this.motionY *= -0.5D;
		}
		int fuse = this.getFuse() - 1;
		this.setFuse(fuse);
		if (fuse <= 0) {
			this.setDead();
			if (!this.world.isRemote) {
				this.explode();
			}
		} else {
			this.handleWaterMovement();
			this.world.spawnParticle(net.minecraft.util.EnumParticleTypes.SMOKE_NORMAL, this.posX, this.posY + 0.5D, this.posZ, 0.0D, 0.0D, 0.0D);
		}
	}

	/** 1.21 {@code explode()}: TNT's blast (the mod's own when it overrides it). */
	public void explode() {
		this.world.createExplosion(this, this.posX, this.posY + this.height / 16.0F, this.posZ, 4.0F, true);
	}
}
