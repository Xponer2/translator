/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * The values 1.21 passes to an entity's hooks that 1.20.6 doesn't (the A and D rows of dropCustomDeathLoot,
 * dropAllDeathLoot, populateDefaultEquipmentEnchantments, eat, onEquippedItemBroken, PlayerList.respawn): 1.21 hands
 * the server level the entity is in, the food eaten, the broken item, why the player respawns; 1.20.6 the looting level
 * it computed from the killing blow.
 */
public final class EntityArgs {
    private EntityArgs() {
    }

    /** the server level of an entity's server-side hook (1.21 passes it; 1.20.6 runs these hooks on the server only) */
    public static ServerLevel level(LivingEntity entity) {
        return (ServerLevel) entity.level();
    }

    /** the level of a mob's spawn equipment hook */
    public static ServerLevelAccessor levelAccessor(Mob mob) {
        return (ServerLevelAccessor) mob.level();
    }

    /** the looting level 1.20.6's dropAllDeathLoot passes to dropCustomDeathLoot, of the same blow */
    public static int looting(LivingEntity entity, DamageSource source) {
        return CommonHooks.getLootingLevel(entity, source.getEntity(), source);
    }

    /** the food of the stack an entity eats (1.21's eat(level, stack) passes the stack's own) */
    public static FoodProperties food(LivingEntity entity, ItemStack stack) {
        return stack.getFoodProperties(entity);
    }

    /** the item that broke in a slot (1.20.6 announces the break before the stack shrinks) */
    public static Item brokenItem(LivingEntity entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot).getItem();
    }

    /** why a player respawns: 1.20.6 keeps everything only for the way back from the End */
    public static Entity.RemovalReason respawnReason(boolean keepEverything) {
        return keepEverything ? Entity.RemovalReason.CHANGED_DIMENSION : Entity.RemovalReason.KILLED;
    }

    /** the saddle 1.21's equipSaddle(stack, source) puts on (1.20.6 always a plain saddle) */
    public static ItemStack saddle() {
        return new ItemStack(Items.SADDLE);
    }

    /** the weapon a shooter shoots with: the item in use (a bow being released), else the main hand */
    public static ItemStack weapon(LivingEntity shooter) {
        ItemStack using = shooter.getUseItem();
        return using.isEmpty() ? shooter.getMainHandItem() : using;
    }

    /** the weapon an arrow was shot with (1.20.6 keeps none: its shooter's, as weapon gives it) */
    public static ItemStack weaponOf(AbstractArrow arrow) {
        return arrow.getOwner() instanceof LivingEntity shooter ? weapon(shooter) : ItemStack.EMPTY;
    }

    /** a skeleton's bow */
    public static ItemStack skeletonWeapon(AbstractSkeleton skeleton) {
        return weapon(skeleton);
    }
}
