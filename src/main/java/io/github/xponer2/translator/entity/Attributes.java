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

import io.github.xponer2.translator.registry.Holder;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 1.21 attributes (registry holders there) that 1.12.2 has; step height and creative flight are virtual.
 */
public final class Attributes {
	public static final Holder<Attribute> MAX_HEALTH = forge(SharedMonsterAttributes.MAX_HEALTH);
	public static final Holder<Attribute> FOLLOW_RANGE = forge(SharedMonsterAttributes.FOLLOW_RANGE);
	public static final Holder<Attribute> KNOCKBACK_RESISTANCE = forge(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
	public static final Holder<Attribute> MOVEMENT_SPEED = forge(SharedMonsterAttributes.MOVEMENT_SPEED);
	public static final Holder<Attribute> FLYING_SPEED = forge(SharedMonsterAttributes.FLYING_SPEED);
	public static final Holder<Attribute> ATTACK_DAMAGE = forge(SharedMonsterAttributes.ATTACK_DAMAGE);
	public static final Holder<Attribute> ATTACK_SPEED = forge(SharedMonsterAttributes.ATTACK_SPEED);
	public static final Holder<Attribute> ARMOR = forge(SharedMonsterAttributes.ARMOR);
	public static final Holder<Attribute> ARMOR_TOUGHNESS = forge(SharedMonsterAttributes.ARMOR_TOUGHNESS);
	public static final Holder<Attribute> LUCK = forge(SharedMonsterAttributes.LUCK);
	public static final Holder<Attribute> SWIM_SPEED = forge(EntityPlayer.SWIM_SPEED);
	public static final Holder<Attribute> ENTITY_REACH = forge(EntityPlayer.REACH_DISTANCE);
	public static final Holder<Attribute> BLOCK_INTERACTION_RANGE = ENTITY_REACH;
	/** 1.21 entity reach: 1.12.2 has one reach attribute (Forge's), for blocks and entities alike. */
	public static final Holder<Attribute> ENTITY_INTERACTION_RANGE = ENTITY_REACH;
	/** Entity#stepHeight in 1.12.2. */
	public static final Holder<Attribute> STEP_HEIGHT = Holder.direct(new Attribute(null, "generic.step_height", 0.6D));
	// 1.21 attributes 1.12.2 doesn't have: their default value
	public static final Holder<Attribute> SAFE_FALL_DISTANCE = Holder.direct(new Attribute(null, "generic.safe_fall_distance", 3.0D));
	public static final Holder<Attribute> FALL_DAMAGE_MULTIPLIER = Holder.direct(new Attribute(null, "generic.fall_damage_multiplier", 1.0D));
	public static final Holder<Attribute> GRAVITY = Holder.direct(new Attribute(null, "generic.gravity", 0.08D));
	public static final Holder<Attribute> JUMP_STRENGTH = Holder.direct(new Attribute(null, "generic.jump_strength", 0.42D));
	public static final Holder<Attribute> SCALE = Holder.direct(new Attribute(null, "generic.scale", 1.0D));
	public static final Holder<Attribute> ATTACK_KNOCKBACK = Holder.direct(new Attribute(null, "generic.attack_knockback", 0.0D));
	public static final Holder<Attribute> MINING_EFFICIENCY = Holder.direct(new Attribute(null, "player.mining_efficiency", 0.0D));
	public static final Holder<Attribute> BLOCK_BREAK_SPEED = Holder.direct(new Attribute(null, "player.block_break_speed", 1.0D));
	public static final Holder<Attribute> SUBMERGED_MINING_SPEED = Holder.direct(new Attribute(null, "player.submerged_mining_speed", 0.2D));
	public static final Holder<Attribute> SNEAKING_SPEED = Holder.direct(new Attribute(null, "player.sneaking_speed", 0.3D));
	public static final Holder<Attribute> OXYGEN_BONUS = Holder.direct(new Attribute(null, "generic.oxygen_bonus", 0.0D));
	public static final Holder<Attribute> MAX_ABSORPTION = Holder.direct(new Attribute(null, "generic.max_absorption", 0.0D));
	public static final Holder<Attribute> BURNING_TIME = Holder.direct(new Attribute(null, "generic.burning_time", 1.0D));
	public static final Holder<Attribute> EXPLOSION_KNOCKBACK_RESISTANCE = Holder.direct(new Attribute(null, "generic.explosion_knockback_resistance", 0.0D));
	public static final Holder<Attribute> MOVEMENT_EFFICIENCY = Holder.direct(new Attribute(null, "generic.movement_efficiency", 0.0D));
	public static final Holder<Attribute> WATER_MOVEMENT_EFFICIENCY = Holder.direct(new Attribute(null, "generic.water_movement_efficiency", 0.0D));
	public static final Holder<Attribute> SWEEPING_DAMAGE_RATIO = Holder.direct(new Attribute(null, "player.sweeping_damage_ratio", 0.0D));
	/** NeoForge's creative flight: the player's allowFlying capability in 1.12.2. */
	public static final Holder<Attribute> CREATIVE_FLIGHT = Holder.direct(new Attribute(null, "neoforge.creative_flight", 0.0D));

	private Attributes() {
	}

	private static Holder<Attribute> forge(net.minecraft.entity.ai.attributes.IAttribute attribute) {
		return Holder.direct(new Attribute(attribute, attribute.getName(), attribute.getDefaultValue()));
	}
}
