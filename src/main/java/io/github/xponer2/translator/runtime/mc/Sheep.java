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
 * 1.21 {@code Sheep} for the mods' sheep that extend it (the Twilight Forest's bighorn): made with their EntityType (1.12.2's
 * EntitySheep takes the world only), their size and attributes from the type. The table maps the class to 1.12.2's; a mod's own one extends this instead
 * (the E line: calls on Sheep itself stay 1.12.2's). Without it the mod's constructor (super(type, level)) couldn't
 * run and no such entity existed in the game.
 */
public abstract class Sheep extends net.minecraft.entity.passive.EntitySheep {
	private final EntityType<?> type;

	protected Sheep(EntityType<?> type, World world) {
		super(world);
		this.type = type;
		if (type != null) {
			this.setSize(type.getWidth(), type.getHeight());
			AttributeSupplier.apply(this, type);
		}
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

	/** 1.21 {@code LivingEntity.createLivingAttributes()}. */
	public static AttributeSupplier.Builder createLivingAttributes() {
		return AttributeSupplier.living();
	}

	/** 1.21 {@code Mob.createMobAttributes()}. */
	public static AttributeSupplier.Builder createMobAttributes() {
		return AttributeSupplier.mob();
	}

	/** The drops of the mob's 1.21 loot table (entities/<type>), else 1.12.2's. */
	@Override
	protected void dropLoot(boolean recentlyHit, int looting, net.minecraft.util.DamageSource source) {
		if (!MobFacades.dropLoot(this, this.getType(), recentlyHit, looting, source)) {
			super.dropLoot(recentlyHit, looting, source);
		}
	}

	/** 1.21 {@code getBreedOffspring(level, partner)}: the child (1.12.2's by default); the mod's override makes it. */
	@javax.annotation.Nullable
	public net.minecraft.entity.passive.EntitySheep getBreedOffspring(net.minecraft.world.WorldServer level, net.minecraft.entity.EntityAgeable partner) {
		return super.createChild(partner);
	}

	@Override
	public net.minecraft.entity.passive.EntitySheep createChild(net.minecraft.entity.EntityAgeable partner) {
		return this.getBreedOffspring(this.world instanceof net.minecraft.world.WorldServer server ? server : null, partner);
	}
}
