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

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.Holder;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * 1.21 {@code DamageSource} for the damage sources of 1.21.1 mods that extend it (the table's E line: only those; a
 * plain {@code new DamageSource(...)} is 1.12.2's own source of the right kind). 1.21's is made of a damage type and
 * the entities; 1.12.2's of a name, with the entities in subclasses: a mod's own source (a spell's, a boss attack's)
 * couldn't be made at all. Named by the type's id path, with the entities and the position 1.12.2 asks its sources for.
 */
public class DamageSource extends net.minecraft.util.DamageSource {
	@Nullable
	private final Entity direct;
	@Nullable
	private final Entity causing;
	@Nullable
	private final Vec3d position;

	@Nullable
	private final Holder<?> typeHolder;

	public DamageSource(Holder<?> type, @Nullable Entity direct, @Nullable Entity causing, @Nullable Vec3d position) {
		super(name(type));
		this.typeHolder = type;
		this.direct = direct;
		this.causing = causing;
		this.position = position;
	}

	public DamageSource(Holder<?> type, @Nullable Entity direct, @Nullable Entity causing) {
		this(type, direct, causing, null);
	}

	public DamageSource(Holder<?> type, Vec3d position) {
		this(type, null, null, position);
	}

	public DamageSource(Holder<?> type, @Nullable Entity entity) {
		this(type, entity, entity, null);
	}

	public DamageSource(Holder<?> type) {
		this(type, null, null, null);
	}

	/** 1.21 {@code type()}: the damage type it was made of (the carried DamageType of the registry), null without one. */
	@Nullable
	public net.minecraft.world.damagesource.DamageType type() {
		Object value = this.typeHolder == null ? null : this.typeHolder.value();
		return value instanceof net.minecraft.world.damagesource.DamageType type ? type : null;
	}

	private static String name(@Nullable Holder<?> type) {
		try {
			if (type != null) {
				var key = type.unwrapKey();
				if (key.isPresent()) {
					return key.get().location().getPath();
				}
			}
		} catch (RuntimeException e) {
			// a direct holder: no id to name it by
		}
		return "generic";
	}

	/** 1.12.2's "who is to blame" (1.21 {@code getEntity()}, the table's name for it). */
	@Nullable
	@Override
	public Entity getTrueSource() {
		return this.causing;
	}

	/** 1.12.2's "what hit" (1.21 {@code getDirectEntity()}). */
	@Nullable
	@Override
	public Entity getImmediateSource() {
		return this.direct;
	}

	/** Where the damage came from (shields block by it): the given position, else the direct entity's. */
	@Nullable
	@Override
	public Vec3d getDamageLocation() {
		if (this.position != null) {
			return this.position;
		}
		return this.direct == null ? null : new Vec3d(this.direct.posX, this.direct.posY, this.direct.posZ);
	}

	/** As 1.12.2's entity damage source: "death.attack.<name>" with the killer, the plain message without one. */
	@Override
	public ITextComponent getDeathMessage(EntityLivingBase victim) {
		Entity killer = this.causing != null ? this.causing : this.direct;
		if (killer == null) {
			return super.getDeathMessage(victim);
		}
		return new TextComponentTranslation("death.attack." + this.damageType, victim.getDisplayName(), killer.getDisplayName());
	}

	@Override
	public boolean isDifficultyScaled() {
		return this.causing != null && this.causing instanceof EntityLivingBase && !(this.causing instanceof net.minecraft.entity.player.EntityPlayer);
	}
}
