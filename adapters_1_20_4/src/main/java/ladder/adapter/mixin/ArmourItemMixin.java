/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import java.util.EnumMap;
import java.util.UUID;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.adapter.armour.ArmourAdapters;
import ladder.adapter.armour.RecordMaterial;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

/**
 * The armour modifiers' id of BODY armour (1.20.5's), beside the four pieces'. An armour item of a 1.20.5 material (a
 * record's) keeps its properties as they are: 1.20.5's armour item takes no durability of its material, and 1.20.4's
 * default durability would make an item of none unstackable.
 */
@Mixin(ArmorItem.class)
abstract class ArmourItemMixin {
    @Shadow
    @Final
    private static EnumMap<ArmorItem.Type, UUID> ARMOR_MODIFIER_UUID_PER_TYPE;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void ladder$bodyModifier(CallbackInfo ci) {
        ARMOR_MODIFIER_UUID_PER_TYPE.put(ArmourAdapters.bodyType(), UUID.fromString("C1C72771-8B8E-BA4A-ACE0-81A93C8928B2"));
    }

    private static final ThreadLocal<Boolean> ladder$record = ThreadLocal.withInitial(() -> false);

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ArmorMaterial;getDurabilityForType(Lnet/minecraft/world/item/ArmorItem$Type;)I"))
    private static int ladder$durability(ArmorMaterial material, ArmorItem.Type type) {
        ladder$record.set(material instanceof RecordMaterial);
        return material.getDurabilityForType(type);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item$Properties;defaultDurability(I)Lnet/minecraft/world/item/Item$Properties;"))
    private static Item.Properties ladder$defaultDurability(Item.Properties properties, int durability) {
        if (ladder$record.get()) {
            ladder$record.set(false);
            return properties;
        }
        return properties.defaultDurability(durability);
    }
}
