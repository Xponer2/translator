/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;

import ladder.rung1206.provided.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

/** 1.20.6's mobs are 1.21's leashable entities: Mob has Leashable's members; canBeLeashed() is 1.21's mob rule. */
@Mixin(Mob.class)
abstract class MobLeashableMixin implements Leashable {
    @Override
    public boolean canBeLeashed() {
        return !(this instanceof Enemy);
    }
}
