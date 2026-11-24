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

/** 1.21's {@code net.minecraft.world.entity.PortalProcessor} (a P row): an entity's time in a portal (PortalSupport). */
public class PortalProcessor {
    private final Portal portal;
    private BlockPos entryPosition;
    private int portalTime;
    private boolean insidePortalThisTick;

    public PortalProcessor(Portal portal, BlockPos entryPosition) {
        this.portal = portal;
        this.entryPosition = entryPosition;
        this.insidePortalThisTick = true;
    }

    public boolean processPortalTeleportation(ServerLevel level, Entity entity, boolean canUsePortal) {
        if (!this.insidePortalThisTick) {
            this.decayTick();
            return false;
        }
        this.insidePortalThisTick = false;
        return canUsePortal && this.portalTime++ >= this.portal.getPortalTransitionTime(level, entity);
    }

    @Nullable
    public PortalInfo getPortalDestination(ServerLevel level, Entity entity) {
        return this.portal.getPortalDestination(level, entity, this.entryPosition);
    }

    public Portal.Transition getPortalLocalTransition() {
        return this.portal.getLocalTransition();
    }

    private void decayTick() {
        this.portalTime = Math.max(this.portalTime - 4, 0);
    }

    public boolean hasExpired() {
        return this.portalTime <= 0;
    }

    public BlockPos getEntryPosition() {
        return this.entryPosition;
    }

    public void updateEntryPosition(BlockPos pos) {
        this.entryPosition = pos;
    }

    public int getPortalTime() {
        return this.portalTime;
    }

    public boolean isInsidePortalThisTick() {
        return this.insidePortalThisTick;
    }

    public void setAsInsidePortalThisTick(boolean inside) {
        this.insidePortalThisTick = inside;
    }

    public boolean isSamePortal(Portal portal) {
        return this.portal == portal;
    }
}
