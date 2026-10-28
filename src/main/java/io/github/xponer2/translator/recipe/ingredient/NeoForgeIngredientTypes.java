package io.github.xponer2.translator.recipe.ingredient;

import io.github.xponer2.translator.registry.MappedRegistry;
import io.github.xponer2.translator.registry.NeoForgeRegistries;
import net.minecraft.util.ResourceLocation;

/**
 * The custom ingredient types of NeoForge, registered under {@code neoforge:<name>} on first use of the registry
 * ({@link IngredientType#byId}, {@link IngredientType#id}).
 */
public final class NeoForgeIngredientTypes {
    public static final IngredientType<DataComponentIngredient> DATA_COMPONENT = new IngredientType<>(DataComponentIngredient.CODEC);
    public static final IngredientType<CompoundIngredient> COMPOUND = new IngredientType<>(CompoundIngredient.CODEC);
    public static final IngredientType<DifferenceIngredient> DIFFERENCE = new IngredientType<>(DifferenceIngredient.CODEC);
    public static final IngredientType<IntersectionIngredient> INTERSECTION = new IngredientType<>(IntersectionIngredient.CODEC);

    private static boolean registered;

    private NeoForgeIngredientTypes() { }

    static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        if (NeoForgeRegistries.INGREDIENT_TYPES instanceof MappedRegistry) {
            var registry = (MappedRegistry<IngredientType<?>>) NeoForgeRegistries.INGREDIENT_TYPES;
            registry.register(new ResourceLocation("neoforge", "components"), DATA_COMPONENT);
            registry.register(new ResourceLocation("neoforge", "compound"), COMPOUND);
            registry.register(new ResourceLocation("neoforge", "difference"), DIFFERENCE);
            registry.register(new ResourceLocation("neoforge", "intersection"), INTERSECTION);
        }
    }
}
