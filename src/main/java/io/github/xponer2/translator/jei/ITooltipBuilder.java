/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.jei;

import net.minecraft.util.text.ITextComponent;

/**
 * Lines added to a slot tooltip (JEI 1.21 {@code ITooltipBuilder}).
 */
public interface ITooltipBuilder {
    void add(ITextComponent line);
}
