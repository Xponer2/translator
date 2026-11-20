/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.20.5's screen backgrounds on 1.20.4's: 1.20.5 draws a menu background (a texture, darker in a world) over a
 * blurred game or the title panorama, and separators above and below lists; 1.20.4 draws its dirt background out of a
 * world and a dark gradient in one, and its lists' edges with the same dirt. The 1.20.5 calls draw 1.20.4's own.
 */
public final class ScreenAdapters {
    /** 1.20.4 has no separator textures: its lists' edges are the dirt background */
    private static final ResourceLocation SEPARATOR = Screen.BACKGROUND_LOCATION;
    private static PanoramaRenderer panorama;

    private ScreenAdapters() {
    }

    public static ResourceLocation headerSeparator() {
        return SEPARATOR;
    }

    public static ResourceLocation footerSeparator() {
        return SEPARATOR;
    }

    /** Screen.renderMenuBackground(graphics): 1.20.4's background of a menu, in a world or out of one */
    public static void renderMenuBackground(Screen screen, GuiGraphics graphics) {
        if (Minecraft.getInstance().level == null) {
            screen.renderDirtBackground(graphics);
        } else {
            screen.renderTransparentBackground(graphics);
        }
    }

    /** Screen.renderBlurredBackground(partialTick): 1.20.4 blurs no screen background (its menus show the game as is) */
    public static void renderBlurredBackground(Screen screen, float partialTick) {
    }

    /** Screen.renderPanorama(graphics, partialTick): the title screen's panorama */
    public static void renderPanorama(Screen screen, GuiGraphics graphics, float partialTick) {
        if (panorama == null) {
            panorama = new PanoramaRenderer(TitleScreen.CUBE_MAP);
        }
        panorama.render(partialTick, 1.0F);
    }
}
