/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;

/** ArmorItem.Type's constructor (a fifth constant, BODY, is made with it). */
@Mixin(ArmorItem.Type.class)
public interface ArmourTypeAccess {
    @Invoker("<init>")
    static ArmorItem.Type ladder$create(String constant, int ordinal, EquipmentSlot slot, String name) {
        throw new AssertionError();
    }
}
