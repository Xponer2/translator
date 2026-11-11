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
 * 1.21 {@code Slime} for the mods' slimes that extend it (the Aether's swets and sentries, the Twilight Forest's maze slimes): made with their EntityType (1.12.2's
 * EntitySlime takes the world only), their attributes from the type; their size is the slime size, as 1.12.2 sets it. The table maps the class to 1.12.2's; a mod's own one extends this instead
 * (the E line: calls on Slime itself stay 1.12.2's). Without it the mod's constructor (super(type, level)) couldn't
 * run and no such entity existed in the game.
 */
public abstract class Slime extends net.minecraft.entity.monster.EntitySlime implements io.github.xponer2.translator.entity.SpawnFinalizing {
	private final EntityType<?> type;

	protected Slime(EntityType<?> type, World world) {
		super(world);
		this.type = type;
		if (type != null) {
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

	/**
	 * 1.21 {@code finalizeSpawn(level, difficulty, spawnType, groupData)}: the mod's mob sets itself up when it spawns
	 * (its override; 1.12.2's onInitialSpawn by default).
	 */
	@Override
	@javax.annotation.Nullable
	public net.minecraft.entity.IEntityLivingData finalizeSpawn(World level, net.minecraft.world.DifficultyInstance difficulty,
			io.github.xponer2.translator.entity.MobSpawnType spawnType, @javax.annotation.Nullable net.minecraft.entity.IEntityLivingData data) {
		return super.onInitialSpawn(difficulty, data);
	}

	/** 1.12.2 calls this when the mob spawns: the mod's finalizeSpawn. */
	@Override
	@javax.annotation.Nullable
	public net.minecraft.entity.IEntityLivingData onInitialSpawn(net.minecraft.world.DifficultyInstance difficulty,
			@javax.annotation.Nullable net.minecraft.entity.IEntityLivingData data) {
		return this.finalizeSpawn(this.world, difficulty, io.github.xponer2.translator.entity.MobSpawnType.NATURAL, data);
	}


	/**
	 * 1.21 {@code populateDefaultEquipmentSlots(random, difficulty)}: the mod's gear, 1.12.2's by default. A mod's
	 * override has this name or the table's (setEquipmentBasedOnDifficulty with the random): both lead here.
	 */
	protected void populateDefaultEquipmentSlots(java.util.Random random, net.minecraft.world.DifficultyInstance difficulty) {
		super.setEquipmentBasedOnDifficulty(difficulty);
	}

	protected void setEquipmentBasedOnDifficulty(java.util.Random random, net.minecraft.world.DifficultyInstance difficulty) {
		this.populateDefaultEquipmentSlots(random, difficulty);
	}

	@Override
	protected void setEquipmentBasedOnDifficulty(net.minecraft.world.DifficultyInstance difficulty) {
		this.setEquipmentBasedOnDifficulty(this.rand, difficulty);
	}

	/** 1.21 {@code populateDefaultEquipmentEnchantments(level, random, difficulty)}: as the gear. */
	protected void populateDefaultEquipmentEnchantments(World level, java.util.Random random, net.minecraft.world.DifficultyInstance difficulty) {
		super.setEnchantmentBasedOnDifficulty(difficulty);
	}

	protected void setEnchantmentBasedOnDifficulty(World level, java.util.Random random, net.minecraft.world.DifficultyInstance difficulty) {
		this.populateDefaultEquipmentEnchantments(level, random, difficulty);
	}

	@Override
	protected void setEnchantmentBasedOnDifficulty(net.minecraft.world.DifficultyInstance difficulty) {
		this.setEnchantmentBasedOnDifficulty(this.world, this.rand, difficulty);
	}
}
