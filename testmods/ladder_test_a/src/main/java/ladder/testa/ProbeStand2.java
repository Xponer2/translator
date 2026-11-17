/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testa;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;

/** A second level of overrides: bad luck here, then what ProbeStand says (its super call goes to the mod's class). */
public class ProbeStand2 extends ProbeStand {
    public ProbeStand2(EntityType<? extends ArmorStand> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean hasEffect(Holder<MobEffect> effect) {
        return effect == MobEffects.UNLUCK || super.hasEffect(effect);
    }
}
