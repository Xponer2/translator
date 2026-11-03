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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code KeyMapping} (runtime): 1.21 key codes are GLFW codes, 1.12.2 uses LWJGL 2 codes. Letters,
 * digits, function keys and the common keys are translated; others are unbound.
 */
@SideOnly(Side.CLIENT)
public class KeyMapping extends KeyBinding {
	private static final int[][] KEYS = {
			{32, Keyboard.KEY_SPACE}, {39, Keyboard.KEY_APOSTROPHE}, {44, Keyboard.KEY_COMMA}, {45, Keyboard.KEY_MINUS},
			{46, Keyboard.KEY_PERIOD}, {47, Keyboard.KEY_SLASH}, {59, Keyboard.KEY_SEMICOLON}, {61, Keyboard.KEY_EQUALS},
			{91, Keyboard.KEY_LBRACKET}, {92, Keyboard.KEY_BACKSLASH}, {93, Keyboard.KEY_RBRACKET}, {96, Keyboard.KEY_GRAVE},
			{256, Keyboard.KEY_ESCAPE}, {257, Keyboard.KEY_RETURN}, {258, Keyboard.KEY_TAB}, {259, Keyboard.KEY_BACK},
			{260, Keyboard.KEY_INSERT}, {261, Keyboard.KEY_DELETE}, {262, Keyboard.KEY_RIGHT}, {263, Keyboard.KEY_LEFT},
			{264, Keyboard.KEY_DOWN}, {265, Keyboard.KEY_UP}, {266, Keyboard.KEY_PRIOR}, {267, Keyboard.KEY_NEXT},
			{268, Keyboard.KEY_HOME}, {269, Keyboard.KEY_END}, {340, Keyboard.KEY_LSHIFT}, {341, Keyboard.KEY_LCONTROL},
			{342, Keyboard.KEY_LMENU}, {344, Keyboard.KEY_RSHIFT}, {345, Keyboard.KEY_RCONTROL}, {346, Keyboard.KEY_RMENU}};

	/** Every key a 1.21.1 mod made (see {@link #registerLeftovers}). */
	private static final List<KeyMapping> CREATED = new ArrayList<>();

	public KeyMapping(String name, int glfwKey, String category) {
		super(name, toLwjgl(glfwKey), category);
		CREATED.add(this);
	}

	// NeoForge's extended constructors (conflict context, modifier, key type)

	public KeyMapping(String name, IKeyConflictContext context, KeyModifier modifier, InputConstants.Type type, int key, String category) {
		this(name, context, modifier, type.getOrCreate(key), category);
	}

	public KeyMapping(String name, IKeyConflictContext context, InputConstants.Type type, int key, String category) {
		this(name, context, KeyModifier.NONE, type.getOrCreate(key), category);
	}

	public KeyMapping(String name, InputConstants.Type type, int key, String category) {
		this(name, KeyConflictContext.UNIVERSAL, KeyModifier.NONE, type.getOrCreate(key), category);
	}

	public KeyMapping(String name, IKeyConflictContext context, InputConstants.Key key, String category) {
		this(name, context, KeyModifier.NONE, key, category);
	}

	public KeyMapping(String name, IKeyConflictContext context, KeyModifier modifier, InputConstants.Key key, String category) {
		super(name, context, modifier, key.lwjgl(), category);
		CREATED.add(this);
	}

	/**
	 * Drops the key categories no registered key uses (a key made but never registered, by a 1.21.1 mod or anything
	 * else): the Controls screen makes one line per category and crashes on the lines left empty.
	 */
	public static void pruneCategories() {
		var used = new java.util.HashSet<String>();
		for (KeyBinding key : Minecraft.getMinecraft().gameSettings.keyBindings) {
			used.add(key.getKeyCategory());
		}
		var unused = new java.util.ArrayList<String>();
		for (String category : KeyBinding.getKeybinds()) {
			if (!used.contains(category)) {
				unused.add(category);
			}
		}
		if (!unused.isEmpty()) {
			KeyBinding.getKeybinds().removeAll(unused);
			ForgeBus.LOGGER.info("Key categories without a registered key left out of the Controls screen: {}", unused);
		}
	}

	/**
	 * Registers the keys the mods made but didn't register (their registration failed): such a key's category would be
	 * counted by the Controls screen without an entry, which then crashes on the empty line.
	 */
	public static void registerLeftovers() {
		var settings = Minecraft.getMinecraft().gameSettings;
		for (KeyMapping key : CREATED) {
			if (!ArrayUtils.contains(settings.keyBindings, key)) {
				ClientRegistry.registerKeyBinding(key);
			}
		}
	}

	public static int toLwjgl(int glfw) {
		if (glfw >= 65 && glfw <= 90) {
			return Keyboard.getKeyIndex(String.valueOf((char) glfw));
		}
		if (glfw >= 48 && glfw <= 57) {
			return glfw == 48 ? Keyboard.KEY_0 : Keyboard.KEY_1 + (glfw - 49);
		}
		if (glfw >= 290 && glfw <= 301) {
			return glfw <= 299 ? Keyboard.KEY_F1 + (glfw - 290) : Keyboard.KEY_F11 + (glfw - 300);
		}
		for (int[] pair : KEYS) {
			if (pair[0] == glfw) {
				return pair[1];
			}
		}
		return Keyboard.KEY_NONE;
	}

	public static int toGlfw(int lwjgl) {
		String name = Keyboard.getKeyName(lwjgl);
		if (name != null && name.length() == 1 && Character.isLetterOrDigit(name.charAt(0))) {
			return Character.toUpperCase(name.charAt(0));
		}
		for (int[] pair : KEYS) {
			if (pair[1] == lwjgl) {
				return pair[0];
			}
		}
		return -1;
	}

	/** 1.21 {@code consumeClick}: 1.12.2 {@code isPressed}. */
	public boolean consumeClick() {
		return this.isPressed();
	}

	public boolean isDown() {
		return this.isKeyDown();
	}
}
