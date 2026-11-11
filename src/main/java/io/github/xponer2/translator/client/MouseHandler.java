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

import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;

/**
 * 1.21 {@code MouseHandler} ({@code Minecraft.mouseHandler}): the mouse in window pixels from the top left, as GLFW
 * gives it (LWJGL 2 counts from the bottom).
 */
public final class MouseHandler {
    public static final MouseHandler INSTANCE = new MouseHandler();

    private MouseHandler() {
    }

    public double xpos() {
        return Mouse.getX();
    }

    public double ypos() {
        return Minecraft.getMinecraft().displayHeight - Mouse.getY() - 1;
    }

    public boolean isLeftPressed() {
        return Mouse.isButtonDown(0);
    }

    public boolean isRightPressed() {
        return Mouse.isButtonDown(1);
    }

    public boolean isMiddlePressed() {
        return Mouse.isButtonDown(2);
    }

    public boolean isMouseGrabbed() {
        return Mouse.isGrabbed();
    }

    public void grabMouse() {
        Minecraft.getMinecraft().mouseHelper.grabMouseCursor();
    }

    public void releaseMouse() {
        Minecraft.getMinecraft().mouseHelper.ungrabMouseCursor();
    }

    /** 1.21: the next mouse move isn't turned into a look (JourneyMap after its fullscreen map); 1.12.2 reads deltas. */
    public void setIgnoreFirstMove() {
        Mouse.getDX();
        Mouse.getDY();
    }

    /** 1.21 {@code cursorEntered()}: the cursor came back into the window. */
    public void cursorEntered() {
    }
}
