/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided;

import net.minecraft.util.Unit;

/**
 * 1.21's {@code EnchantmentEffectComponents} (a P row): the effects an enchantment has. Below, enchantments are classes,
 * not data: the curses' two effects are markers here (a component type needs a codec to be built), answered by {@link ladder.adapter.misc.EnchantmentEffects}.
 */
public final class EnchantmentEffectComponents {
    public static final ladder.adapter.component.DataComponentType<Unit> PREVENT_EQUIPMENT_DROP = ladder.adapter.component.DataComponentType.<Unit>builder().persistent(com.mojang.serialization.Codec.unit(Unit.INSTANCE)).build();
    public static final ladder.adapter.component.DataComponentType<Unit> PREVENT_ARMOR_CHANGE = ladder.adapter.component.DataComponentType.<Unit>builder().persistent(com.mojang.serialization.Codec.unit(Unit.INSTANCE)).build();

    private EnchantmentEffectComponents() {
    }
}
