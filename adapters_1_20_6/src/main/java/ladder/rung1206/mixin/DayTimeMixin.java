/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.rung1206.misc.DayTime;
import net.minecraft.server.level.ServerLevel;

/** The server's day time advances by NeoForge 21's day time speed (DayTime), one per tick when none is set. */
@Mixin(ServerLevel.class)
abstract class DayTimeMixin {
    @Redirect(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    private void ladder$speed(ServerLevel level, long next) {
        level.setDayTime(next - 1L + DayTime.advance(level));
    }
}
