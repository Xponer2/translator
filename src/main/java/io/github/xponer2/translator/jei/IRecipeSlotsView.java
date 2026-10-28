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
