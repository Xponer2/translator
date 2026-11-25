/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.rung1206.provided.event.XpOrbTargetingEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge 21's XpOrbTargetingEvent on 1.20.6: the orb's choice of the player to follow is the event's (its default the nearest). */
@Mixin(ExperienceOrb.class)
abstract class XpOrbTargetingMixin {
    @Redirect(method = "scanForEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getNearestPlayer(Lnet/minecraft/world/entity/Entity;D)Lnet/minecraft/world/entity/player/Player;"))
    private Player ladder$targetingEvent(Level level, Entity orb, double distance) {
        return NeoForge.EVENT_BUS.post(new XpOrbTargetingEvent((ExperienceOrb) orb, distance)).getFollowingPlayer();
    }
}
