/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 1.21's {@code LayeredDraw.Layer}: one layer of the HUD. A mod's layer (often a lambda) implements 1.21's
 * render(graphics, DeltaTracker); 1.20.6's code, carried down, calls render(graphics, partialTick), which the default
 * passes on. On 1.20.4 a layer is a GUI overlay (event.GuiLayerBridge).
 */
@FunctionalInterface
public interface GuiLayer {
    void render(GuiGraphics graphics, DeltaTracker deltaTracker);

    default void render(GuiGraphics graphics, float partialTick) {
        this.render(graphics, new DeltaTracker.PartialTick(partialTick));
    }
}
