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
package io.github.xponer2.translator.world;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

/**
 * The damage sources of a level (1.21 {@code level.damageSources()}, neoforge-1.21.1-runtime.tsv): the 1.12.2 damage
 * sources of the same kind.
 */
public final class DamageSources {
	public static final DamageSources INSTANCE = new DamageSources();

	private DamageSources() {
	}

	public DamageSource generic() {
		return DamageSource.GENERIC;
	}

	public DamageSource magic() {
		return DamageSource.MAGIC;
	}

	public DamageSource indirectMagic(@Nullable Entity source, @Nullable Entity attacker) {
		return DamageSource.causeIndirectMagicDamage(source, attacker);
	}

	public DamageSource inFire() {
		return DamageSource.IN_FIRE;
	}

	public DamageSource onFire() {
		return DamageSource.ON_FIRE;
	}

	public DamageSource lava() {
		return DamageSource.LAVA;
	}

	public DamageSource hotFloor() {
		return DamageSource.HOT_FLOOR;
	}

	public DamageSource inWall() {
		return DamageSource.IN_WALL;
	}

	public DamageSource cramming() {
		return DamageSource.CRAMMING;
	}

	public DamageSource drown() {
		return DamageSource.DROWN;
	}

	public DamageSource starve() {
		return DamageSource.STARVE;
	}

	public DamageSource cactus() {
		return DamageSource.CACTUS;
	}

	public DamageSource sweetBerryBush() {
		return DamageSource.CACTUS;
	}

	public DamageSource fall() {
		return DamageSource.FALL;
	}

	public DamageSource flyIntoWall() {
		return DamageSource.FLY_INTO_WALL;
	}

	public DamageSource fellOutOfWorld() {
		return DamageSource.OUT_OF_WORLD;
	}

	public DamageSource wither() {
		return DamageSource.WITHER;
	}

	public DamageSource dragonBreath() {
		return DamageSource.DRAGON_BREATH;
	}

	public DamageSource anvil(@Nullable Entity entity) {
		return DamageSource.ANVIL;
	}

	public DamageSource fallingBlock(@Nullable Entity entity) {
		return DamageSource.FALLING_BLOCK;
	}

	public DamageSource lightningBolt() {
		return DamageSource.LIGHTNING_BOLT;
	}

	public DamageSource freeze() {
		return DamageSource.GENERIC;
	}

	public DamageSource stalagmite() {
		return DamageSource.GENERIC;
	}

	public DamageSource outOfBorder() {
		return DamageSource.IN_WALL;
	}

	public DamageSource genericKill() {
		return DamageSource.OUT_OF_WORLD;
	}

	public DamageSource mobAttack(EntityLivingBase mob) {
		return DamageSource.causeMobDamage(mob);
	}

	public DamageSource noAggroMobAttack(EntityLivingBase mob) {
		return DamageSource.causeMobDamage(mob);
	}

	public DamageSource playerAttack(EntityPlayer player) {
		return DamageSource.causePlayerDamage(player);
	}

	public DamageSource thrown(Entity projectile, @Nullable Entity thrower) {
		return DamageSource.causeThrownDamage(projectile, thrower);
	}

	/** 1.21 {@code arrow(arrow, shooter)}: 1.12.2's arrow damage. */
	public DamageSource arrow(net.minecraft.entity.projectile.EntityArrow arrow, @Nullable Entity shooter) {
		return DamageSource.causeArrowDamage(arrow, shooter);
	}

	/** 1.21 {@code trident(trident, thrower)}: thrown damage named "trident" (1.12.2 has no trident). */
	public DamageSource trident(Entity trident, @Nullable Entity thrower) {
		return new net.minecraft.util.EntityDamageSourceIndirect("trident", trident, thrower == null ? trident : thrower).setProjectile();
	}

	public DamageSource mobProjectile(Entity projectile, @Nullable EntityLivingBase thrower) {
		return DamageSource.causeThrownDamage(projectile, thrower);
	}

	public DamageSource explosion(@Nullable Entity source, @Nullable Entity attacker) {
		return attacker instanceof EntityLivingBase living ? DamageSource.causeExplosionDamage(living)
				: new DamageSource("explosion").setDifficultyScaled().setExplosion();
	}

	/** 1.21 {@code explosion(explosion)}: 1.12.2's damage of that explosion (by who set it off). */
	public DamageSource explosion(@Nullable net.minecraft.world.Explosion explosion) {
		return DamageSource.causeExplosionDamage(explosion);
	}

	/** 1.21 {@code campfire()}: fire damage named "campfire" (1.12.2 has no campfire). */
	public DamageSource campfire() {
		return new DamageSource("campfire").setFireDamage();
	}

	public DamageSource sonicBoom(Entity attacker) {
		return DamageSource.causeIndirectMagicDamage(attacker, attacker);
	}

	public DamageSource thorns(Entity attacker) {
		return DamageSource.causeThornsDamage(attacker);
	}

	public DamageSource fireball(Entity fireball, @Nullable Entity shooter) {
		return new net.minecraft.util.EntityDamageSourceIndirect("fireball", fireball, shooter).setFireDamage().setProjectile();
	}

	/** A damage type by its 1.21 key: the 1.12.2 source of that name, a generic one of the mod's name otherwise. */
	public DamageSource source(io.github.xponer2.translator.registry.ResourceKey<?> type) {
		return this.source(type, null, null);
	}

	public DamageSource source(io.github.xponer2.translator.registry.ResourceKey<?> type, @Nullable Entity attacker) {
		return this.source(type, attacker, attacker);
	}

	public DamageSource source(io.github.xponer2.translator.registry.ResourceKey<?> type, @Nullable Entity source, @Nullable Entity attacker) {
		String name = type.location().getPath();
		if (source != null) {
			return new net.minecraft.util.EntityDamageSourceIndirect(name, source, attacker);
		}
		return new DamageSource(name);
	}
}
