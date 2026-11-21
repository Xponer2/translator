/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.portal;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;

/**
 * 1.21's DimensionTransition below: 1.20.6's PortalInfo (its intermediary twin: the position, speed and rotation) with
 * what 1.21 added, the level it leads to, whether the respawn block was missing, and what runs after it.
 */
public final class Transition extends PortalInfo {
    @Nullable
    final ServerLevel newLevel;
    final boolean missingRespawnBlock;
    final ladder.carried_1_20_6.net.minecraft.world.level.portal.DimensionTransition$PostDimensionTransition post;

    Transition(@Nullable ServerLevel newLevel, Vec3 pos, Vec3 speed, float yRot, float xRot, boolean missingRespawnBlock,
            ladder.carried_1_20_6.net.minecraft.world.level.portal.DimensionTransition$PostDimensionTransition post) {
        super(pos, speed, yRot, xRot);
        this.newLevel = newLevel;
        this.missingRespawnBlock = missingRespawnBlock;
        this.post = post;
    }
}
