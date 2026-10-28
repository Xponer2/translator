package io.github.xponer2.translator.jei;

/**
 * Extra tooltip lines of a slot (JEI 1.21 {@code IRecipeSlotRichTooltipCallback}).
 */
@FunctionalInterface
public interface IRecipeSlotRichTooltipCallback {
    void onRichTooltip(IRecipeSlotView slot, ITooltipBuilder tooltip);
}
