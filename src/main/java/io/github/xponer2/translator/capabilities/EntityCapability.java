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
package io.github.xponer2.translator.capabilities;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

/**
 * 1.21 entity capability (Curios' inventory of a player) on a 1.12.2 Forge capability. Providers registered in
 * {@link RegisterCapabilitiesEvent#registerEntity} are attached to the entities of their type.
 */
public final class EntityCapability<T, C> {
	private final Supplier<Capability<T>> capability;

	EntityCapability(Supplier<Capability<T>> capability) {
		this.capability = capability;
	}

	@SuppressWarnings("unchecked")
	public static <T, C> EntityCapability<T, C> create(ResourceLocation name, Class<T> type, Class<C> context) {
		return (EntityCapability<T, C>) CustomCapabilities.CREATED.computeIfAbsent("entity " + name,
				key -> new EntityCapability<T, C>(() -> CustomCapabilities.forge(name)));
	}

	public static <T> EntityCapability<T, Void> createVoid(ResourceLocation name, Class<T> type) {
		return create(name, type, Void.class);
	}

	public static <T> EntityCapability<T, EnumFacing> createSided(ResourceLocation name, Class<T> type) {
		return create(name, type, EnumFacing.class);
	}

	@Nullable
	public Capability<T> forge() {
		return this.capability.get();
	}

	/** 1.21 {@code entity.getCapability(capability)}. */
	@Nullable
	public T getCapability(Entity entity) {
		return this.getCapability(entity, null);
	}

	@Nullable
	public T getCapability(Entity entity, @Nullable C context) {
		var cap = this.forge();
		EnumFacing side = context instanceof EnumFacing facing ? facing : null;
		return cap != null && entity != null && entity.hasCapability(cap, side) ? entity.getCapability(cap, side) : null;
	}
}
