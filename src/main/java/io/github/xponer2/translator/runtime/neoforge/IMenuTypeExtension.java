/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.runtime.neoforge;

import io.github.xponer2.translator.menu.IContainerFactory;
import io.github.xponer2.translator.menu.MenuType;

import net.minecraft.inventory.Container;

/** NeoForge {@code IMenuTypeExtension}: menu types whose factory reads the extra data the server sent. */
public interface IMenuTypeExtension {
	static <T extends Container> MenuType<T> create(IContainerFactory<T> factory) {
		return new MenuType<>(factory, null);
	}
}
