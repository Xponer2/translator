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

import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.level.block.Blocks;

/** Small members of 1.20.5 / 1.21: holders' names and equality, a registry's holder by location, food, state values. */
final class SmallApiTest {
    private SmallApiTest() {
    }

    static void run(Map<String, String> results) {
        results.put("registeredName", MobEffects.REGENERATION.getRegisteredName());
        results.put("holderIsSame", String.valueOf(MobEffects.REGENERATION.is(MobEffects.REGENERATION)));
        results.put("holderIsOther", String.valueOf(MobEffects.REGENERATION.is(MobEffects.POISON)));
        results.put("holderByLocation", BuiltInRegistries.ITEM.getHolder(ResourceLocation.withDefaultNamespace("apple"))
                .map(h -> h.getRegisteredName()).orElse("none"));
        FoodProperties apple = Foods.APPLE;
        results.put("foodNutrition", String.valueOf(apple.nutrition()));
        results.put("foodSaturation", String.valueOf(apple.saturation()));
        results.put("foodEatSeconds", String.valueOf(apple.eatSeconds()));
        results.put("stateValues", String.valueOf(Blocks.OAK_STAIRS.defaultBlockState().getValues().size()));
    }
}
