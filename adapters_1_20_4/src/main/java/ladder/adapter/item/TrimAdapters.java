/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.item;

import java.util.Objects;

import net.minecraft.core.Holder;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;

/**
 * 1.20.5's ArmorTrim has a show_in_tooltip flag; 1.20.4's has none (the stack's HideFlags bit 128 hides the upgrades
 * line). A hidden trim is a {@link Hidden} one: DataComponents.TRIM writes it as the bit and reads the bit back as it,
 * and it compares with the flag as 1.20.5's trim does.
 */
public final class TrimAdapters {
    private TrimAdapters() {
    }

    /** 1.20.4's trim with 1.20.5's show_in_tooltip false */
    public static final class Hidden extends ArmorTrim {
        public Hidden(Holder<TrimMaterial> material, Holder<TrimPattern> pattern) {
            super(material, pattern);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Hidden hidden && hidden.material().equals(this.material()) && hidden.pattern().equals(this.pattern());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.material(), this.pattern(), false);
        }
    }

    public static boolean shown(ArmorTrim trim) {
        return !(trim instanceof Hidden);
    }

    /** new ArmorTrim(material, pattern, showInTooltip) */
    public static ArmorTrim create(Holder<TrimMaterial> material, Holder<TrimPattern> pattern, boolean showInTooltip) {
        return showInTooltip ? new ArmorTrim(material, pattern) : new Hidden(material, pattern);
    }

    /** ArmorTrim.withTooltip(show) */
    public static ArmorTrim withTooltip(ArmorTrim trim, boolean showInTooltip) {
        return create(trim.material(), trim.pattern(), showInTooltip);
    }
}
