/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.client;

import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import ladder.adapter.provided.ReceivingLevelReason;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** 1.20.5's level loading screen with a reason, on 1.20.4's (one background, no reason) */
public final class TransitionScreens {
    private TransitionScreens() {
    }

    /** {@code new ReceivingLevelScreen(ready, reason)} (an X row on the constructor): 1.20.4's screen */
    public static ReceivingLevelScreen receivingLevelScreen(BooleanSupplier ready, ReceivingLevelReason reason) {
        return new ReceivingLevelScreen(ready);
    }

    /** the reason 1.20.5 gives a level change: a portal of the nether or the end when one side is that dimension */
    public static ReceivingLevelReason reason(@Nullable ResourceKey<Level> to, @Nullable ResourceKey<Level> from) {
        if (Level.NETHER.equals(to) || Level.NETHER.equals(from)) {
            return ReceivingLevelReason.NETHER_PORTAL;
        }
        if (Level.END.equals(to) || Level.END.equals(from)) {
            return ReceivingLevelReason.END_PORTAL;
        }
        return ReceivingLevelReason.OTHER;
    }
}
