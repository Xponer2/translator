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
	/** Entity#stepHeight in 1.12.2. */
	public static final Holder<Attribute> STEP_HEIGHT = Holder.direct(new Attribute(null, "generic.step_height", 0.6D));
	/** NeoForge's creative flight: the player's allowFlying capability in 1.12.2. */
	public static final Holder<Attribute> CREATIVE_FLIGHT = Holder.direct(new Attribute(null, "neoforge.creative_flight", 0.0D));

	private Attributes() {
	}

	private static Holder<Attribute> forge(net.minecraft.entity.ai.attributes.IAttribute attribute) {
		return Holder.direct(new Attribute(attribute, attribute.getName(), attribute.getDefaultValue()));
	}
}
