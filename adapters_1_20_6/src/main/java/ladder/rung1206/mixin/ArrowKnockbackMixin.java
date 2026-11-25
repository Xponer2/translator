/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import ladder.rung1206.entity.ArrowWeapon;
import ladder.rung1206.entity.EntityArgs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1.21's arrow on 1.20.6:
 * <ul>
 * <li>{@code doKnockback(target, source)} (added to the class), and 1.20.6's onHitEntity knocking back through it: a mod
 * arrow's override decides its knockback, as on 1.21. The base method is 1.20.6's own knockback (its punch level), so a
 * vanilla arrow knocks back as before.</li>
 * <li>the weapon of 1.21's constructors (EntityArgs hands it over): the arrow keeps it ({@code getWeaponItem}) and takes
 * what 1.20.6's bow gives an arrow from it when 1.21 does (punch, flame, piercing: setting them again is harmless when
 * 1.20.6's bow also does). Power stays the bow's: 1.21 adds it at the hit, 1.20.6's bow at the shot.</li>
 * </ul>
 */
@Mixin(AbstractArrow.class)
abstract class ArrowKnockbackMixin extends Projectile implements ArrowWeapon {
    @Shadow
    private int knockback;

    @Unique
    @Nullable
    private ItemStack ladder$weapon;

    ArrowKnockbackMixin(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Shadow
    public abstract void setPierceLevel(byte level);

    @Override
    @Nullable
    public ItemStack ladder$weapon() {
        return this.ladder$weapon;
    }

    /** 1.21's {@code getWeaponItem()} (added to the class: a mod arrow overrides it, or calls super): the kept weapon */
    public ItemStack getWeaponItem() {
        return this.ladder$weapon == null ? ItemStack.EMPTY : this.ladder$weapon;
    }

    protected void doKnockback(LivingEntity target, DamageSource source) {
        if (this.knockback > 0) {
            double resistance = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 push = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(this.knockback * 0.6 * resistance);
            if (push.lengthSqr() > 0.0) {
                target.push(push.x, 0.1, push.z);
            }
        }
    }

    /** 1.20.6's inline knockback test: the hook knocks back (a mod's override, or the base one), the inline code is skipped */
    @ModifyExpressionValue(method = "onHitEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;knockback:I", ordinal = 0))
    private int ladder$knockbackHook(int knockback, @Local LivingEntity target, @Local DamageSource source) {
        this.doKnockback(target, source);
        return 0;
    }

    /** the end of 1.20.6's positioned constructor (every arrow constructor that takes a stack runs it) */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;DDDLnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private void ladder$weapon(CallbackInfo ci) {
        ItemStack weapon = EntityArgs.takePendingWeapon();
        if (weapon == null || weapon.isEmpty() || !(this.level() instanceof ServerLevel)) {
            return;
        }
        this.ladder$weapon = weapon.copy();
        int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH, weapon);
        if (punch > 0) {
            this.knockback = punch;
        }
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAME, weapon) > 0) {
            this.igniteForSeconds(100);
        }
        int piercing = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, weapon);
        if (piercing > 0) {
            this.setPierceLevel((byte) piercing);
        }
    }
}
