/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
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
