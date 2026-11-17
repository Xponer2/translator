/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.jei;

/**
 * Extra tooltip lines of a slot (JEI 1.21 {@code IRecipeSlotRichTooltipCallback}).
 */
@FunctionalInterface
public interface IRecipeSlotRichTooltipCallback {
    void onRichTooltip(IRecipeSlotView slot, ITooltipBuilder tooltip);
}
