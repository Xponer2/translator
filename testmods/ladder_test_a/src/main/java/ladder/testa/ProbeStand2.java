/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
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
