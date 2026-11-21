/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;

import ladder.adapter.provided.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

/**
 * 1.20.4's mobs are 1.21's leashable entities: Mob has most of Leashable's members; mayBeLeashed (1.20.5) is whether it
 * is on a lead, canBeLeashed() is 1.21's mob rule.
 */
@Mixin(Mob.class)
abstract class MobLeashableMixin implements Leashable {
    @Override
    public boolean mayBeLeashed() {
        return this.isLeashed();
    }

    @Override
    public boolean canBeLeashed() {
        return !(this instanceof Enemy);
    }
}
