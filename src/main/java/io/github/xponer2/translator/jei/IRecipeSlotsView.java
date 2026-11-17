/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.jei;

import java.util.Collections;
import java.util.List;

/**
 * The slots of a shown recipe (JEI 1.21 {@code IRecipeSlotsView}).
 */
public interface IRecipeSlotsView {
    IRecipeSlotsView EMPTY = Collections::emptyList;

    List<IRecipeSlotView> getSlotViews();
}
