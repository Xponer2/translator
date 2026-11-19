/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.adapter.armour.ArmourAdapters;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;

/**
 * 1.20.5's ArmorItem.Type.BODY (horse and wolf armour) as a fifth constant, made with the enum (before anything can ask
 * for its values): worn in the chest slot, as 1.20.5's EquipmentSlot.BODY is here.
 */
@Mixin(ArmorItem.Type.class)
abstract class ArmourTypeMixin {
    @Shadow
    @Final
    @Mutable
    private static ArmorItem.Type[] $VALUES;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void ladder$body(CallbackInfo ci) {
        ArmorItem.Type body = ArmourTypeAccess.ladder$create("BODY", $VALUES.length, EquipmentSlot.CHEST, "body");
        ArmorItem.Type[] values = Arrays.copyOf($VALUES, $VALUES.length + 1);
        values[values.length - 1] = body;
        $VALUES = values;
        ArmourAdapters.body = body;
    }
}
