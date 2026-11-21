/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.adapter.provided.event.EntityTickEvent;
import net.minecraft.world.entity.Entity;

/** NeoForge 20.5's entity tick events around the server's entity ticks and ride ticks (EntityTickEvent). */
@Mixin(net.minecraft.server.level.ServerLevel.class)
abstract class EntityTickEventMixin {
    @Redirect(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void ladder$tick(Entity entity) {
        EntityTickEvent.ticked(entity, entity::tick);
    }

    @Redirect(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void ladder$rideTick(Entity passenger) {
        EntityTickEvent.ticked(passenger, passenger::rideTick);
    }
}
