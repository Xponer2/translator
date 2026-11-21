/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.event;

import ladder.adapter.provided.event.LivingDamagePre;
import ladder.adapter.provided.event.LivingIncomingDamageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;

/**
 * NeoForge 21's damage events from 20.4's: LivingIncomingDamageEvent where 20.4 posts LivingHurtEvent (the amount before
 * armour, cancellable), LivingDamageEvent.Pre where it posts LivingDamageEvent (the damage taken). What the mods set goes
 * back into 20.4's event.
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
        });
    }
}
