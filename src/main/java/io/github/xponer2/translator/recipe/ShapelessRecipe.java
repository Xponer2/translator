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

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.registry.HolderLookup;
import io.github.xponer2.translator.util.Codecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

import java.util.ArrayList;

/**
 * A shapeless crafting recipe (1.21 {@code ShapelessRecipe}, type {@code minecraft:crafting_shapeless}).
 */
public class ShapelessRecipe implements CraftingRecipe {
    protected final String group;
    protected final CraftingBookCategory category;
    protected final ItemStack result;
    protected final NonNullList<Ingredient> ingredients;

    public ShapelessRecipe(String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients) {
        this.group = group;
        this.category = category;
        this.result = result;
        this.ingredients = ingredients;
    }

    @Override
    public boolean matches(CraftingInput input, World level) {
        if (input.ingredientCount() != this.ingredients.size()) {
            return false;
        }
        var stacks = new ArrayList<ItemStack>();
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                stacks.add(input.getItem(i));
            }
        }
        return RecipeMatcher.findMatches(stacks, this.ingredients) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.ingredients.size();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return VanillaRecipes.SHAPELESS;
    }

    static DataResult<NonNullList<Ingredient>> ingredients(java.util.List<Ingredient> list) {
        if (list.isEmpty()) {
            return DataResult.error("No ingredients for shapeless recipe");
        }
        if (list.size() > ShapedRecipePattern.getMaxWidth() * ShapedRecipePattern.getMaxHeight()) {
            return DataResult.error("Too many ingredients for shapeless recipe");
        }
        var result = NonNullList.<Ingredient>create();
        result.addAll(list);
        return DataResult.success(result);
    }

    public static class Serializer implements RecipeSerializer<ShapelessRecipe> {
        public static final MapCodec<ShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(r -> r.category),
                Codecs.ITEM_STACK.fieldOf("result").forGetter(r -> r.result),
                Codecs.INGREDIENT_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(ShapelessRecipe::ingredients, DataResult::success).forGetter(r -> r.ingredients)
        ).apply(instance, ShapelessRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, recipe) -> {
                    buf.writeUtf(recipe.group);
                    buf.writeEnumValue(recipe.category);
                    buf.writeVarInt(recipe.ingredients.size());
                    for (var ingredient : recipe.ingredients) {
                        ByteBufCodecs.INGREDIENT.encode(buf, ingredient);
                    }
                    ByteBufCodecs.ITEM_STACK.encode(buf, recipe.result);
                },
                buf -> {
                    var group = buf.readUtf();
                    var category = buf.readEnumValue(CraftingBookCategory.class);
                    var ingredients = NonNullList.withSize(buf.readVarInt(), Ingredient.EMPTY);
                    for (int i = 0; i < ingredients.size(); i++) {
                        ingredients.set(i, ByteBufCodecs.INGREDIENT.decode(buf));
                    }
                    return new ShapelessRecipe(group, category, ByteBufCodecs.ITEM_STACK.decode(buf), ingredients);
                });

        @Override
        public MapCodec<ShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
