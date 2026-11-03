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
package io.github.xponer2.translator.runtime.neoforge;

/** 1.21 {@code InputConstants} (blaze3d): key constants with GLFW codes. */
public final class InputConstants {
	public static final Key UNKNOWN = new Key(-1);

	private InputConstants() {
	}

	public static Key getKey(String name) {
		return UNKNOWN;
	}

	public static Key getKey(int keysym, int scancode) {
		return new Key(keysym);
	}

	/** Whether a key (GLFW code) is held now: the 1.12.2 keyboard (hotkeys a mod checks itself, "hold shift for more"). */
	public static boolean isKeyDown(long window, int key) {
		return io.github.xponer2.translator.runtime.redirect.GlfwBridges.glfwGetKey(window, key) == 1;
	}

	public static final class Key {
		private final int value;
		private final Type type;

		public Key(int value) {
			this(Type.KEYSYM, value);
		}

		Key(Type type, int value) {
			this.type = type;
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		public Type getType() {
			return this.type;
		}

		/** The 1.12.2 key code: LWJGL 2 keys, mouse buttons as button - 100. */
		int lwjgl() {
			return this.type == Type.MOUSE ? this.value - 100 : this.type == Type.SCANCODE ? this.value : KeyMapping.toLwjgl(this.value);
		}
	}

	/** 1.21 {@code InputConstants.Type}: keyboard key (GLFW code), scan code or mouse button. */
	public enum Type {
		KEYSYM,
		SCANCODE,
		MOUSE;

		public Key getOrCreate(int value) {
			return new Key(this, value);
		}
	}
}
