/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.entity;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

/** the weapon 1.21's arrow constructor was given, kept on 1.20.6's arrow (ArrowKnockbackMixin) */
public interface ArrowWeapon {
    @Nullable
    ItemStack ladder$weapon();
}
