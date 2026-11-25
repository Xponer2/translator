/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.21's Mob members on 1.20.6 (added to the class): {@code playAttackSound()}, the hook 1.21's doHurtTarget calls after a
 * hit (empty for vanilla mobs: a mod's mob plays its sound there), and {@code dropPreservedEquipment}, which drops the
 * equipment a mob would always drop (only mods call it).
 */
@Mixin(Mob.class)
abstract class MobAttackMixin extends LivingEntity {
    MobAttackMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Shadow
    protected abstract float getEquipmentDropChance(EquipmentSlot slot);

    protected void playAttackSound() {
    }

    public void dropPreservedEquipment() {
        this.dropPreservedEquipment(stack -> true);
    }

    public Set<EquipmentSlot> dropPreservedEquipment(Predicate<ItemStack> filter) {
        Set<EquipmentSlot> kept = new HashSet<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                if (!filter.test(stack)) {
                    kept.add(slot);
                } else if (this.getEquipmentDropChance(slot) > 1.0) {
                    this.setItemSlot(slot, ItemStack.EMPTY);
                    this.spawnAtLocation(stack);
                }
            }
        }
        return kept;
    }

    /** 1.21's doHurtTarget plays the attack sound last, after a hit that landed */
    @Inject(method = "doHurtTarget", at = @At("RETURN"))
    private void ladder$attackSound(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            this.playAttackSound();
        }
    }
}
