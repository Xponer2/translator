package io.github.xponer2.translator.recipe.ingredient;

import io.github.xponer2.translator.util.Codecs;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import java.util.stream.Stream;

/**
 * An ingredient without the items of another (NeoForge {@code DifferenceIngredient}, type
 * {@code neoforge:difference}): {@code {"base": ..., "subtracted": ...}}.
 */
public class DifferenceIngredient implements ICustomIngredient {
    public static final MapCodec<DifferenceIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codecs.INGREDIENT_NONEMPTY.fieldOf("base").forGetter(DifferenceIngredient::base),
            Codecs.INGREDIENT.fieldOf("subtracted").forGetter(DifferenceIngredient::subtracted)
    ).apply(instance, DifferenceIngredient::new));

    private final Ingredient base;
    private final Ingredient subtracted;

    public DifferenceIngredient(Ingredient base, Ingredient subtracted) {
        this.base = base;
        this.subtracted = subtracted;
    }

    public static Ingredient of(Ingredient base, Ingredient subtracted) {
        return new DifferenceIngredient(base, subtracted).toVanilla();
    }

    public Ingredient base() {
        return this.base;
    }

    public Ingredient subtracted() {
        return this.subtracted;
    }

    @Override
    public boolean test(ItemStack stack) {
        return this.base.apply(stack) && !this.subtracted.apply(stack);
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Stream.of(this.base.getMatchingStacks()).filter(stack -> !this.subtracted.apply(stack));
    }

    @Override
    public boolean isSimple() {
        return this.base.isSimple() && this.subtracted.isSimple();
    }

    @Override
    public IngredientType<?> getType() {
        return NeoForgeIngredientTypes.DIFFERENCE;
    }
}
