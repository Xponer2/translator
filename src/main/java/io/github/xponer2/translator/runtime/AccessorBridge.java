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
package io.github.xponer2.translator.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

/**
 * What a mod's accessor mixin into a Translator class does, by reflection ({@link AccessorMixins} rewrites the
 * accessor's methods to call this). The Translator class is built differently from the 1.21 class: a field the
 * accessor names may not be there, then the value lives beside the object (what the mod sets, it reads back).
 */
public final class AccessorBridge {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator Runtime");
	private static final Map<String, Optional<Field>> FIELDS = new ConcurrentHashMap<>();
	private static final Map<String, Optional<Method>> METHODS = new ConcurrentHashMap<>();
	/** Fields the Translator class lacks, per object (weak: they go with it) and per class for static ones. */
	private static final Map<Object, Map<String, Object>> VIRTUAL = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

	private AccessorBridge() {
	}

	/** An accessor's getter: the field (one of {@code names}, comma-separated) of the target, or of the class when static. */
	@Nullable
	public static Object get(@Nullable Object target, String owner, String names) {
		Field field = field(target, owner, names);
		if (field != null) {
			try {
				return field.get(Modifier.isStatic(field.getModifiers()) ? null : target);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				report(owner, names, e);
			}
		}
		return virtual(target, owner).get(names);
	}

	/** An accessor's setter. */
	public static void set(@Nullable Object target, String owner, String names, @Nullable Object value) {
		Field field = field(target, owner, names);
		if (field != null) {
			try {
				if (Modifier.isFinal(field.getModifiers())) {
					unfinal(field);
				}
				field.set(Modifier.isStatic(field.getModifiers()) ? null : target, value);
				return;
			} catch (ReflectiveOperationException | IllegalArgumentException e) {
				report(owner, names, e);
			}
		}
		virtual(target, owner).put(names, value);
	}

	/** An invoker: the method (one of {@code names}) with that many arguments, or the constructor for {@code <init>}. */
	@Nullable
	public static Object invoke(@Nullable Object target, String owner, String names, Object[] args) {
		Class<?> type = type(target, owner);
		if (type == null) {
			return null;
		}
		try {
			if ("<init>".equals(names)) {
				for (Constructor<?> constructor : type.getDeclaredConstructors()) {
					if (constructor.getParameterCount() == args.length) {
						constructor.setAccessible(true);
						return constructor.newInstance(args);
					}
				}
				report(owner, names, null);
				return null;
			}
			Method method = METHODS.computeIfAbsent(type.getName() + "#" + names + "/" + args.length,
					key -> Optional.ofNullable(findMethod(type, names, args.length))).orElse(null);
			if (method == null) {
				report(owner, names, null);
				return null;
			}
			return method.invoke(Modifier.isStatic(method.getModifiers()) ? null : target, args);
		} catch (java.lang.reflect.InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException runtime) {
				throw runtime;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			report(owner, names, cause);
			return null;
		} catch (ReflectiveOperationException | IllegalArgumentException e) {
			report(owner, names, e);
			return null;
		}
	}

	// the accessor's primitive results: a field the Translator class lacks reads as 0 / false until set

	public static int intValue(@Nullable Object value) {
		return value instanceof Number number ? number.intValue() : value instanceof Character c ? c : 0;
	}

	public static long longValue(@Nullable Object value) {
		return value instanceof Number number ? number.longValue() : 0L;
	}

	public static float floatValue(@Nullable Object value) {
		return value instanceof Number number ? number.floatValue() : 0.0F;
	}

	public static double doubleValue(@Nullable Object value) {
		return value instanceof Number number ? number.doubleValue() : 0.0D;
	}

	public static boolean booleanValue(@Nullable Object value) {
		return value instanceof Boolean flag && flag;
	}

	public static byte byteValue(@Nullable Object value) {
		return value instanceof Number number ? number.byteValue() : 0;
	}

	public static short shortValue(@Nullable Object value) {
		return value instanceof Number number ? number.shortValue() : 0;
	}

	public static char charValue(@Nullable Object value) {
		return value instanceof Character c ? c : value instanceof Number number ? (char) number.intValue() : 0;
	}

	// --- lookup ---------------------------------------------------------------------------------------------------

	@Nullable
	private static Class<?> type(@Nullable Object target, String owner) {
		if (target != null) {
			return target.getClass();
		}
		try {
			return Class.forName(owner.replace('/', '.'), false, AccessorBridge.class.getClassLoader());
		} catch (ClassNotFoundException | LinkageError e) {
			report(owner, "", e);
			return null;
		}
	}

	@Nullable
	private static Field field(@Nullable Object target, String owner, String names) {
		Class<?> type = type(target, owner);
		if (type == null) {
			return null;
		}
		return FIELDS.computeIfAbsent(type.getName() + "#" + names, key -> {
			for (Class<?> current = type; current != null; current = current.getSuperclass()) {
				for (String name : names.split(",")) {
					try {
						Field found = current.getDeclaredField(name);
						found.setAccessible(true);
						return Optional.of(found);
					} catch (NoSuchFieldException | RuntimeException e) {
						// another name, or the superclass
					}
				}
				// the release game names Minecraft's fields by SRG: their MCP names compared
				for (Field declared : current.getDeclaredFields()) {
					if (declared.getName().startsWith("field_") && java.util.Arrays.asList(names.split(",")).contains(Mappings.mcpField(declared.getName()))) {
						declared.setAccessible(true);
						return Optional.of(declared);
					}
				}
			}
			LOGGER.info("Translator: {} has no field {}: the mod's accessor keeps the value beside the object", type.getName(), names);
			return Optional.empty();
		}).orElse(null);
	}

	@Nullable
	private static Method findMethod(Class<?> type, String names, int count) {
		for (Class<?> current = type; current != null; current = current.getSuperclass()) {
			for (String name : names.split(",")) {
				for (Method method : current.getDeclaredMethods()) {
					if ((method.getName().equals(name) || method.getName().startsWith("func_") && name.equals(Mappings.mcpMethod(method.getName())))
							&& method.getParameterCount() == count) {
						method.setAccessible(true);
						return method;
					}
				}
			}
		}
		return null;
	}

	private static Map<String, Object> virtual(@Nullable Object target, String owner) {
		synchronized (VIRTUAL) {
			// a plain map: a field may hold null
			return VIRTUAL.computeIfAbsent(target != null ? target : owner.intern(), key -> Collections.synchronizedMap(new java.util.HashMap<>()));
		}
	}

	private static void unfinal(Field field) throws ReflectiveOperationException {
		Field modifiers = Field.class.getDeclaredField("modifiers");
		modifiers.setAccessible(true);
		modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
	}

	private static void report(String owner, String names, @Nullable Throwable error) {
		if (REPORTED.add(owner + "#" + names)) {
			LOGGER.warn("Translator: a mod's accessor of {} {} doesn't work on 1.12.2 yet{}", owner, names,
					error != null ? " (" + error + ")" : "");
		}
	}
}
