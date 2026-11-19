/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.block;

import java.util.List;

import ladder.adapter.mixin.BeeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;

/**
 * Bees: BeehiveBlockEntity.IGNORED_BEE_TAGS is private in 1.20.4 and 1.20.6's Occupant (carried down) uses it (1.20.6's
 * list); Bee.setHivePos is new in 1.20.5 (1.20.4's field is package-private: an accessor sets it).
 */
public final class BeeAdapters {
    private static final List<String> IGNORED_BEE_TAGS = List.of("Air", "ArmorDropChances", "ArmorItems", "Brain", "CanPickUpLoot",
            "DeathTime", "FallDistance", "FallFlying", "Fire", "HandDropChances", "HandItems", "HurtByTimestamp", "HurtTime", "LeftHanded",
            "Motion", "NoGravity", "OnGround", "PortalCooldown", "Pos", "Rotation", "SleepingX", "SleepingY", "SleepingZ",
            "CannotEnterHiveTicks", "TicksSincePollination", "CropsGrownSincePollination", "hive_pos", "Passengers", "leash", "UUID");

    private BeeAdapters() {
    }

    public static List<String> ignoredBeeTags() {
        return IGNORED_BEE_TAGS;
    }

    /** Bee.setHivePos(pos) */
    public static void setHivePos(Bee bee, BlockPos pos) {
        ((BeeAccess) bee).ladder$setHivePos(pos);
    }
}
