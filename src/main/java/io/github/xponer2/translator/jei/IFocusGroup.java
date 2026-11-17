/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.jei;

/**
 * The ingredients the player looked up (JEI 1.21 {@code IFocusGroup}); JEI 4.16 doesn't pass them, always empty.
 */
public interface IFocusGroup {
    IFocusGroup EMPTY = new IFocusGroup() { };
}
