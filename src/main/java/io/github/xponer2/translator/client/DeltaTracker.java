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

/**
 * 1.21 {@code DeltaTracker} ({@code Minecraft.getTimer()}): the partial tick of the frame. 1.12.2 runs 20 ticks a
 * second like 1.21; one tick per tick.
 */
public final class DeltaTracker {
    public static final DeltaTracker INSTANCE = new DeltaTracker();

    private DeltaTracker() {
    }

    public float getGameTimeDeltaPartialTick(boolean runsNormally) {
        return Minecraft.getMinecraft().getRenderPartialTicks();
    }

    public float getGameTimeDeltaTicks() {
        return 1.0F;
    }

    public float getRealtimeDeltaTicks() {
        return 1.0F;
    }
}
