/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.event;

import ladder.rung1206.provided.event.LivingDamagePre;
import ladder.rung1206.provided.event.LivingIncomingDamageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;

/**
 * NeoForge 21's damage events from 20.6's: LivingIncomingDamageEvent where 20.6 posts LivingHurtEvent (the amount before
 * armour, cancellable), LivingDamageEvent.Pre where it posts LivingDamageEvent (the damage taken). What the mods set goes
 * back into 20.6's event.
 */
public final class DamageEvents {
    private DamageEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LivingHurtEvent.class, event -> {
            LivingIncomingDamageEvent incoming = new LivingIncomingDamageEvent(event.getEntity(), event.getSource(), event.getAmount());
            NeoForge.EVENT_BUS.post(incoming);
            if (incoming.isCanceled()) {
                event.setCanceled(true);
            } else {
                event.setAmount(incoming.getAmount());
            }
        });
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.class, event -> {
            LivingDamagePre pre = new LivingDamagePre(event.getEntity(), event.getSource(), event.getAmount());
            NeoForge.EVENT_BUS.post(pre);
            event.setAmount(pre.getNewDamage());
            TAKEN.put(event.getEntity(), new float[] {pre.getOriginalDamage(), pre.getNewDamage()});
        });
    }

    /** an entity's hit being taken: its first damage and the one it took (20.6's damage event, then actuallyHurt's end) */
    static final java.util.Map<net.minecraft.world.entity.LivingEntity, float[]> TAKEN = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** when 20.6's actuallyHurt ends: NeoForge 21's LivingDamageEvent.Post, if the hit was taken (not cancelled) */
    public static void afterHurt(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        float[] taken = TAKEN.remove(entity);
        if (taken != null && taken[1] > 0.0F) {
            NeoForge.EVENT_BUS.post(new ladder.rung1206.provided.event.LivingDamagePost(entity, source, taken[0], taken[1]));
        }
    }
}
