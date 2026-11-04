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

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.Holder;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21 attribute instance of an entity: modifiers by id. Real attributes go to the 1.12.2 instance; step height and
 * creative flight keep their modifiers here and set the entity field / player capability.
 */
public final class AttributeInstance {
	private static final Map<Entity, Map<String, Map<ResourceLocation, AttributeModifier>>> VIRTUAL = new WeakHashMap<>();

	private final EntityLivingBase entity;
	private final Attribute attribute;
	@Nullable
	private final IAttributeInstance forge;

	private AttributeInstance(EntityLivingBase entity, Attribute attribute, @Nullable IAttributeInstance forge) {
		this.entity = entity;
		this.attribute = attribute;
		this.forge = forge;
	}

	/** 1.21 entity.getAttribute(attribute): null when the entity doesn't have it. */
	@Nullable
	public static AttributeInstance of(EntityLivingBase entity, Holder<Attribute> holder) {
		var attribute = holder.value();
		if (attribute.forge() == null) {
			return new AttributeInstance(entity, attribute, null);
		}

		var instance = entity.getEntityAttribute(attribute.forge());
		return instance == null ? null : new AttributeInstance(entity, attribute, instance);
	}

	/**
	 * 1.21 {@code attributeMap.getInstance(attribute)}: the map has no entity, so only an attribute 1.12.2 has (its
	 * own instance in the map); null otherwise, or when the map lacks it.
	 */
	@Nullable
	public static AttributeInstance of(net.minecraft.entity.ai.attributes.AbstractAttributeMap map, Holder<Attribute> holder) {
		var attribute = holder.value();
		if (attribute.forge() == null) {
			return null;
		}
		var instance = map.getAttributeInstance(attribute.forge());
		return instance == null ? null : new AttributeInstance(null, attribute, instance);
	}

	private Map<ResourceLocation, AttributeModifier> virtual() {
		return VIRTUAL.computeIfAbsent(this.entity, e -> new HashMap<>()).computeIfAbsent(this.attribute.name(), n -> new HashMap<>());
	}

	public double getBaseValue() {
		return this.forge != null ? this.forge.getBaseValue() : this.attribute.getDefaultValue();
	}

	public double getValue() {
		if (this.forge != null) {
			return this.forge.getAttributeValue();
		}

		double base = this.attribute.getDefaultValue();
		double value = base;
		for (var modifier : this.virtual().values()) {
			value += switch (modifier.operation()) {
				case ADD_VALUE -> modifier.amount();
				case ADD_MULTIPLIED_BASE -> base * modifier.amount();
				case ADD_MULTIPLIED_TOTAL -> 0;
			};
		}
		for (var modifier : this.virtual().values()) {
			if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
				value *= 1 + modifier.amount();
			}
		}
		return value;
	}

	@Nullable
	public AttributeModifier getModifier(ResourceLocation id) {
		if (this.forge != null) {
			var modifier = this.forge.getModifier(AttributeModifier.uuidOf(id));
			return modifier == null ? null : new AttributeModifier(id, modifier.getAmount(), AttributeModifier.Operation.values()[modifier.getOperation()]);
		}

		return this.virtual().get(id);
	}

	public boolean hasModifier(ResourceLocation id) {
		return this.getModifier(id) != null;
	}

	public void addTransientModifier(AttributeModifier modifier) {
		if (this.forge != null) {
			this.forge.applyModifier(modifier.forge());
		} else {
			this.virtual().put(modifier.id(), modifier);
			this.applyVirtual();
		}
	}

	public void addPermanentModifier(AttributeModifier modifier) {
		// 1.12.2 saves the modifiers of the attribute map; the augments re-add theirs every tick anyway
		this.addTransientModifier(modifier);
	}

	/** 1.21 {@code addOrUpdateTransientModifier}: the modifier of that id replaced. */
	public void addOrUpdateTransientModifier(AttributeModifier modifier) {
		this.removeModifier(modifier.id());
		this.addTransientModifier(modifier);
	}

	public void addOrReplacePermanentModifier(AttributeModifier modifier) {
		this.removeModifier(modifier.id());
		this.addPermanentModifier(modifier);
	}

	/** Removes the modifier of that id (the ports call this one). */
	public void removeModifier(ResourceLocation id) {
		this.removeModifierById(id);
	}

	/**
	 * 1.21 {@code removeModifier(id)} as original 1.21.1 mods call it (it returns whether the attribute had it, 8 mods
	 * test that): M line of the runtime table.
	 */
	public boolean removeModifierById(ResourceLocation id) {
		if (this.forge != null) {
			boolean had = this.forge.getModifier(AttributeModifier.uuidOf(id)) != null;
			this.forge.removeModifier(AttributeModifier.uuidOf(id));
			return had;
		}
		if (this.virtual().remove(id) != null) {
			this.applyVirtual();
			return true;
		}
		return false;
	}

	public void removeModifier(AttributeModifier modifier) {
		this.removeModifier(modifier.id());
	}

	private void applyVirtual() {
		if (this.attribute == Attributes.STEP_HEIGHT.value()) {
			this.entity.stepHeight = (float) this.getValue();
		} else if (this.attribute == Attributes.CREATIVE_FLIGHT.value() && this.entity instanceof EntityPlayer player
				&& !player.isCreative() && !player.isSpectator()) {
			boolean flight = this.getValue() > 0;
			player.capabilities.allowFlying = flight;
			if (!flight) {
				player.capabilities.isFlying = false;
			}
			player.sendPlayerAbilities();
		}
	}
}
