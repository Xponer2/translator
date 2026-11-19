/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.carried.net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.ItemStack;

/**
 * A horse wears 1.20.5's horse armour too (AnimalArmorItem of the equestrian body type, carried; 1.20.4's horse takes
 * only its HorseArmorItem). Its protection comes with the item's own armour modifiers (the body armour is the chest's),
 * not the horse armour bonus 1.20.4's horse gives a HorseArmorItem (which it is not).
 */
@Mixin(Horse.class)
abstract class HorseArmourMixin extends AbstractHorse {
    @Shadow
    @Final
    private static UUID ARMOR_MODIFIER_UUID;

    private HorseArmourMixin() {
        super(null, null);
    }

    @Inject(method = "isArmor", at = @At("HEAD"), cancellable = true)
    private void ladder$animalArmour(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof AnimalArmorItem armour && armour.getBodyType() == AnimalArmorItem.BodyType.EQUESTRIAN) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "setArmorEquipment", at = @At("HEAD"), cancellable = true)
    private void ladder$animalArmourEquipment(ItemStack stack, CallbackInfo ci) {
        if (stack.getItem() instanceof AnimalArmorItem) {
            this.setItemSlot(EquipmentSlot.CHEST, stack);
            this.setDropChance(EquipmentSlot.CHEST, 0.0F);
            if (!this.level().isClientSide) {
                this.getAttribute(Attributes.ARMOR).removeModifier(ARMOR_MODIFIER_UUID);
            }
            ci.cancel();
        }
    }
}
