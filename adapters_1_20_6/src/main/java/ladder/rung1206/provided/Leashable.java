/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;

/**
 * 1.21's {@code Leashable} (a P row): what can be put on a lead. 1.20.6's mobs are the leashable entities; Mob has these
 * members itself and implements this interface (mixin.MobLeashableMixin). 1.21's leash data and leash physics are not
 * here.
 */
public interface Leashable {
    boolean isLeashed();

    @Nullable
    Entity getLeashHolder();

    void setLeashedTo(Entity holder, boolean broadcast);

    void dropLeash(boolean broadcast, boolean dropItem);

    boolean mayBeLeashed();

    /** whether a lead may be put on it (1.21: the entity's own rule; a mob: not an enemy) */
    default boolean canBeLeashed() {
        return true;
    }
}
