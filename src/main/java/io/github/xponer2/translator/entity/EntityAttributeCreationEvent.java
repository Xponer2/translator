/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.entity;

import io.github.xponer2.translator.registry.EntityType;

import net.minecraftforge.fml.common.eventhandler.Event;

/** NeoForge {@code EntityAttributeCreationEvent} (mod bus): the attributes of the mods' living entity types. */
public class EntityAttributeCreationEvent extends Event {
	public void put(EntityType<?> type, AttributeSupplier supplier) {
		AttributeSupplier.put(type, supplier);
	}

	public void put(EntityType<?> type, AttributeSupplier.Builder builder) {
		AttributeSupplier.put(type, builder.build());
	}

	private static boolean fired;

	/** Once, after the registries (the mods' types are there). */
	public static synchronized void fire() {
		if (!fired) {
			fired = true;
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new EntityAttributeCreationEvent());
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new EntityAttributeModificationEvent());
		}
	}
}
