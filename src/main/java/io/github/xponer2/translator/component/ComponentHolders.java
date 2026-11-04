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
package io.github.xponer2.translator.component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

/**
 * Calls through the 1.21 component holder interfaces (DataComponentHolder, NeoForge's MutableDataComponentHolder and
 * IDataComponentHolderExtension): the 1.12.2 ItemStack doesn't implement them, so a call through one on a stack was
 * an IncompatibleClassChangeError (Sophisticated Storage's item names). The converter makes those calls static ones
 * here: a stack's components are {@link StackComponents}', any other holder (a FluidStack) answers itself.
 */
public final class ComponentHolders {
	private static final Map<String, Optional<Method>> METHODS = new ConcurrentHashMap<>();

	private ComponentHolders() {
	}

	@Nullable
	public static Object get(Object holder, DataComponentType<?> type) {
		return holder instanceof ItemStack stack ? StackComponents.get(stack, type) : call(holder, "get", new Class<?>[] {DataComponentType.class}, type);
	}

	@Nullable
	public static Object get(Object holder, Supplier<?> type) {
		return get(holder, (DataComponentType<?>) type.get());
	}

	@Nullable
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Object getOrDefault(Object holder, DataComponentType<?> type, Object defaultValue) {
		if (holder instanceof ItemStack stack) {
			return StackComponents.getOrDefault(stack, (DataComponentType) type, defaultValue);
		}
		Object value = get(holder, type);
		return value != null ? value : defaultValue;
	}

	@Nullable
	public static Object getOrDefault(Object holder, Supplier<?> type, Object defaultValue) {
		return getOrDefault(holder, (DataComponentType<?>) type.get(), defaultValue);
	}

	public static boolean has(Object holder, DataComponentType<?> type) {
		return holder instanceof ItemStack stack ? StackComponents.has(stack, type) : get(holder, type) != null;
	}

	public static boolean has(Object holder, Supplier<?> type) {
		return has(holder, (DataComponentType<?>) type.get());
	}

	@Nullable
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Object set(Object holder, DataComponentType<?> type, @Nullable Object value) {
		return holder instanceof ItemStack stack ? StackComponents.set(stack, (DataComponentType) type, value)
				: call(holder, "set", new Class<?>[] {DataComponentType.class, Object.class}, type, value);
	}

	@Nullable
	public static Object set(Object holder, Supplier<?> type, @Nullable Object value) {
		return set(holder, (DataComponentType<?>) type.get(), value);
	}

	@Nullable
	public static Object remove(Object holder, DataComponentType<?> type) {
		return holder instanceof ItemStack stack ? StackComponents.remove(stack, type) : call(holder, "remove", new Class<?>[] {DataComponentType.class}, type);
	}

	@Nullable
	public static Object remove(Object holder, Supplier<?> type) {
		return remove(holder, (DataComponentType<?>) type.get());
	}

	@Nullable
	public static Object getComponents(Object holder) {
		return holder instanceof ItemStack stack ? StackComponents.getComponents(stack) : call(holder, "getComponents", new Class<?>[0]);
	}

	/** The holder's own method (a carried class implementing the interface): the exact overload, by its parameter types. */
	@Nullable
	private static Object call(Object holder, String name, Class<?>[] types, Object... args) {
		Method method = METHODS.computeIfAbsent(holder.getClass().getName() + "#" + name + "/" + types.length, key -> {
			try {
				Method found = holder.getClass().getMethod(name, types);
				found.setAccessible(true);
				return Optional.of(found);
			} catch (NoSuchMethodException | RuntimeException e) {
				return Optional.empty();
			}
		}).orElse(null);
		if (method == null) {
			return null;
		}
		try {
			return method.invoke(holder, args);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException runtime) {
				throw runtime;
			}
			if (e.getCause() instanceof Error error) {
				throw error;
			}
			return null;
		} catch (IllegalAccessException e) {
			return null;
		}
	}
}
