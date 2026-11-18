/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testd.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.testd.LadderTestD;

/** 1.21's eat(Level, ItemStack, FoodProperties): 1.20.4 has eat(Level, ItemStack), one parameter less. heal: the same in both. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void ladderTestD$eat(Level level, ItemStack stack, FoodProperties food, CallbackInfoReturnable<ItemStack> cir) {
        LadderTestD.record("eat", BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount() + " client=" + level.isClientSide);
    }

    @Inject(method = "heal", at = @At("HEAD"))
    private void ladderTestD$heal(float amount, CallbackInfo ci) {
        LadderTestD.record("heal", String.valueOf(amount));
    }
}
