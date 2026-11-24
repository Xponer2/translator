/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.portal;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;

/**
 * 1.21's portals on 1.20.6: an entity keeps 1.21's PortalProcessor (the field {@code portalProcess} the adapters' Entity
 * mixin adds), set by 1.21's {@code setAsInsidePortal} (a mod's portal block) and by 1.20.6's nether portal (so 1.21's
 * {@code portalProcess.isInsidePortalThisTick()} sees it too). Each tick, at the end of 1.20.6's handleNetherPortal (1.21's
 * handlePortal, the M row), a mod's portal counts its time and moves the entity by its transition, as 1.21's handlePortal
 * does; 1.20.6's nether portal keeps its own teleport. On the client a confusion portal (1.21's nether-like ones) gives
 * 1.20.6's portal effect.
 */
public final class PortalSupport {
    private PortalSupport() {
    }

    /** the adapters' Entity mixin: 1.21's {@code Entity.portalProcess} */
    public interface Holder {
        @Nullable
        ladder.carried_1_20_6.net.minecraft.world.entity.PortalProcessor ladder$portalProcess();

        void ladder$setPortalProcess(@Nullable ladder.carried_1_20_6.net.minecraft.world.entity.PortalProcessor process);

        /** 1.20.6's {@code isInsidePortal = true}: its client portal effect this tick */
        void ladder$setInsidePortal();
    }

    /** 1.20.6's nether portal as 1.21's portal processor sees it (its teleport stays 1.20.6's: no destination here) */
    public static final ladder.carried_1_20_6.net.minecraft.world.level.block.Portal NETHER = new ladder.carried_1_20_6.net.minecraft.world.level.block.Portal() {
        @Override
        public int getPortalTransitionTime(ServerLevel level, Entity entity) {
            return entity.getPortalWaitTime();
        }

        @Override
        @Nullable
        public PortalInfo getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
            return null;
        }

        @Override
        public ladder.carried_1_20_6.net.minecraft.world.level.block.Portal.Transition getLocalTransition() {
            return ladder.carried_1_20_6.net.minecraft.world.level.block.Portal.Transition.CONFUSION;
        }
    };

    /** 1.21's {@code Entity.setAsInsidePortal(portal, pos)} */
    public static void setAsInsidePortal(Entity entity, ladder.carried_1_20_6.net.minecraft.world.level.block.Portal portal, BlockPos pos) {
        if (entity.isOnPortalCooldown()) {
            entity.setPortalCooldown();
            return;
        }
        Holder holder = (Holder) entity;
        ladder.carried_1_20_6.net.minecraft.world.entity.PortalProcessor process = holder.ladder$portalProcess();
        if (process != null && process.isSamePortal(portal)) {
            process.updateEntryPosition(pos.immutable());
            process.setAsInsidePortalThisTick(true);
        } else {
            holder.ladder$setPortalProcess(new ladder.carried_1_20_6.net.minecraft.world.entity.PortalProcessor(portal, pos.immutable()));
        }
    }

    /** 1.21's {@code handlePortal()} for the processor (after 1.20.6's handleNetherPortal, which did the cooldown) */
    public static void tick(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Holder holder = (Holder) entity;
        ladder.carried_1_20_6.net.minecraft.world.entity.PortalProcessor process = holder.ladder$portalProcess();
        if (process == null) {
            return;
        }
        boolean nether = process.isSamePortal(NETHER);
        // 1.21's canUsePortal(false): not a passenger, alive (1.20.6's canChangeDimensions also leaves out vehicles)
        if (process.processPortalTeleportation(level, entity, !nether && entity.canChangeDimensions() && entity.isAlive())) {
            level.getProfiler().push("portal");
            entity.setPortalCooldown();
            PortalInfo transition = process.getPortalDestination(level, entity);
            if (transition != null) {
                ServerLevel to = PortalAdapters.newLevel(transition);
                boolean enabled = to == null || to.dimension() != Level.NETHER || level.getServer().isNetherEnabled();
                if (enabled && (to == null || to == level || entity.canChangeDimensions())) {
                    PortalAdapters.changeDimension(entity, transition);
                }
            }
            level.getProfiler().pop();
        } else if (process.hasExpired()) {
            holder.ladder$setPortalProcess(null);
        }
    }

    /** 1.21's client: a confusion portal the player stands in gives the portal effect (1.20.6's, by isInsidePortal) */
    public static void clientTick(Entity player) {
        Holder holder = (Holder) player;
        ladder.carried_1_20_6.net.minecraft.world.entity.PortalProcessor process = holder.ladder$portalProcess();
        if (process != null && process.isInsidePortalThisTick()
                && process.getPortalLocalTransition() == ladder.carried_1_20_6.net.minecraft.world.level.block.Portal.Transition.CONFUSION) {
            holder.ladder$setInsidePortal();
            process.setAsInsidePortalThisTick(false);
        }
    }

    /** 1.21's restoreFrom: the new entity (a respawned or moved one) keeps the old one's portal */
    public static void restoreFrom(Entity entity, Entity from) {
        ((Holder) entity).ladder$setPortalProcess(((Holder) from).ladder$portalProcess());
    }
}
