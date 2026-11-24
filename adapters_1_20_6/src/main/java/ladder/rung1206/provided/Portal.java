/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;

/**
 * 1.21's {@code net.minecraft.world.level.block.Portal} (a P row): a block an entity goes through. Its destination is
 * 1.21's DimensionTransition, below 1.20.6's PortalInfo (portal.Transition). The adapters' Entity mixin uses it, so it
 * is the adapters' own (a carried copy may be missing from a pack).
 */
public interface Portal {
    default int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return 0;
    }

    @Nullable
    PortalInfo getPortalDestination(ServerLevel level, Entity entity, BlockPos pos);

    default Portal.Transition getLocalTransition() {
        return Portal.Transition.NONE;
    }

    enum Transition {
        CONFUSION,
        NONE;
    }
}
