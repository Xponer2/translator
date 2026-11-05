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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.EntityType;
import io.github.xponer2.translator.registry.Holder;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

/**
 * 1.21 {@code AttributeSupplier}: the base values of a living entity type's attributes, which the mods build from
 * {@code Monster.createMonsterAttributes()} and register in EntityAttributeCreationEvent. Applied when an entity of
 * the type is made (1.12.2 registers attributes per entity, in applyEntityAttributes).
 */
public final class AttributeSupplier {
	private static final Map<EntityType<?>, AttributeSupplier> TYPES = new ConcurrentHashMap<>();

	private final Map<Holder<Attribute>, Double> values;

	AttributeSupplier(Map<Holder<Attribute>, Double> values) {
		this.values = values;
	}

	public static Builder builder() {
		return new Builder();
	}

	public double getBaseValue(Holder<Attribute> attribute) {
		Double value = this.values.get(attribute);
		return value != null ? value : attribute.value().getDefaultValue();
	}

	public double getValue(Holder<Attribute> attribute) {
		return this.getBaseValue(attribute);
	}

	public boolean hasAttribute(Holder<Attribute> attribute) {
		return this.values.containsKey(attribute);
	}

	/** The type's attributes (EntityAttributeCreationEvent.put), with what modification events add. */
	public static void put(EntityType<?> type, AttributeSupplier supplier) {
		TYPES.put(type, supplier);
	}

	public static void add(EntityType<?> type, Holder<Attribute> attribute, double value) {
		TYPES.compute(type, (key, old) -> {
			var values = new LinkedHashMap<Holder<Attribute>, Double>();
			if (old != null) {
				values.putAll(old.values);
			}
			values.put(attribute, value);
			return new AttributeSupplier(values);
		});
	}

	public static boolean has(EntityType<?> type) {
		return TYPES.containsKey(type);
	}

	@Nullable
	public static AttributeSupplier of(EntityType<?> type) {
		return TYPES.get(type);
	}

	/** The type's attributes on a new entity of it: registered where 1.12.2 lacks them, base values set, health full. */
	public static void apply(EntityLivingBase entity, @Nullable EntityType<?> type) {
		AttributeSupplier supplier = type == null ? null : TYPES.get(type);
		if (supplier == null) {
			return;
		}
		for (var entry : supplier.values.entrySet()) {
			Object value = entry.getKey().value();
			IAttribute forge = value instanceof Attribute attribute ? attribute.forge() : value instanceof IAttribute plain ? plain : null;
			if (forge == null) {
				continue;
			}
			IAttributeInstance instance = entity.getAttributeMap().getAttributeInstance(forge);
			if (instance == null) {
				instance = entity.getAttributeMap().registerAttribute(forge);
			}
			instance.setBaseValue(entry.getValue());
		}
		entity.setHealth(entity.getMaxHealth());
	}

	/** 1.21 {@code AttributeSupplier.Builder}. */
	public static final class Builder {
		private final Map<Holder<Attribute>, Double> values = new LinkedHashMap<>();

		public Builder add(Holder<Attribute> attribute) {
			this.values.put(attribute, attribute.value().getDefaultValue());
			return this;
		}

		public Builder add(Holder<Attribute> attribute, double value) {
			this.values.put(attribute, value);
			return this;
		}

		public Builder combine(Builder other) {
			this.values.putAll(other.values);
			return this;
		}

		public boolean hasAttribute(Holder<Attribute> attribute) {
			return this.values.containsKey(attribute);
		}

		public AttributeSupplier build() {
			return new AttributeSupplier(new LinkedHashMap<>(this.values));
		}
	}

	// --- the vanilla bases (LivingEntity.createLivingAttributes, Mob.createMobAttributes, Monster.createMonsterAttributes)

	public static Builder living() {
		return builder().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.0D).add(Attributes.MOVEMENT_SPEED, 0.7D)
				.add(Attributes.ARMOR, 0.0D).add(Attributes.ARMOR_TOUGHNESS, 0.0D).add(Attributes.STEP_HEIGHT).add(Attributes.SCALE)
				.add(Attributes.GRAVITY).add(Attributes.SAFE_FALL_DISTANCE).add(Attributes.FALL_DAMAGE_MULTIPLIER).add(Attributes.JUMP_STRENGTH)
				.add(Attributes.BURNING_TIME).add(Attributes.MAX_ABSORPTION).add(Attributes.MOVEMENT_EFFICIENCY);
	}

	public static Builder mob() {
		return living().add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.ATTACK_KNOCKBACK);
	}

	public static Builder monster() {
		return mob().add(Attributes.ATTACK_DAMAGE);
	}
}
