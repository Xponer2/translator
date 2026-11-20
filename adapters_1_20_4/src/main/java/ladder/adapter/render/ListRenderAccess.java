/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.render;

import net.minecraft.client.gui.GuiGraphics;

/** 1.20.4's own list background and shadows, as its renderWidget draws them (AbstractSelectionListMixin) */
public interface ListRenderAccess {
    void ladder$vanillaBackground(GuiGraphics graphics);

    void ladder$vanillaSeparators(GuiGraphics graphics);
}
