package io.github.xponer2.translator.jei;

import io.github.xponer2.translator.client.GuiGraphics;
import mezz.jei.api.gui.IDrawable;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * A recipe category in the JEI 1.21 shape ({@code IRecipeCategory}); {@link JeiCategory} shows it in JEI 4.16.
 */
public interface IRecipeCategory<T> {
    RecipeType<T> getRecipeType();

    ITextComponent getTitle();

    IDrawable getBackground();

    @Nullable
    IDrawable getIcon();

    void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses);

    default void draw(T recipe, IRecipeSlotsView slots, GuiGraphics gfx, double mouseX, double mouseY) { }

    default List<ITextComponent> getTooltipStrings(T recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        return Collections.emptyList();
    }
}
