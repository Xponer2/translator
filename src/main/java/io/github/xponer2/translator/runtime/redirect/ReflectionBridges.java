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
package io.github.xponer2.translator.runtime.redirect;

import java.lang.reflect.Method;

/**
 * NeoForge's {@code ObfuscationReflectionHelper.findMethod} for the classes that are the Translator's or 1.12.2's here:
 * the method as NeoForge finds it, else the one of that name whose parameters take the asked types (a Translator
 * method declares an interface where NeoForge's names FML's own class: {@code ModConfig.setConfig(LoadedConfig, ..)}).
 * The exact lookup alone threw in a mod's static initializer and left the whole class without its fields (Moonlight's
 * configs).
 */
public final class ReflectionBridges {
	private ReflectionBridges() {
	}

	public static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
		try {
			Method exact = owner.getDeclaredMethod(name, parameterTypes);
			exact.setAccessible(true);
			return exact;
		} catch (NoSuchMethodException missing) {
			for (Method candidate : owner.getDeclaredMethods()) {
				if (candidate.getName().equals(name) && takes(candidate.getParameterTypes(), parameterTypes)) {
					candidate.setAccessible(true);
					return candidate;
				}
			}
			throw new IllegalStateException("Translator: no method " + owner.getName() + "." + name + " for those parameters", missing);
		}
	}

	/**
	 * NeoForge's {@code ObfuscationReflectionHelper.findField(owner, name)}: the field of that name, else the 1.12.2 field
	 * the 1.21 name maps to on that class (Xaero's World Map looks up {@code Minecraft.mainRenderTarget}, 1.12.2's
	 * framebuffer: its framebuffer class failed to load). NeoForge's exception when neither is there.
	 */
	public static java.lang.reflect.Field findField(Class<?> owner, String name) {
		try {
			java.lang.reflect.Field exact = owner.getDeclaredField(name);
			exact.setAccessible(true);
			return exact;
		} catch (NoSuchFieldException missing) {
			String mapped = io.github.xponer2.translator.runtime.FieldNames.of(owner, name);
			if (mapped != null && !mapped.equals(name)) {
				try {
					java.lang.reflect.Field field = owner.getDeclaredField(mapped);
					field.setAccessible(true);
					return field;
				} catch (NoSuchFieldException alsoMissing) {
					// NeoForge's exception below
				}
			}
			throw unableToFindField(missing);
		}
	}

	/** NeoForge's {@code UnableToFindFieldException} (what a mod may catch), a plain one when it can't be made. */
	private static RuntimeException unableToFindField(Exception cause) {
		try {
			Class<?> type = Class.forName("net.neoforged.fml.util.ObfuscationReflectionHelper$UnableToFindFieldException", true,
					net.minecraft.launchwrapper.Launch.classLoader);
			return (RuntimeException) type.getConstructor(Exception.class).newInstance(cause);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return new IllegalStateException(cause);
		}
	}

	private static boolean takes(Class<?>[] declared, Class<?>[] asked) {
		if (declared.length != asked.length) {
			return false;
		}
		for (int i = 0; i < declared.length; i++) {
			if (asked[i] == null || !declared[i].isAssignableFrom(asked[i])) {
				return false;
			}
		}
		return true;
	}
}
