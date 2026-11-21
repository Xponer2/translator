/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.rung1206.item.UseDurations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** The client's uses of a stack's use duration with the user known (the player drawn, the holder of an item model). */
final class UseDurationClientSites {
    private UseDurationClientSites() {
    }

    @Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
    abstract static class HandRenderer {
        @Shadow
        @Final
        private Minecraft minecraft;

        @Redirect(method = {"applyEatTransform", "renderArmWithItem"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseDuration()I"))
        private int ladder$withUser(ItemStack stack) {
            return UseDurations.stackDuration(stack, this.minecraft.player);
        }
    }

    @Mixin(net.minecraft.client.renderer.item.ItemProperties.class)
    abstract static class Properties {
        @Redirect(method = {"lambda$static$7", "lambda$static$13"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseDuration()I"))
        private static int ladder$withUser(ItemStack stack, ItemStack same, ClientLevel level, LivingEntity entity, int seed) {
            return UseDurations.stackDuration(stack, entity);
        }
    }
}
