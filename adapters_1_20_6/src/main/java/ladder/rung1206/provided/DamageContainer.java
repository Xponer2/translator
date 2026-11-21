/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import net.minecraft.world.damagesource.DamageSource;

/**
 * NeoForge 21's {@code DamageContainer} (a P row): one hit's source, its first amount and the amount it comes to; the
 * damage events of the adapters share it (the lower game's own reductions stay its own).
 */
public class DamageContainer {
    private final DamageSource source;
    private final float originalDamage;
    private float newDamage;
    private int invulnerabilityTicks = 20;

    public DamageContainer(DamageSource source, float originalDamage) {
        this.source = source;
        this.originalDamage = originalDamage;
        this.newDamage = originalDamage;
    }

    public DamageSource getSource() {
        return this.source;
    }

    public float getOriginalDamage() {
        return this.originalDamage;
    }

    public float getNewDamage() {
        return this.newDamage;
    }

    public void setNewDamage(float damage) {
        this.newDamage = damage;
    }

    public int getPostAttackInvulnerabilityTicks() {
        return this.invulnerabilityTicks;
    }

    public void setPostAttackInvulnerabilityTicks(int ticks) {
        this.invulnerabilityTicks = ticks;
    }
}
