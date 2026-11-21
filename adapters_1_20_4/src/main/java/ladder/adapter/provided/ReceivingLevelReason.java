/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided;

/**
 * 1.20.5's {@code ReceivingLevelScreen.Reason} (a P row): why a level is being loaded, which picks the loading screen's
 * background. 1.20.4's screen has one background; the reason is kept for the mods' own screens.
 */
public enum ReceivingLevelReason {
    NETHER_PORTAL,
    END_PORTAL,
    OTHER
}
