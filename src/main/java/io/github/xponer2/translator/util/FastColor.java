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

/**
 * 1.21 packed color helpers.
 */
public final class FastColor {
	private FastColor() {
	}

	public static final class ARGB32 {
		private ARGB32() {
		}

		public static int alpha(int color) {
			return color >>> 24;
		}

		public static int red(int color) {
			return color >> 16 & 0xFF;
		}

		public static int green(int color) {
			return color >> 8 & 0xFF;
		}

		public static int blue(int color) {
			return color & 0xFF;
		}

		public static int color(int alpha, int red, int green, int blue) {
			return alpha << 24 | red << 16 | green << 8 | blue;
		}

		public static int color(int red, int green, int blue) {
			return color(0xFF, red, green, blue);
		}

		public static int color(int alpha, int rgb) {
			return alpha << 24 | rgb & 0xFFFFFF;
		}

		public static int opaque(int color) {
			return color | 0xFF000000;
		}

		public static int multiply(int first, int second) {
			return color(alpha(first) * alpha(second) / 255, red(first) * red(second) / 255, green(first) * green(second) / 255,
					blue(first) * blue(second) / 255);
		}
	}
}
