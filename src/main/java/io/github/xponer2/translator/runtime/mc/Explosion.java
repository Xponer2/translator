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

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.21 {@code Explosion} for the mods' own explosions (Supplementaries' cannon balls and gunpowder): 1.21's
 * constructors on 1.12.2's explosion, and the fields 1.21 code reads that 1.12.2 keeps private (level, x, y, z,
 * radius, the source, the damage calculator). A plain {@code new Explosion(...)} goes to LevelGapBridges.newExplosion.
 */
public class Explosion extends net.minecraft.world.Explosion {
	/** 1.21 {@code level} (the table's name: world). */
	protected final World world;
	protected final double x;
	protected final double y;
	protected final double z;
	/** 1.21 {@code radius} (the table's name: size). */
	protected final float size;
	@Nullable
	protected final Entity source;
	@Nullable
	protected final net.minecraft.world.level.ExplosionDamageCalculator damageCalculator;
	/** 1.21 {@code fire} (the table's name: causesFire, private in 1.12.2). */
	protected final boolean causesFire;
	@Nullable
	protected final net.minecraft.world.level.Explosion.BlockInteraction blockInteraction;

	public Explosion(World level, @Nullable Entity source, double x, double y, double z, float radius, boolean fire,
			@Nullable net.minecraft.world.level.Explosion.BlockInteraction interaction) {
		super(level, source, x, y, z, radius, fire, breaksBlocks(interaction));
		this.world = level;
		this.source = source;
		this.x = x;
		this.y = y;
		this.z = z;
		this.size = radius;
		this.causesFire = fire;
		this.blockInteraction = interaction;
		this.damageCalculator = null;
	}

	public Explosion(World level, @Nullable Entity source, double x, double y, double z, float radius, boolean fire,
			@Nullable net.minecraft.world.level.Explosion.BlockInteraction interaction, List<BlockPos> toBlow) {
		super(level, source, x, y, z, radius, fire, breaksBlocks(interaction), toBlow);
		this.world = level;
		this.source = source;
		this.x = x;
		this.y = y;
		this.z = z;
		this.size = radius;
		this.causesFire = fire;
		this.blockInteraction = interaction;
		this.damageCalculator = null;
	}

	/** 1.21's explosion with its damage source, calculator and effects: 1.12.2's own damage and particles. */
	public Explosion(World level, @Nullable Entity source, @Nullable net.minecraft.util.DamageSource damage,
			@Nullable net.minecraft.world.level.ExplosionDamageCalculator calculator, double x, double y, double z, float radius, boolean fire,
			@Nullable net.minecraft.world.level.Explosion.BlockInteraction interaction, @Nullable io.github.xponer2.translator.util.particle.ParticleOptions small,
			@Nullable io.github.xponer2.translator.util.particle.ParticleOptions large, @Nullable io.github.xponer2.translator.registry.Holder<?> sound) {
		super(level, source, x, y, z, radius, fire, breaksBlocks(interaction));
		this.world = level;
		this.source = source;
		this.x = x;
		this.y = y;
		this.z = z;
		this.size = radius;
		this.causesFire = fire;
		this.blockInteraction = interaction;
		this.damageCalculator = calculator;
	}

	/** Whether a 1.21 block interaction breaks blocks: all but KEEP (TRIGGER_BLOCK only triggers, 1.12.2 can't). */
	private static boolean breaksBlocks(@Nullable Object interaction) {
		if (!(interaction instanceof Enum<?> value)) {
			return true;
		}
		return !"KEEP".equals(value.name()) && !"TRIGGER_BLOCK".equals(value.name());
	}

	/** 1.21 {@code getBlockInteraction()}. */
	@Nullable
	public net.minecraft.world.level.Explosion.BlockInteraction getBlockInteraction() {
		return this.blockInteraction;
	}

	/** 1.21 {@code center()}: where it went off. */
	public net.minecraft.util.math.Vec3d center() {
		return new net.minecraft.util.math.Vec3d(this.x, this.y, this.z);
	}

	/** 1.21 {@code radius()}. */
	public float radius() {
		return this.size;
	}

	/** 1.21 {@code getDirectSourceEntity()}. */
	@Nullable
	public Entity getDirectSourceEntity() {
		return this.source;
	}

	/** 1.21 {@code interactsWithBlocks()}. */
	public boolean interactsWithBlocks() {
		return breaksBlocks(this.blockInteraction);
	}
}
