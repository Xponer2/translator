/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.render;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 1.20.5's list drawing hooks (AbstractSelectionList.renderListBackground and renderListSeparators, called by its
 * renderWidget). 1.20.4's renderWidget draws the same parts itself (its background, its shadows at the top and the
 * bottom); AbstractSelectionListMixin calls a converted mod list's override in their place.
 */
public interface ListRenderApi {
    default void renderListBackground(GuiGraphics graphics) {
        ((ListRenderAccess) this).ladder$vanillaBackground(graphics);
    }

    default void renderListSeparators(GuiGraphics graphics) {
        ((ListRenderAccess) this).ladder$vanillaSeparators(graphics);
    }
}
