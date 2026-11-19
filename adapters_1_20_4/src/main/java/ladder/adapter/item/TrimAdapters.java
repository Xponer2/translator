/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;

/**
 * 1.20.5's ArmorTrim has a show_in_tooltip flag; 1.20.4's keeps it in the stack's HideFlags (bit 128, the upgrades
 * line), not in the trim. A trim made hidden is the plain trim here: the flag is the stack's (DataComponents.TRIM).
 */
public final class TrimAdapters {
    private TrimAdapters() {
    }

    /** new ArmorTrim(material, pattern, showInTooltip) */
    public static ArmorTrim create(Holder<TrimMaterial> material, Holder<TrimPattern> pattern, boolean showInTooltip) {
        return new ArmorTrim(material, pattern);
    }

    /** ArmorTrim.withTooltip(show) */
    public static ArmorTrim withTooltip(ArmorTrim trim, boolean showInTooltip) {
        return trim;
    }
}
