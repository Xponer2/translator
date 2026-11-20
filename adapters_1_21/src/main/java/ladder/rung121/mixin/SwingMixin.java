/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.rung121.SwingApi;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 21.0's swing asks the item with the swinging hand when a converted mod's item takes it (NeoForge 21.1's onEntitySwing). */
@Mixin(LivingEntity.class)
abstract class SwingMixin {
    @Redirect(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;onEntitySwing(Lnet/minecraft/world/entity/LivingEntity;)Z", remap = false))
    private boolean ladder$swingWithHand(ItemStack stack, LivingEntity entity, InteractionHand hand, boolean updateSelf) {
        return stack.getItem() instanceof SwingApi api ? api.onEntitySwing(stack, entity, hand) : stack.onEntitySwing(entity);
    }
}
