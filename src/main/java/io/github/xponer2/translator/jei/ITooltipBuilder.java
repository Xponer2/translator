package io.github.xponer2.translator.jei;

import net.minecraft.util.text.ITextComponent;

/**
 * Lines added to a slot tooltip (JEI 1.21 {@code ITooltipBuilder}).
 */
public interface ITooltipBuilder {
    void add(ITextComponent line);
}
