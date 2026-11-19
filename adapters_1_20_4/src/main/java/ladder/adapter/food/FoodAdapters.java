/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.food;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ladder.carried.net.minecraft.world.food.FoodProperties$PossibleEffect;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;

/**
 * 1.20.5's food properties are a record (nutrition, saturation, canAlwaysEat, eatSeconds, effects); 1.20.4's are a
 * class with a saturation modifier (saturation = nutrition * modifier * 2, as FoodData.eat adds it) and a fast flag
 * (0.8 seconds to eat, else 1.6). The record's accessors read 1.20.4's values so, and a record made by a mod becomes
 * 1.20.4's food through its builder (an eating time other than 0.8 or 1.6 seconds is the nearer of the two).
 */
public final class FoodAdapters {
    /** 1.20.6's FoodProperties.DIRECT_CODEC */
    private static final Codec<FoodProperties> FIELDS = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("nutrition").forGetter(FoodAdapters::nutrition),
            Codec.FLOAT.fieldOf("saturation").forGetter(FoodAdapters::saturation),
            Codec.BOOL.optionalFieldOf("can_always_eat", false).forGetter(FoodProperties::canAlwaysEat),
            ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("eat_seconds", 1.6F).forGetter(FoodAdapters::eatSeconds),
            FoodProperties$PossibleEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(FoodAdapters::effects))
            .apply(i, FoodAdapters::create));

    /** 1.21's using_converts_to (the item left after eating) has no place in 1.20.4's food: such data is refused, not cut */
    private static final Codec<FoodProperties> DIRECT_CODEC = Codec.of(FIELDS, new com.mojang.serialization.Decoder<>() {
        @Override
        public <T> com.mojang.serialization.DataResult<Pair<FoodProperties, T>> decode(com.mojang.serialization.DynamicOps<T> ops, T input) {
            boolean converts = ops.getMap(input).result().map(map -> map.get("using_converts_to") != null).orElse(false);
            return converts ? com.mojang.serialization.DataResult.error(() -> "1.20.4's food can't turn into another item (using_converts_to)")
                    : FIELDS.decode(ops, input);
        }
    });

    private FoodAdapters() {
    }

    /** FoodProperties.nutrition(). */
    public static int nutrition(FoodProperties food) {
        return food.getNutrition();
    }

    /** FoodProperties.saturation(): the saturation the food gives, 1.20.4's nutrition * modifier * 2. */
    public static float saturation(FoodProperties food) {
        return food.getNutrition() * food.getSaturationModifier() * 2.0F;
    }

    /** FoodProperties.eatSeconds(): 1.20.4 eats a fast food in 16 ticks, any other in 32. */
    public static float eatSeconds(FoodProperties food) {
        return food.isFastFood() ? 0.8F : 1.6F;
    }

    /** FoodProperties.eatDurationTicks(). */
    public static int eatDurationTicks(FoodProperties food) {
        return (int) (eatSeconds(food) * 20.0F);
    }

    /** FoodProperties.effects(): 1.20.4's (effect, probability) pairs, each effect a fresh copy when asked. */
    public static List<FoodProperties$PossibleEffect> effects(FoodProperties food) {
        List<FoodProperties$PossibleEffect> effects = new ArrayList<>();
        for (Pair<MobEffectInstance, Float> pair : food.getEffects()) {
            MobEffectInstance effect = pair.getFirst();
            if (effect != null) {
                effects.add(new FoodProperties$PossibleEffect(() -> new MobEffectInstance(effect), pair.getSecond()));
            }
        }
        return effects;
    }

    /** new FoodProperties(nutrition, saturation, canAlwaysEat, eatSeconds, effects), through 1.20.4's builder. */
    public static FoodProperties create(int nutrition, float saturation, boolean canAlwaysEat, float eatSeconds,
            List<FoodProperties$PossibleEffect> effects) {
        FoodProperties.Builder builder = new FoodProperties.Builder().nutrition(nutrition)
                .saturationMod(nutrition == 0 ? 0.0F : saturation / (nutrition * 2.0F));
        if (canAlwaysEat) {
            builder.alwaysEat();
        }
        if (eatSeconds < 1.2F) {
            builder.fast();
        }
        for (FoodProperties$PossibleEffect effect : effects) {
            builder.effect(effect::effect, effect.probability());
        }
        return builder.build();
    }

    public static Codec<FoodProperties> directCodec() {
        return DIRECT_CODEC;
    }
}
