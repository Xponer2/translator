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
package io.github.xponer2.translator.menu;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * The 1.21 calls of a menu's click (a mod's own {@code doClick}: Sophisticated Core) on 1.12.2's Slot and ItemStack
 * (runtime, missing calls on them come here): taking from a slot, and the item hooks of one stack clicked onto
 * another ({@code overrideStackedOnOther}, {@code overrideOtherStackedOnMe}: a backpack takes the item it is clicked
 * with), asked of the item's own 1.21 methods.
 */
public final class ClickBridges {
	private static final Map<Class<?>, Optional<Method>> STACKED_ON_OTHER = new ConcurrentHashMap<>();
	private static final Map<Class<?>, Optional<Method>> OTHER_STACKED_ON_ME = new ConcurrentHashMap<>();

	private ClickBridges() {
	}

	/** 1.21 {@code slot.tryRemove(count, max, player)}: what the player may take, taken (empty when nothing). */
	public static Optional<ItemStack> tryRemove(Slot slot, int count, int max, EntityPlayer player) {
		if (!slot.canTakeStack(player)) {
			return Optional.empty();
		}
		// a slot the player may only take from (1.21 allowModification): all of the stack or nothing
		if (!slot.isItemValid(slot.getStack()) && max < slot.getStack().getCount()) {
			return Optional.empty();
		}
		count = Math.min(count, max);
		ItemStack taken = slot.decrStackSize(count);
		if (taken.isEmpty()) {
			return Optional.empty();
		}
		if (slot.getStack().isEmpty()) {
			slot.putStack(ItemStack.EMPTY);
		}
		return Optional.of(taken);
	}

	/** 1.21 {@code slot.setByPlayer(stack)}: the stack into the slot. */
	public static void setByPlayer(Slot slot, ItemStack stack) {
		slot.putStack(stack);
	}

	public static void setByPlayer(Slot slot, ItemStack stack, ItemStack previous) {
		slot.putStack(stack);
	}

	/** 1.21 {@code stack.overrideStackedOnOther(slot, action, player)}: the item's own 1.21 method, else false. */
	public static boolean overrideStackedOnOther(ItemStack stack, Slot slot, Object action, EntityPlayer player) {
		Method method = find(STACKED_ON_OTHER, stack.getItem(), "overrideStackedOnOther", 4);
		return method != null && call(method, stack.getItem(), stack, slot, action, player);
	}

	/** 1.21 {@code stack.overrideOtherStackedOnMe(other, slot, action, player, access)}: the item's own 1.21 method, else false. */
	public static boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, Object action, EntityPlayer player, SlotAccess access) {
		Method method = find(OTHER_STACKED_ON_ME, stack.getItem(), "overrideOtherStackedOnMe", 6);
		return method != null && call(method, stack.getItem(), stack, other, slot, action, player, access);
	}

	@Nullable
	private static Method find(Map<Class<?>, Optional<Method>> cache, Item item, String name, int parameters) {
		return cache.computeIfAbsent(item.getClass(), type -> {
			for (Class<?> c = type; c != null && c != Item.class; c = c.getSuperclass()) {
				for (Method method : c.getDeclaredMethods()) {
					if (method.getName().equals(name) && method.getParameterCount() == parameters && method.getReturnType() == boolean.class) {
						method.setAccessible(true);
						return Optional.of(method);
					}
				}
			}
			return Optional.empty();
		}).orElse(null);
	}

	private static boolean call(Method method, Object item, Object... arguments) {
		try {
			return (Boolean) method.invoke(item, arguments);
		} catch (ReflectiveOperationException | RuntimeException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(method.getDeclaringClass().getName() + "." + method.getName(),
					e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e);
			return false;
		}
	}
}
