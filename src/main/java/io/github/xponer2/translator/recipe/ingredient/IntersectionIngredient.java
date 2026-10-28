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

import io.github.xponer2.translator.util.Codecs;
import com.mojang.serialization.MapCodec;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * The items all of several ingredients match (NeoForge {@code IntersectionIngredient}, type
 * {@code neoforge:intersection}): {@code {"children": [...]}}.
 */
public class IntersectionIngredient implements ICustomIngredient {
    public static final MapCodec<IntersectionIngredient> CODEC = Codecs.INGREDIENT_NONEMPTY.listOf().fieldOf("children")
            .xmap(IntersectionIngredient::new, IntersectionIngredient::children);

    private final List<Ingredient> children;

    public IntersectionIngredient(List<Ingredient> children) {
        this.children = children;
    }

    public static Ingredient of(Ingredient... children) {
        return new IntersectionIngredient(Arrays.asList(children)).toVanilla();
    }

    public List<Ingredient> children() {
        return this.children;
    }

    @Override
    public boolean test(ItemStack stack) {
        return this.children.stream().allMatch(child -> child.apply(stack));
    }

    @Override
    public Stream<ItemStack> getItems() {
        if (this.children.isEmpty()) {
            return Stream.empty();
        }
        return Stream.of(this.children.get(0).getMatchingStacks()).filter(this::test);
    }

    @Override
    public boolean isSimple() {
        return this.children.stream().allMatch(Ingredient::isSimple);
    }

    @Override
    public IngredientType<?> getType() {
        return NeoForgeIngredientTypes.INTERSECTION;
    }
}
