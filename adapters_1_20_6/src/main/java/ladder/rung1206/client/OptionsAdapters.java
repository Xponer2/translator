/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.OptionsSubScreen;

/** 1.21's options list made for a sub screen (1.20.6's takes the height too: the screen layout's contents). */
public final class OptionsAdapters {
    private OptionsAdapters() {
    }

    /** the K row's height of {@code new OptionsList(minecraft, width, screen)} */
    public static int listHeight(Minecraft minecraft, int width, OptionsSubScreen screen) {
        return screen.layout.getContentHeight();
    }
}
