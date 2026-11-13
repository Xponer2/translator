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
package io.github.xponer2.translator.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.xponer2.translator.entity.Attribute;
import io.github.xponer2.translator.entity.AttributeModifier;
import io.github.xponer2.translator.entity.EquipmentSlotGroup;
import io.github.xponer2.translator.registry.Holder;

import net.minecraft.util.ResourceLocation;

/**
 * The attribute modifiers of an item (1.21 {@code ItemAttributeModifiers}, neoforge-1.21.1-runtime.tsv): a tool's
 * attack damage and speed ({@code createAttributes}), and its 1.21 entries (attribute, modifier, slot group), kept for
 * the 1.21 item facades and the mods that read them; their codec reads them from data packs (Twilight Forest's
 * travellers' gear modifiers).
 */
public final class ItemAttributeModifiers {
	public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(0.0F, 0.0F);

	private final float attackDamage;
	private final float attackSpeed;
	private final List<Entry> entries;
	private final boolean showInTooltip;

	public ItemAttributeModifiers(float attackDamage, float attackSpeed) {
		this(attackDamage, attackSpeed, new ArrayList<>(), true);
	}

	/** 1.21 {@code new ItemAttributeModifiers(modifiers, showInTooltip)}. */
	public ItemAttributeModifiers(List<Entry> modifiers, boolean showInTooltip) {
		this(0.0F, 0.0F, new ArrayList<>(modifiers), showInTooltip);
	}

	private ItemAttributeModifiers(float attackDamage, float attackSpeed, List<Entry> entries, boolean showInTooltip) {
		this.attackDamage = attackDamage;
		this.attackSpeed = attackSpeed;
		this.entries = entries;
		this.showInTooltip = showInTooltip;
	}

	public float attackDamage() {
		return this.attackDamage;
	}

	public float attackSpeed() {
		return this.attackSpeed;
	}

	/** 1.21 {@code modifiers()}: the entries. */
	public List<Entry> modifiers() {
		return Collections.unmodifiableList(this.entries);
	}

	/** The modifiers and one more (1.21: one of the same attribute and id goes first). */
	public ItemAttributeModifiers withModifierAdded(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
		var entries = new ArrayList<Entry>(this.entries.size() + 1);
		for (Entry entry : this.entries) {
			if (!entry.matches(attribute, modifier.id())) {
				entries.add(entry);
			}
		}
		entries.add(new Entry(attribute, modifier, slot));
		return new ItemAttributeModifiers(this.attackDamage, this.attackSpeed, entries, this.showInTooltip);
	}

	/** The same for callers the converter typed loosely. */
	@SuppressWarnings("unchecked")
	public ItemAttributeModifiers withModifierAdded(Object attribute, Object modifier, Object slot) {
		return this.withModifierAdded((Holder<Attribute>) attribute, (AttributeModifier) modifier, (EquipmentSlotGroup) slot);
	}

	public ItemAttributeModifiers withTooltip(boolean showInTooltip) {
		return new ItemAttributeModifiers(this.attackDamage, this.attackSpeed, this.entries, showInTooltip);
	}

	public boolean showInTooltip() {
		return this.showInTooltip;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final List<Entry> entries = new ArrayList<>();

		public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
			this.entries.add(new Entry(attribute, modifier, slot));
			return this;
		}

		@SuppressWarnings("unchecked")
		public Builder add(Object attribute, Object modifier, Object slot) {
			return this.add((Holder<Attribute>) attribute, (AttributeModifier) modifier, (EquipmentSlotGroup) slot);
		}

		public ItemAttributeModifiers build() {
			return new ItemAttributeModifiers(0.0F, 0.0F, this.entries, true);
		}
	}

	/** An attribute by its id in minecraft:attribute (1.21 {@code Attribute.CODEC}), looked up when used. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	static final Codec<Holder<Attribute>> ATTRIBUTE = new Codec<Holder<Attribute>>() {
		private Codec<Holder<Attribute>> codec() {
			var registry = io.github.xponer2.translator.util.BuiltInRegistries.byKey(io.github.xponer2.translator.util.Registries.ATTRIBUTE);
			return (Codec) registry.holderByNameCodec();
		}

		@Override
		public <T> DataResult<Pair<Holder<Attribute>, T>> decode(DynamicOps<T> ops, T input) {
			return this.codec().decode(ops, input).flatMap(pair -> pair.getFirst() == null || pair.getFirst().value() == null
					? DataResult.error("Unknown attribute " + input) : DataResult.success(pair));
		}

		@Override
		public <T> DataResult<T> encode(Holder<Attribute> input, DynamicOps<T> ops, T prefix) {
			return this.codec().encode(input, ops, prefix);
		}
	};

	/** 1.21 {@code AttributeModifier.MAP_CODEC}: id, amount, operation. */
	static final MapCodec<AttributeModifier> MODIFIER = RecordCodecBuilder.mapCodec(instance -> instance.group(
			io.github.xponer2.translator.util.Codecs.RESOURCE_LOCATION.fieldOf("id").forGetter(AttributeModifier::id),
			Codec.DOUBLE.fieldOf("amount").forGetter(AttributeModifier::amount),
			AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(AttributeModifier::operation)).apply(instance, AttributeModifier::new));

	/** 1.21 {@code ItemAttributeModifiers.Entry}: an attribute, its modifier, the slots it counts in. */
	public static final class Entry {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ATTRIBUTE.fieldOf("type").forGetter(Entry::attribute),
				MODIFIER.forGetter(Entry::modifier),
				EquipmentSlotGroup.CODEC.optionalFieldOf("slot", EquipmentSlotGroup.ANY).forGetter(Entry::slot)).apply(instance, Entry::new));

		private final Holder<Attribute> attribute;
		private final AttributeModifier modifier;
		private final EquipmentSlotGroup slot;

		public Entry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
			this.attribute = attribute;
			this.modifier = modifier;
			this.slot = slot;
		}

		public Holder<Attribute> attribute() {
			return this.attribute;
		}

		public AttributeModifier modifier() {
			return this.modifier;
		}

		public EquipmentSlotGroup slot() {
			return this.slot;
		}

		/** 1.21 {@code matches(attribute, id)}: the same attribute and modifier id. */
		public boolean matches(Holder<Attribute> attribute, ResourceLocation id) {
			return java.util.Objects.equals(this.attribute, attribute) && this.modifier.id().equals(id);
		}

		@Override
		public String toString() {
			return "Entry[" + this.attribute + ", " + this.modifier.id() + ", " + this.slot + "]";
		}
	}
}
