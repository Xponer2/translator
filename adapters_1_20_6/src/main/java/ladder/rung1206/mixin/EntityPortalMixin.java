/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.rung1206.portal.PortalSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/**
 * 1.21's portal members on 1.20.6's Entity (they are added to the class): {@code portalProcess} and
 * {@code setAsInsidePortal}; 1.20.6's nether portal sets the processor too, and the mod portals tick after 1.20.6's
 * handleNetherPortal (PortalSupport).
 */
@Mixin(Entity.class)
abstract class EntityPortalMixin implements PortalSupport.Holder {
    @Shadow
    protected boolean isInsidePortal;

    public ladder.rung1206.provided.PortalProcessor portalProcess;

    public void setAsInsidePortal(ladder.rung1206.provided.Portal portal, BlockPos pos) {
        PortalSupport.setAsInsidePortal((Entity) (Object) this, portal, pos);
    }

    @Override
    public ladder.rung1206.provided.PortalProcessor ladder$portalProcess() {
        return this.portalProcess;
    }

    @Override
    public void ladder$setPortalProcess(ladder.rung1206.provided.PortalProcessor process) {
        this.portalProcess = process;
    }

    @Override
    public void ladder$setInsidePortal() {
        this.isInsidePortal = true;
    }

    @Inject(method = "handleInsidePortal", at = @At("HEAD"))
    private void ladder$netherPortal(BlockPos pos, CallbackInfo ci) {
        PortalSupport.setAsInsidePortal((Entity) (Object) this, PortalSupport.NETHER, pos);
    }

    @Inject(method = "handleNetherPortal", at = @At("TAIL"))
    private void ladder$modPortals(CallbackInfo ci) {
        PortalSupport.tick((Entity) (Object) this);
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void ladder$restorePortal(Entity from, CallbackInfo ci) {
        PortalSupport.restoreFrom((Entity) (Object) this, from);
    }
}
