/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/**
 * 1.21 {@code com.mojang.blaze3d.platform.Window} ({@code Minecraft.getWindow()}): the sizes of the 1.12.2 display and
 * of its GUI scale. There is no GLFW window on LWJGL 2: the handle is 0.
 */
public final class Window {
    private static final Window INSTANCE = new Window();

    private Window() {
    }

    public static Window get() {
        return INSTANCE;
    }

    private static ScaledResolution scaled() {
        return new ScaledResolution(Minecraft.getMinecraft());
    }

    public int getGuiScaledWidth() {
        return scaled().getScaledWidth();
    }

    public int getGuiScaledHeight() {
        return scaled().getScaledHeight();
    }

    public double getGuiScale() {
        return scaled().getScaleFactor();
    }

    public int getWidth() {
        return Minecraft.getMinecraft().displayWidth;
    }

    public int getHeight() {
        return Minecraft.getMinecraft().displayHeight;
    }

    public int getScreenWidth() {
        return Minecraft.getMinecraft().displayWidth;
    }

    public int getScreenHeight() {
        return Minecraft.getMinecraft().displayHeight;
    }

    public long getWindow() {
        return 0L;
    }

    public boolean isFullscreen() {
        return Minecraft.getMinecraft().isFullScreen();
    }
}
