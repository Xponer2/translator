/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
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

/**
 * An armor stand that says it has luck always, else what its superclass says: an override of a method whose
 * parameter changed below (Holder<MobEffect> -> MobEffect), with a super call. Called by the game (the lower method),
 * by the mod (its own calls, bridged) and through a super call.
 */
public class ProbeStand extends ArmorStand {
    public ProbeStand(EntityType<? extends ArmorStand> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean hasEffect(Holder<MobEffect> effect) {
        return effect == MobEffects.LUCK || super.hasEffect(effect);
    }
}
