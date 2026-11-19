/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

/**
 * Client only: the recipes as the client got them from the server (on 1.20.4 over the network: the mod serializer's
 * stream codec), looked up and crafted by code: the mod's own type, its own crafting recipe class, a shaped recipe
 * from its data, the game's smelting.
 */
final class RecipeChecks {
    private RecipeChecks() {
    }

    static void run(Map<String, String> results) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        RecipeManager recipes = level.getRecipeManager();
        var registries = level.registryAccess();

        // the mod's own type: lookup, matches, assemble, the remainder, the recipe as decoded
        SingleRecipeInput sticks = new SingleRecipeInput(new ItemStack(Items.STICK, 7));
        Optional<RecipeHolder<Recipes.Infusing>> infusing = recipes.getRecipeFor(Recipes.INFUSING.get(), sticks, level);
        results.put("recipeInfusing", infusing.map(h -> h.id() + " " + describe(h.value().assemble(sticks, registries)) + " left "
                + describe(h.value().getRemainingItems(sticks))).orElse("none"));
        results.put("recipeInfusingNoMatch", String.valueOf(recipes.getRecipeFor(Recipes.INFUSING.get(), new SingleRecipeInput(new ItemStack(Items.STICK, 2)),
                level).isPresent()));
        results.put("recipeInfusingDecoded", infusing.map(h -> {
            Recipes.Infusing r = h.value();
            return describe(List.of(r.ingredient().getItems())) + " x" + r.count() + " -> " + describe(r.result()) + " "
                    + BuiltInRegistries.RECIPE_SERIALIZER.getKey(r.getSerializer()) + " " + BuiltInRegistries.RECIPE_TYPE.getKey(r.getType());
        }).orElse("none"));
        results.put("recipeInfusingAll", String.valueOf(recipes.getAllRecipesFor(Recipes.INFUSING.get()).size()));

        // the mod's own crafting recipe class, in a 1x1 grid
        CraftingInput apples = CraftingInput.of(1, 1, List.of(new ItemStack(Items.APPLE, 5)));
        Optional<RecipeHolder<CraftingRecipe>> doubling = recipes.getRecipeFor(RecipeType.CRAFTING, apples, level);
        results.put("recipeDoubling", doubling.map(h -> h.id() + " " + describe(h.value().assemble(apples, registries)) + " "
                + h.value().getClass().getSimpleName()).orElse("none"));

        // a shaped recipe from the mod's data (its result's count), in a 3x3 grid of which 2x2 is used
        ItemStack dirt = new ItemStack(Items.COARSE_DIRT);
        CraftingInput grid = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, dirt, dirt.copy(),
                ItemStack.EMPTY, dirt.copy(), dirt.copy()));
        Optional<RecipeHolder<CraftingRecipe>> shaped = recipes.getRecipeFor(RecipeType.CRAFTING, grid, level);
        results.put("recipeShaped", shaped.map(h -> h.id() + " " + describe(h.value().assemble(grid, registries)) + " "
                + describe(h.value().getResultItem(registries)) + " " + h.value().matches(grid, level)).orElse("none"));

        // the game's own smelting
        SingleRecipeInput ore = new SingleRecipeInput(new ItemStack(Items.RAW_IRON));
        Optional<RecipeHolder<SmeltingRecipe>> smelting = recipes.getRecipeFor(RecipeType.SMELTING, ore, level);
        results.put("recipeSmelting", smelting.map(h -> h.id() + " " + describe(h.value().assemble(ore, registries)) + " " + h.value().getCookingTime())
                .orElse("none"));
        results.put("recipeByKey", String.valueOf(recipes.byKey(Payloads.id("infuse_sticks")).map(h -> h.value().getType() == Recipes.INFUSING.get())
                .orElse(false)));
    }

    private static String describe(ItemStack stack) {
        return stack.getCount() + "x" + BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private static String describe(List<ItemStack> stacks) {
        return stacks.stream().map(RecipeChecks::describe).collect(Collectors.joining(",", "[", "]"));
    }

    private static String describe(NonNullList<ItemStack> stacks) {
        return describe((List<ItemStack>) stacks);
    }
}
