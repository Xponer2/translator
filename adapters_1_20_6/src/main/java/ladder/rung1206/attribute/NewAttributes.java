/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.attribute;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 1.21's attributes 1.20.6 hasn't, made with their 1.21 ids, bounds and defaults and given to every living entity; the
 * game uses them where 1.21 does, beside 1.20.6's enchantment checks (mixin.NewAttributesMixin).
 */
public final class NewAttributes {
    public static final DeferredHolder<Attribute, Attribute> BURNING_TIME = holder("generic.burning_time");
    public static final DeferredHolder<Attribute, Attribute> EXPLOSION_KNOCKBACK_RESISTANCE = holder("generic.explosion_knockback_resistance");
    public static final DeferredHolder<Attribute, Attribute> OXYGEN_BONUS = holder("generic.oxygen_bonus");
    public static final DeferredHolder<Attribute, Attribute> WATER_MOVEMENT_EFFICIENCY = holder("generic.water_movement_efficiency");
    public static final DeferredHolder<Attribute, Attribute> SUBMERGED_MINING_SPEED = holder("player.submerged_mining_speed");
    public static final DeferredHolder<Attribute, Attribute> SWEEPING_DAMAGE_RATIO = holder("player.sweeping_damage_ratio");

    private NewAttributes() {
    }

    private static DeferredHolder<Attribute, Attribute> holder(String name) {
        return DeferredHolder.create(Registries.ATTRIBUTE, new ResourceLocation(name));
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterEvent event) -> event.register(Registries.ATTRIBUTE, helper -> {
            helper.register(new ResourceLocation("generic.burning_time"), attribute("generic.burning_time", 1.0, 0.0, 1024.0));
            helper.register(new ResourceLocation("generic.explosion_knockback_resistance"), attribute("generic.explosion_knockback_resistance", 0.0, 0.0, 1.0));
            helper.register(new ResourceLocation("generic.oxygen_bonus"), attribute("generic.oxygen_bonus", 0.0, 0.0, 1024.0));
            helper.register(new ResourceLocation("generic.water_movement_efficiency"), attribute("generic.water_movement_efficiency", 0.0, 0.0, 1.0));
            helper.register(new ResourceLocation("player.submerged_mining_speed"), attribute("player.submerged_mining_speed", 0.2, 0.0, 20.0));
            helper.register(new ResourceLocation("player.sweeping_damage_ratio"), attribute("player.sweeping_damage_ratio", 0.0, 0.0, 1.0));
        }));
        modBus.addListener(NewAttributes::addToEntities);
    }

    private static Attribute attribute(String name, double base, double min, double max) {
        return new RangedAttribute("attribute.name." + name, base, min, max).setSyncable(true);
    }

    private static void addToEntities(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            for (Holder<Attribute> attribute : java.util.List.of(BURNING_TIME, EXPLOSION_KNOCKBACK_RESISTANCE, OXYGEN_BONUS, WATER_MOVEMENT_EFFICIENCY,
                    SUBMERGED_MINING_SPEED, SWEEPING_DAMAGE_RATIO)) {
                if (!event.has(type, attribute)) {
                    event.add(type, attribute);
                }
            }
        }
    }

    /** an entity's value of one of them (its default where it hasn't it) */
    public static double value(LivingEntity entity, Holder<Attribute> attribute) {
        return entity.getAttributes().hasAttribute(attribute) ? entity.getAttributeValue(attribute) : attribute.value().getDefaultValue();
    }

    // the F rows' adapters
    public static Holder<Attribute> burningTime() {
        return BURNING_TIME;
    }

    public static Holder<Attribute> explosionKnockbackResistance() {
        return EXPLOSION_KNOCKBACK_RESISTANCE;
    }

    public static Holder<Attribute> oxygenBonus() {
        return OXYGEN_BONUS;
    }

    public static Holder<Attribute> waterMovementEfficiency() {
        return WATER_MOVEMENT_EFFICIENCY;
    }

    public static Holder<Attribute> submergedMiningSpeed() {
        return SUBMERGED_MINING_SPEED;
    }

    public static Holder<Attribute> sweepingDamageRatio() {
        return SWEEPING_DAMAGE_RATIO;
    }
}
