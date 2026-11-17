/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client;

/**
 * The damage/hurt overlay of 1.21 ({@code OverlayTexture}); 1.12.2 has none, only the "no overlay" value is used.
 */
public final class OverlayTexture {
    public static final int NO_OVERLAY = pack(0, 10);

    public OverlayTexture() { }

    /** 1.21 {@code setupOverlayColor()} / {@code teardownOverlayColor()}: no overlay texture on 1.12.2. */
    public void setupOverlayColor() {
    }

    public void teardownOverlayColor() {
    }

    public static int pack(int u, int v) {
        return u | v << 16;
    }
}
