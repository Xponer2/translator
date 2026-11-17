/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.decoration.ArmorStand;

/** What the mixins into LivingEntity.hasEffect saw (mixin.LivingEntityMixin). */
public final class MixinTest {
    public static boolean recording;
    public static final List<String> seen = new ArrayList<>();
    public static int returns;

    private MixinTest() {
    }

    static void run(ArmorStand stand, Map<String, String> results) {
        seen.clear();
        returns = 0;
        recording = true;
        boolean omen = stand.hasEffect(MobEffects.BAD_OMEN);
        boolean luck = stand.hasEffect(MobEffects.LUCK);
        recording = false;
        results.put("mixinOmen", String.valueOf(omen));
        results.put("mixinLuck", String.valueOf(luck));
        results.put("mixinSeen", String.join(",", seen));
        results.put("mixinReturns", String.valueOf(returns));
    }
}
