/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** 1.21's members around leash rendering on 1.20.6 */
public final class LeashAdapters {
    private LeashAdapters() {
    }

    /** 1.21's {@code Entity.getPreciseBodyRotation(partialTick)}: a living entity's body yaw, another's yaw, of the frame */
    public static float getPreciseBodyRotation(Entity entity, float partialTick) {
        if (entity instanceof LivingEntity living) {
            return Mth.lerp(partialTick, living.yBodyRotO, living.yBodyRot);
        }
        return Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
    }
}
