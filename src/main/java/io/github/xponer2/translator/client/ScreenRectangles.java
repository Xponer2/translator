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
package io.github.xponer2.translator.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

/**
 * The carried 1.21 GUI classes the widget and screen facades hand out: {@code ScreenRectangle} (bounds of a widget,
 * which JourneyMap and layouts read) and {@code ComponentPath} (a focus path). They are 1.21 code loaded with the mods,
 * so the facades make and use them by reflection. Client only.
 */
final class ScreenRectangles {
	@Nullable
	private static Constructor<?> rectangle;
	@Nullable
	private static Method applyFocus;
	private static boolean missing;

	private ScreenRectangles() {
	}

	/** A {@code new ScreenRectangle(x, y, width, height)}, null when the class can't be loaded. */
	@Nullable
	static Object of(int x, int y, int width, int height) {
		if (missing) {
			return null;
		}
		try {
			if (rectangle == null) {
				rectangle = Class.forName("net.minecraft.client.gui.navigation.ScreenRectangle", true, net.minecraft.launchwrapper.Launch.classLoader)
						.getConstructor(int.class, int.class, int.class, int.class);
			}
			return rectangle.newInstance(x, y, width, height);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			missing = true;
			return null;
		}
	}

	/** {@code path.applyFocus(focused)} of a carried ComponentPath; false when it isn't one. */
	static boolean applyFocus(@Nullable Object path, boolean focused) {
		if (path == null) {
			return false;
		}
		try {
			if (applyFocus == null) {
				applyFocus = Class.forName("net.minecraft.client.gui.ComponentPath", true, net.minecraft.launchwrapper.Launch.classLoader)
						.getMethod("applyFocus", boolean.class);
			}
			applyFocus.invoke(path, focused);
			return true;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return false;
		}
	}
}
