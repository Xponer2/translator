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
package io.github.xponer2.translator.recipe;

import io.github.xponer2.translator.registry.RegistryAccess;
import io.github.xponer2.translator.tags.TagManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Loads the 1.21 data once every mod is set up (post init: items, OreDictionary entries and the mods' registries
 * exist; 1.12.2 registries are frozen right after it) and hands the recipes to 1.12.2: crafting recipes to the
 * recipe registry under their 1.21 id, smelting recipes to {@code FurnaceRecipes}.
 */
public final class RecipeLoader {
    private static final Logger LOGGER = LogManager.getLogger("Cucumber Recipes");
    private static final List<Runnable> TAGS_LOADED = new ArrayList<>();
    private static final List<Function<RecipeManager, ? extends Collection<RecipeHolder<?>>>> RECIPE_SOURCES = new ArrayList<>();
    private static final List<Consumer<RecipeManager>> LOADED = new ArrayList<>();

    private RecipeLoader() { }

    /**
     * Runs once the tags are read, before the recipes (1.21 {@code TagsUpdatedEvent}).
     */
    public static void onTagsLoaded(Runnable listener) {
        TAGS_LOADED.add(listener);
    }

    /**
     * Recipes a mod adds in code, next to the JSON ones (1.21 mixins of {@code RecipeManager#apply}).
     */
    public static void addRecipeSource(Function<RecipeManager, ? extends Collection<RecipeHolder<?>>> source) {
        RECIPE_SOURCES.add(source);
    }

    /**
     * Runs once recipes and tags are ready (1.21 mixins of {@code ReloadableServerResources#updateRegistryTags}).
     */
    public static void onLoaded(Consumer<RecipeManager> listener) {
        LOADED.add(listener);
    }

    public static void load() {
        VanillaRecipes.bootstrap();
        TagManager.reload();
        TAGS_LOADED.forEach(Runnable::run);
        var manager = RecipeManager.get();
        manager.setConditionContext(TagManager.conditionContext());
        var recipes = new ArrayList<RecipeHolder<?>>(manager.load());
        for (var source : RECIPE_SOURCES) {
            recipes.addAll(source.apply(manager));
        }
        manager.replaceRecipes(recipes);

        int crafting = 0;
        int smelting = 0;
        var loader = Loader.instance();
        var active = loader.activeModContainer();
        try {
            for (var holder : recipes) {
                var recipe = holder.value();
                if (recipe instanceof CraftingRecipe craftingRecipe) {
                    if (ForgeRegistries.RECIPES.containsKey(holder.id())) {
                        LOGGER.warn("Recipe {} exists in 1.12.2 already, the 1.21 one is only used by the recipe manager", holder.id());
                        continue;
                    }
                    // registry names are checked against the mod that registers them
                    var owner = loader.getIndexedModList().get(holder.id().getNamespace());
                    loader.setActiveModContainer(owner != null ? owner : active);
                    ForgeRegistries.RECIPES.register(new CraftingRecipeWrapper(craftingRecipe).setRegistryName(holder.id()));
                    crafting++;
                } else if (recipe instanceof VanillaRecipes.SmeltingRecipe smeltingRecipe) {
                    var result = smeltingRecipe.getResultItem(RegistryAccess.INSTANCE);
                    for (var input : smeltingRecipe.ingredient().getMatchingStacks()) {
                        FurnaceRecipes.instance().addSmeltingRecipe(input, result, smeltingRecipe.getExperience());
                        smelting++;
                    }
                }
            }
        } finally {
            loader.setActiveModContainer(active);
        }
        LOGGER.info("Registered {} crafting recipes and {} furnace recipes of the 1.21 data", crafting, smelting);
        for (var listener : LOADED) {
            listener.accept(manager);
        }
    }
}
