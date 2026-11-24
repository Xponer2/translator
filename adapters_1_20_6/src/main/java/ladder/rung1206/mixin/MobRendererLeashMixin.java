/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.Entity;

/** 1.20.6's MobRenderer.render draws the mob's leash itself; EntityRendererLeashMixin draws it now (1.21's place) */
@Mixin(value = MobRenderer.class, priority = 500)
abstract class MobRendererLeashMixin {
    @ModifyExpressionValue(method = "render(Lnet/minecraft/world/entity/Mob;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getLeashHolder()Lnet/minecraft/world/entity/Entity;"))
    private Entity ladder$leashDrawnByEntityRenderer(Entity holder) {
        return null;
    }
}
