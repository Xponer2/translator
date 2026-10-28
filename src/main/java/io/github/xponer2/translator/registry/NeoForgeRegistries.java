package io.github.xponer2.translator.registry;

import io.github.xponer2.translator.recipe.ICondition;
import io.github.xponer2.translator.recipe.ingredient.IngredientType;
import io.github.xponer2.translator.util.BuiltInRegistries;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.ResourceLocation;

/**
 * The registries NeoForge adds (1.21 {@code NeoForgeRegistries}), as {@link MappedRegistry}s.
 */
public final class NeoForgeRegistries {
    public static final Registry<MapCodec<? extends ICondition>> CONDITION_SERIALIZERS =
            BuiltInRegistries.byKey(Keys.CONDITION_CODECS);
    public static final Registry<IngredientType<?>> INGREDIENT_TYPES = BuiltInRegistries.byKey(Keys.INGREDIENT_TYPES);

    private NeoForgeRegistries() { }

    public static final class Keys {
        public static final ResourceKey<Registry<MapCodec<? extends ICondition>>> CONDITION_CODECS =
                ResourceKey.createRegistryKey(new ResourceLocation("neoforge", "condition_codecs"));
        public static final ResourceKey<Registry<IngredientType<?>>> INGREDIENT_TYPES =
                ResourceKey.createRegistryKey(new ResourceLocation("neoforge", "ingredient_serializer"));

        private Keys() { }
    }
}
