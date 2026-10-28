package io.github.xponer2.translator.jei;

/**
 * The ingredients the player looked up (JEI 1.21 {@code IFocusGroup}); JEI 4.16 doesn't pass them, always empty.
 */
public interface IFocusGroup {
    IFocusGroup EMPTY = new IFocusGroup() { };
}
