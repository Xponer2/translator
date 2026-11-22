/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

/** NeoForge 21's {@code ModConfigSpec.RestartType} (a P row): what a config value's change needs (20.6 knows a world restart only). */
public enum RestartType {
    NONE,
    WORLD,
    GAME;

    /** {@code valueSpec.restartType()} (an X row) */
    public static RestartType restartType(net.neoforged.neoforge.common.ModConfigSpec.ValueSpec spec) {
        return spec.needsWorldRestart() ? WORLD : NONE;
    }

    /** 21's {@code with(other)}: the more of the two (a game restart over a world one) */
    public RestartType with(RestartType other) {
        return other == null || other.ordinal() <= this.ordinal() ? this : other;
    }
}
