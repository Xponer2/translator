/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
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

    public static ResourceLocation inworldFooterSeparator() {
        return SEPARATOR;
    }

    public static ResourceLocation inworldHeaderSeparator() {
        return SEPARATOR;
    }

    /** Screen.MENU_BACKGROUND: 1.20.4's menus are drawn on its dirt background */
    public static ResourceLocation menuBackground() {
        return Screen.BACKGROUND_LOCATION;
    }

    /** a mod list's {@code super.renderListBackground(graphics)}: 1.20.4's own list background */
    public static void renderListBackground(AbstractSelectionList<?> list, GuiGraphics graphics) {
        ((ListRenderAccess) list).ladder$vanillaBackground(graphics);
    }

    /**
     * OptionsScreen.layout: 1.20.5's options screen lays itself out in a header (61) and footer (33) layout over the
     * whole screen; 1.20.4's puts the same buttons in a grid. The layout of the same size, arranged to the screen's size
     * now, gives what mods read of it: its geometry only (the converter's F row lets nothing else be used; it holds
     * none of the screen's widgets).
     */
    public static HeaderAndFooterLayout optionsLayout(OptionsScreen screen) {
        HeaderAndFooterLayout layout = new HeaderAndFooterLayout(screen, 61, 33);
        layout.arrangeElements();
        return layout;
    }

    /** ConfirmLinkScreen.confirmLinkNow(screen, url, trusted): 1.20.4's own takes every link as trusted */
    public static void confirmLinkNow(Screen screen, String url, boolean trusted) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ConfirmLinkScreen(open -> {
            if (open) {
                net.minecraft.Util.getPlatform().openUri(url);
            }
            minecraft.setScreen(screen);
        }, url, trusted));
    }

    /** a mod list's {@code super.renderListSeparators(graphics)}: 1.20.4's own shadows at the list's top and bottom */
    public static void renderListSeparators(AbstractSelectionList<?> list, GuiGraphics graphics) {
        ((ListRenderAccess) list).ladder$vanillaSeparators(graphics);
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
