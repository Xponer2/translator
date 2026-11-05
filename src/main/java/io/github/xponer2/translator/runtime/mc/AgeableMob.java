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

import io.github.xponer2.translator.entity.AttributeSupplier;
import io.github.xponer2.translator.entity.SynchedDataBuilder;
import io.github.xponer2.translator.registry.EntityType;

import net.minecraft.world.World;

/**
 * 1.21 {@code AgeableMob} for the mods' ageable mobs that extend it: made with their EntityType (1.12.2's EntityAgeable takes the
 * world only), their size and attributes from the type. Without it their constructor couldn't run and no such entity
 * existed in the game.
 */
public abstract class AgeableMob extends net.minecraft.entity.EntityAgeable {
	private final EntityType<?> type;

	protected AgeableMob(EntityType<?> type, World world) {
		super(world);
		this.type = type;
		if (type != null) {
			this.setSize(type.getWidth(), type.getHeight());
			AttributeSupplier.apply(this, type);
		}
	}

	public EntityType<?> getType() {
		return this.type;
	}

	/** 1.21 {@code defineSynchedData(builder)} (the tables name it entityInit): a mod's override calls it as super. */
	protected void entityInit(SynchedDataBuilder builder) {
	}

	/** 1.12.2 defines the data here: the 1.12.2 data first, then the mod's (its override of the builder method). */
	@Override
	protected void entityInit() {
		super.entityInit();
		this.entityInit(SynchedDataBuilder.of(this));
	}

	/** 1.21 {@code LivingEntity.createLivingAttributes()}. */
	public static AttributeSupplier.Builder createLivingAttributes() {
		return AttributeSupplier.living();
	}

	/** 1.21 {@code Mob.createMobAttributes()}. */
	public static AttributeSupplier.Builder createMobAttributes() {
		return AttributeSupplier.mob();
	}

	/** 1.21 {@code getBreedOffspring(level, partner)}: the child of a mod's mob (nothing here). */
	@javax.annotation.Nullable
	public net.minecraft.entity.EntityAgeable getBreedOffspring(net.minecraft.world.WorldServer level, net.minecraft.entity.EntityAgeable partner) {
		return null;
	}

	@Override
	public net.minecraft.entity.EntityAgeable createChild(net.minecraft.entity.EntityAgeable partner) {
		return this.getBreedOffspring(this.world instanceof net.minecraft.world.WorldServer server ? server : null, partner);
	}
}
