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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.21 widget tooltip ({@code Tooltip.create}, {@code widget.setTooltip}): shown while the mouse is over the widget,
 * after the whole screen is drawn (1.21 defers it too: later widgets mustn't draw over it), wrapped at 170 pixels like
 * 1.21. Client only.
 */
public final class Tooltip {
    private static final int MAX_WIDTH = 170;
    private static boolean listening;
    @Nullable
    private static Tooltip pending;
    private static int pendingX;
    private static int pendingY;

    private final ITextComponent message;
    @Nullable
    private final ITextComponent narration;

    private Tooltip(ITextComponent message, @Nullable ITextComponent narration) {
        this.message = message;
        this.narration = narration;
    }

    public static Tooltip create(ITextComponent message) {
        return new Tooltip(message, message);
    }

    public static Tooltip create(ITextComponent message, @Nullable ITextComponent narration) {
        return new Tooltip(message, narration);
    }

    /** The lines as drawn (1.21 FormattedCharSequence = a text component here). */
    public List<ITextComponent> toCharSequence(Minecraft mc) {
        var lines = new ArrayList<ITextComponent>();
        for (String line : this.lines(mc)) {
            lines.add(new TextComponentString(line));
        }
        return lines;
    }

    private List<String> lines(Minecraft mc) {
        return mc.fontRenderer.listFormattedStringToWidth(this.message.getFormattedText(), MAX_WIDTH);
    }

    public ITextComponent getMessage() {
        return this.message;
    }

    @Nullable
    public ITextComponent getNarration() {
        return this.narration;
    }

    /** A widget the mouse is over shows this tooltip once the screen is drawn (the last one of a frame wins). */
    static void show(Tooltip tooltip, int mouseX, int mouseY) {
        if (!listening) {
            listening = true;
            MinecraftForge.EVENT_BUS.register(Tooltip.class);
        }
        pending = tooltip;
        pendingX = mouseX;
        pendingY = mouseY;
    }

    @SubscribeEvent
    public static void onScreenDrawn(GuiScreenEvent.DrawScreenEvent.Post event) {
        Tooltip tooltip = pending;
        pending = null;
        if (tooltip == null) {
            return;
        }
        var mc = Minecraft.getMinecraft();
        var screen = event.getGui();
        GuiUtils.drawHoveringText(tooltip.lines(mc), pendingX, pendingY, screen.width, screen.height, -1, mc.fontRenderer);
    }
}
