/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.adapter.entity.SynchedDataApi;
import ladder.adapter.entity.SynchedDataBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** A converted mod's entity defines its synched data the 1.20.5 way, right after 1.20.4's own (SynchedDataApi). */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;defineSynchedData()V", shift = At.Shift.AFTER))
    private void ladder$defineSynchedData(EntityType<?> type, Level level, CallbackInfo ci) {
        if ((Object) this instanceof SynchedDataApi api) {
            api.defineSynchedData(new SynchedDataBuilder((Entity) (Object) this));
        }
    }
}
