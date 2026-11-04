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

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;

/**
 * 1.21 item capability on a 1.12.2 Forge capability (no context: 1.12.2 asks items with a null side).
 */
public final class ItemCapability<T, C> {
	private final Supplier<Capability<T>> capability;

	ItemCapability(Supplier<Capability<T>> capability) {
		this.capability = capability;
	}

	/** 1.21 {@code ItemCapability.create}: a capability of the mod's own (Curios' curio items). */
	@SuppressWarnings("unchecked")
	public static <T, C> ItemCapability<T, C> create(net.minecraft.util.ResourceLocation name, Class<T> type, Class<C> context) {
		return (ItemCapability<T, C>) CustomCapabilities.CREATED.computeIfAbsent("item " + name,
				key -> new ItemCapability<T, C>(() -> CustomCapabilities.forge(name)));
	}

	public static <T> ItemCapability<T, Void> createVoid(net.minecraft.util.ResourceLocation name, Class<T> type) {
		return create(name, type, Void.class);
	}

	@Nullable
	public Capability<T> forge() {
		return this.capability.get();
	}

	/** 1.21 stack.getCapability(capability) (the converter writes capability.getCapability(stack)). */
	@Nullable
	public T getCapability(ItemStack stack) {
		return this.getCapability(stack, null);
	}

	@Nullable
	public T getCapability(ItemStack stack, @Nullable C context) {
		var cap = this.forge();
		return cap != null && !stack.isEmpty() && stack.hasCapability(cap, null) ? stack.getCapability(cap, null) : null;
	}
}
