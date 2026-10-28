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

	public void addOrReplacePermanentModifier(AttributeModifier modifier) {
		this.removeModifier(modifier.id());
		this.addPermanentModifier(modifier);
	}

	public void removeModifier(ResourceLocation id) {
		if (this.forge != null) {
			this.forge.removeModifier(AttributeModifier.uuidOf(id));
		} else if (this.virtual().remove(id) != null) {
			this.applyVirtual();
		}
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
