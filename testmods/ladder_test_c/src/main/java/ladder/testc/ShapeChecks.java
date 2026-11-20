/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc;

import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;

/**
 * Client only: mixins aimed where 1.21 has a point 1.20.4 lacks (shape patches below): the options screen's own
 * repositionElements (its TAIL, run on a resize) and the pack entry's final super.mouseClicked (its mixin applied when
 * the class loads; a click off the entry's buttons reaches it).
 */
public final class ShapeChecks {
    public static int repositioned;
    public static int superClicks;

    private ShapeChecks() {
    }

    static void run(Map<String, String> results) {
        Minecraft minecraft = Minecraft.getInstance();
        OptionsScreen options = new OptionsScreen(null, minecraft.options);
        int before = repositioned;
        options.init(minecraft, 320, 240);
        int opened = repositioned - before;
        options.resize(minecraft, 400, 300);
        results.put("shapeReposition", "opened " + opened + ", resized " + (repositioned - before - opened));
        try {
            Class.forName(TransferableSelectionList.PackEntry.class.getName());
            results.put("shapePackEntry", "loaded " + superClicks);
        } catch (ClassNotFoundException e) {
            results.put("shapePackEntry", "missing");
        }
    }
}
