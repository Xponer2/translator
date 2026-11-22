/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;

import ladder.rung1206.provided.DeltaTracker;
import ladder.rung1206.render.TimeAdapters;
import net.minecraft.client.Minecraft;

/**
 * 1.21's {@code Minecraft.getTimer()} on 1.20.6 (added to the class, so a mod's mixin can shadow it): the client's frame
 * time, the adapters' DeltaTracker of 1.20.6's timer. (A call takes the X row's adapter, the same tracker.)
 */
@Mixin(value = Minecraft.class, priority = 500)
abstract class MinecraftTimerMixin {
    public DeltaTracker getTimer() {
        return TimeAdapters.getTimer((Minecraft) (Object) this);
    }
}
