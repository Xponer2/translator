/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * NeoForge 21's {@code LivingIncomingDamageEvent} (a P row): damage coming to an entity, before its armour; cancelled,
 * no damage; its amount can change. Posted from 20.6's LivingHurtEvent (event.DamageEvents).
 */
public class LivingIncomingDamageEvent extends LivingEvent implements ICancellableEvent {
    private final DamageSource source;
    private final float original;
    private float amount;

    public LivingIncomingDamageEvent(LivingEntity entity, DamageSource source, float amount) {
        super(entity);
        this.source = source;
        this.original = amount;
        this.amount = amount;
    }

    public DamageSource getSource() {
        return this.source;
    }

    public float getAmount() {
        return this.amount;
    }

    public float getOriginalAmount() {
        return this.original;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    /** the invulnerability after the hit (the lower game keeps its own) */
    public void setInvulnerabilityTicks(int ticks) {
        this.getEntity().invulnerableTime = ticks;
    }

    /** NeoForge 21's container of this hit: its new damage is this event's amount */
    public ladder.rung1206.provided.DamageContainer getContainer() {
        LivingIncomingDamageEvent event = this;
        return new ladder.rung1206.provided.DamageContainer(this.source, this.original) {
            @Override
            public float getNewDamage() {
                return event.amount;
            }

            @Override
            public void setNewDamage(float damage) {
                event.amount = damage;
            }

            @Override
            public void setPostAttackInvulnerabilityTicks(int ticks) {
                event.setInvulnerabilityTicks(ticks);
            }
        };
    }
}
