/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.entity;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Parrot;

/**
 * 1.21's {@code TamableAnimal.canFlyToOwner()} (a tamed animal that flies after its owner: the parrot; false for any
 * other unless it says so). A converted mod's tamed animal that answers it gets this interface (an O row); 1.20.6's
 * FollowOwnerGoal takes the answer when it is made (its canFly flag): {@link #followCanFly}.
 */
public interface FlyToOwnerApi {
    default boolean canFlyToOwner() {
        return canFlyToOwner(null);
    }

    /** the game's own answer (a super call) */
    static boolean canFlyToOwner(TamableAnimal self) {
        return self instanceof Parrot;
    }

    /** {@code tamable.canFlyToOwner()} on any tamed animal */
    static boolean answer(TamableAnimal tamable) {
        return tamable instanceof FlyToOwnerApi own ? own.canFlyToOwner() : canFlyToOwner(tamable);
    }

    /** 1.21's {@code new FollowOwnerGoal(tamable, speed, start, stop)}: 1.20.6's canFly is the animal's answer */
    static boolean followCanFly(TamableAnimal tamable, double speed, float start, float stop) {
        return answer(tamable);
    }
}
