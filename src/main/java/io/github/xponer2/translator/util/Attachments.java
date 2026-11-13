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
package io.github.xponer2.translator.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * NeoForge data attachments ({@code entity.getData(TYPE)}, also on block entities and levels) on 1.12.2: the values
 * of each holder by attachment type, while the holder lives. The type is the mod's own NeoForge {@code AttachmentType}
 * (carried with the API, or a DeferredHolder of one); its default value comes from its default value supplier.
 */
public final class Attachments {
	private static final Map<Object, Map<Object, Object>> VALUES = Collections.synchronizedMap(new WeakHashMap<>());

	private Attachments() {
	}

	/** The attachment type itself: a holder (DeferredHolder, a Supplier) gives its value. */
	private static Object type(Object type) {
		return type instanceof Supplier<?> supplier && !type.getClass().getName().startsWith("net.neoforged.neoforge.attachment.") ? supplier.get() : type;
	}

	private static Map<Object, Object> of(Object holder) {
		return VALUES.computeIfAbsent(holder, k -> new HashMap<>());
	}

	public static Object get(Object holder, Object type) {
		Object key = type(type);
		var values = of(holder);
		synchronized (values) {
			Object value = values.get(key);
			if (value == null) {
				value = defaultValue(key);
				if (value != null) {
					values.put(key, value);
				}
			}
			return value;
		}
	}

	@Nullable
	public static Object set(Object holder, Object type, Object value) {
		var values = of(holder);
		synchronized (values) {
			return values.put(type(type), value);
		}
	}

	/** Whether the holder has any attachment. */
	public static boolean any(Object holder) {
		var values = VALUES.get(holder);
		return values != null && !values.isEmpty();
	}

	public static boolean has(Object holder, Object type) {
		var values = VALUES.get(holder);
		return values != null && values.containsKey(type(type));
	}

	@Nullable
	public static Object remove(Object holder, Object type) {
		var values = VALUES.get(holder);
		return values == null ? null : values.remove(type(type));
	}

	/** The default value of an AttachmentType: its {@code defaultValueSupplier}, given no holder. */
	@Nullable
	@SuppressWarnings("unchecked")
	private static Object defaultValue(Object type) {
		for (Class<?> c = type.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
			try {
				var field = c.getDeclaredField("defaultValueSupplier");
				field.setAccessible(true);
				Object supplier = field.get(type);
				if (supplier instanceof Function<?, ?> function) {
					return ((Function<Object, Object>) function).apply(null);
				}
				if (supplier instanceof Supplier<?> plain) {
					return plain.get();
				}
				return null;
			} catch (NoSuchFieldException e) {
				// a superclass may declare it
			} catch (ReflectiveOperationException | RuntimeException e) {
				io.github.xponer2.translator.runtime.RuntimeStubs.failed("the default value of the attachment " + type, e);
				return null;
			}
		}
		return null;
	}
}
