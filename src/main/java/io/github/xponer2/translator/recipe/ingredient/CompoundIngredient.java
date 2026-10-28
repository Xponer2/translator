package io.github.xponer2.translator.recipe.ingredient;

import io.github.xponer2.translator.util.Codecs;
import com.mojang.serialization.MapCodec;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Any of several ingredients (NeoForge {@code CompoundIngredient}, type {@code neoforge:compound}):
 * {@code {"children": [...]}}.
 */
public class CompoundIngredient implements ICustomIngredient {
    public static final MapCodec<CompoundIngredient> CODEC = Codecs.INGREDIENT_NONEMPTY.listOf().fieldOf("children")
            .xmap(CompoundIngredient::new, CompoundIngredient::children);

    private final List<Ingredient> children;

    public CompoundIngredient(List<Ingredient> children) {
        this.children = children;
    }

    public static Ingredient of(Ingredient... children) {
        return children.length == 1 ? children[0] : new CompoundIngredient(Arrays.asList(children)).toVanilla();
    }

    public List<Ingredient> children() {
        return this.children;
    }

    @Override
    public boolean test(ItemStack stack) {
        return this.children.stream().anyMatch(child -> child.apply(stack));
    }

    @Override
    public Stream<ItemStack> getItems() {
        return this.children.stream().flatMap(child -> Stream.of(child.getMatchingStacks()));
    }

    @Override
    public boolean isSimple() {
        return this.children.stream().allMatch(Ingredient::isSimple);
    }

    @Override
    public IngredientType<?> getType() {
        return NeoForgeIngredientTypes.COMPOUND;
    }
}
