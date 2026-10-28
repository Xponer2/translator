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

/**
 * The damage/hurt overlay of 1.21 ({@code OverlayTexture}); 1.12.2 has none, only the "no overlay" value is used.
 */
public final class OverlayTexture {
    public static final int NO_OVERLAY = pack(0, 10);

    private OverlayTexture() { }

    public static int pack(int u, int v) {
        return u | v << 16;
    }
}
