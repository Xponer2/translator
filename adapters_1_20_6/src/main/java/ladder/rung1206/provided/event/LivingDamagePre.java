/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * NeoForge 21's {@code LivingDamageEvent.Pre} (a P row): the damage about to be taken, after armour and absorption;
 * it can change (0: none). Posted from 20.6's LivingDamageEvent (event.DamageEvents).
 */
public class LivingDamagePre extends LivingEvent {
    private final DamageSource source;
    private final float original;
    private float damage;

    public LivingDamagePre(LivingEntity entity, DamageSource source, float damage) {
        super(entity);
        this.source = source;
        this.original = damage;
        this.damage = damage;
    }

    public DamageSource getSource() {
        return this.source;
    }

    public float getNewDamage() {
        return this.damage;
    }

    public float getOriginalDamage() {
        return this.original;
    }

    public void setNewDamage(float damage) {
        this.damage = damage;
    }

    /** NeoForge 21's container of this hit: its new damage is this event's */
    public ladder.rung1206.provided.DamageContainer getContainer() {
        LivingDamagePre event = this;
        return new ladder.rung1206.provided.DamageContainer(this.source, this.original) {
            {
                super.setNewDamage(event.damage);
            }

            @Override
            public float getNewDamage() {
                return event.damage;
            }

            @Override
            public void setNewDamage(float damage) {
                event.damage = damage;
            }
        };
    }
}
