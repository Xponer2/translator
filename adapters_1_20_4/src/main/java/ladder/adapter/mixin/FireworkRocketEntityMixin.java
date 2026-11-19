/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.20.5's flight durations go up to 255; 1.20.4's rocket reads Flight as a signed byte, so the stack keeps it at 127
 * there and the real value beside it (DataComponents.FLIGHT_KEY): the rocket's lifetime is made of the real value, as
 * 1.20.4 makes it of Flight (10 ticks per flight level, plus its random part).
 */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin extends Projectile {
    @Shadow
    private int lifetime;

    private FireworkRocketEntityMixin(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private void ladder$longFlight(Level level, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ladder.adapter.component.DataComponents.FLIGHT_KEY, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
            int flight = 1 + tag.getInt(ladder.adapter.component.DataComponents.FLIGHT_KEY);
            this.lifetime = 10 * flight + this.random.nextInt(6) + this.random.nextInt(7);
        }
    }
}
