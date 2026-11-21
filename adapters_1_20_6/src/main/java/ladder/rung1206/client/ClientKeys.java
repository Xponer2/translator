/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;

/** The client's keys and level (loaded on the client only). */
public final class ClientKeys {
    private ClientKeys() {
    }

    public static boolean shift() {
        return Screen.hasShiftDown();
    }

    public static boolean control() {
        return Screen.hasControlDown();
    }

    public static boolean alt() {
        return Screen.hasAltDown();
    }

    public static Level level() {
        return Minecraft.getInstance().level;
    }
}
