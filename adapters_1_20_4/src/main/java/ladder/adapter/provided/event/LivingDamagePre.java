/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * NeoForge 21's {@code LivingDamageEvent.Pre} (a P row): the damage about to be taken, after armour and absorption;
 * it can change (0: none). Posted from 20.4's LivingDamageEvent (event.DamageEvents).
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
}
