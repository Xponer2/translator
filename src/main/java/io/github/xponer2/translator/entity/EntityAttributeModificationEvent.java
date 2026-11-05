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

import java.util.ArrayList;
import java.util.List;

import io.github.xponer2.translator.registry.EntityType;
import io.github.xponer2.translator.registry.Holder;

import net.minecraftforge.fml.common.eventhandler.Event;

/** NeoForge {@code EntityAttributeModificationEvent} (mod bus): attributes added to entity types (the mods' and vanilla). */
public class EntityAttributeModificationEvent extends Event {
	public void add(EntityType<?> type, Holder<Attribute> attribute, double value) {
		AttributeSupplier.add(type, attribute, value);
	}

	public void add(EntityType<?> type, Holder<Attribute> attribute) {
		AttributeSupplier.add(type, attribute, attribute.value().getDefaultValue());
	}

	public boolean has(EntityType<?> type, Holder<Attribute> attribute) {
		AttributeSupplier supplier = AttributeSupplier.of(type);
		return supplier != null && supplier.hasAttribute(attribute);
	}

	/** The types with attributes (1.12.2's vanilla entities keep theirs). */
	public List<EntityType<?>> getTypes() {
		return new ArrayList<>();
	}
}
