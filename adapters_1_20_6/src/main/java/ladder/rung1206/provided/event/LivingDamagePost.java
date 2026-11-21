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
 * NeoForge 21's {@code LivingDamageEvent.Post} (a P row): the damage an entity took, after its health changed. Posted
 * when 20.6's actuallyHurt ends (mixin.DamagePostMixin, event.DamageEvents).
 */
public class LivingDamagePost extends LivingEvent {
    private final DamageSource source;
    private final float original;
    private final float damage;

    public LivingDamagePost(LivingEntity entity, DamageSource source, float original, float damage) {
        super(entity);
        this.source = source;
        this.original = original;
        this.damage = damage;
    }

    public DamageSource getSource() {
        return this.source;
    }

    public float getOriginalDamage() {
        return this.original;
    }

    public float getNewDamage() {
        return this.damage;
    }

    public float getBlockedDamage() {
        return 0.0F;
    }

    public float getShieldDamage() {
        return 0.0F;
    }

    public int getPostAttackInvulnerabilityTicks() {
        return this.getEntity().invulnerableTime;
    }
}
