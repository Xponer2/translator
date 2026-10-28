package io.github.xponer2.translator.recipe;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.registry.HolderLookup;
import io.github.xponer2.translator.registry.MappedRegistry;
import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.Codecs;
import io.github.xponer2.translator.util.Registries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * The vanilla recipe types and serializers the 1.21 data uses (crafting, smelting, stonecutting), registered in the
 * bridge registries by {@link #bootstrap()}.
 */
public final class VanillaRecipes {
    public static final RecipeSerializer<ShapedRecipe> SHAPED = new ShapedRecipe.Serializer();
    public static final RecipeSerializer<ShapelessRecipe> SHAPELESS = new ShapelessRecipe.Serializer();
    public static final RecipeSerializer<SmeltingRecipe> SMELTING = new SingleItemSerializer<>(SmeltingRecipe.CODEC, SmeltingRecipe::new);
    public static final RecipeSerializer<StonecutterRecipe> STONECUTTER = new SingleItemSerializer<>(StonecutterRecipe.CODEC, StonecutterRecipe::new);
    private static boolean registered;

    private VanillaRecipes() { }

    @SuppressWarnings("unchecked")
    public static synchronized void bootstrap() {
        if (registered) {
            return;
        }
        registered = true;
        var types = (MappedRegistry<RecipeType<?>>) BuiltInRegistries.byKey(Registries.RECIPE_TYPE);
        types.register(new ResourceLocation("minecraft", "crafting"), RecipeType.CRAFTING);
        types.register(new ResourceLocation("minecraft", "smelting"), RecipeType.SMELTING);
        types.register(new ResourceLocation("minecraft", "stonecutting"), RecipeType.STONECUTTING);
        var serializers = (MappedRegistry<RecipeSerializer<?>>) BuiltInRegistries.byKey(Registries.RECIPE_SERIALIZER);
        serializers.register(new ResourceLocation("minecraft", "crafting_shaped"), SHAPED);
        serializers.register(new ResourceLocation("minecraft", "crafting_shapeless"), SHAPELESS);
        serializers.register(new ResourceLocation("minecraft", "smelting"), SMELTING);
        serializers.register(new ResourceLocation("minecraft", "stonecutting"), STONECUTTER);
    }

    /**
     * One ingredient to one result (1.21 {@code SingleItemRecipe} / {@code AbstractCookingRecipe}).
     */
    public abstract static class SingleItemRecipe implements Recipe<SingleRecipeInput> {
        protected final String group;
        protected final Ingredient ingredient;
        protected final ItemStack result;

        protected SingleItemRecipe(String group, Ingredient ingredient, ItemStack result) {
            this.group = group;
            this.ingredient = ingredient;
            this.result = result;
        }

        public Ingredient ingredient() {
            return this.ingredient;
        }

        @Override
        public boolean matches(SingleRecipeInput input, World level) {
            return this.ingredient.apply(input.item());
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            return this.result.copy();
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return true;
        }

        @Override
        public ItemStack getResultItem(HolderLookup.Provider registries) {
            return this.result;
        }

        @Override
        public NonNullList<Ingredient> getIngredients() {
            var list = NonNullList.<Ingredient>create();
            list.add(this.ingredient);
            return list;
        }

        @Override
        public String getGroup() {
            return this.group;
        }
    }

    /**
     * A furnace recipe; the 1.12.2 furnace gets it through {@code FurnaceRecipes} (see RecipeLoader).
     */
    public static class SmeltingRecipe extends SingleItemRecipe {
        static final MapCodec<SmeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Codecs.INGREDIENT_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.ingredient),
                Codecs.SINGLE_ITEM.fieldOf("result").forGetter(r -> r.result),
                Codec.FLOAT.optionalFieldOf("experience", 0.0F).forGetter(r -> r.experience),
                Codec.INT.optionalFieldOf("cookingtime", 200).forGetter(r -> r.cookingTime)
        ).apply(instance, SmeltingRecipe::new));

        private final float experience;
        private final int cookingTime;

        public SmeltingRecipe(String group, Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
            super(group, ingredient, result);
            this.experience = experience;
            this.cookingTime = cookingTime;
        }

        SmeltingRecipe(String group, Ingredient ingredient, ItemStack result) {
            this(group, ingredient, result, 0.0F, 200);
        }

        public float getExperience() {
            return this.experience;
        }

        public int getCookingTime() {
            return this.cookingTime;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return SMELTING;
        }

        @Override
        public RecipeType<?> getType() {
            return RecipeType.SMELTING;
        }
    }

    /**
     * A stonecutter recipe: 1.12.2 has no stonecutter, the recipe is only known to the recipe manager (and JEI).
     */
    public static class StonecutterRecipe extends SingleItemRecipe {
        static final MapCodec<StonecutterRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Codecs.INGREDIENT_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.ingredient),
                Codecs.ITEM_STACK.fieldOf("result").forGetter(r -> r.result)
        ).apply(instance, StonecutterRecipe::new));

        public StonecutterRecipe(String group, Ingredient ingredient, ItemStack result) {
            super(group, ingredient, result);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return STONECUTTER;
        }

        @Override
        public RecipeType<?> getType() {
            return RecipeType.STONECUTTING;
        }
    }

    private static final class SingleItemSerializer<T extends SingleItemRecipe> implements RecipeSerializer<T> {
        private final MapCodec<T> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

        SingleItemSerializer(MapCodec<T> codec, Factory<T> factory) {
            this.codec = codec;
            this.streamCodec = StreamCodec.of(
                    (buf, recipe) -> {
                        buf.writeUtf(recipe.group);
                        ByteBufCodecs.INGREDIENT.encode(buf, recipe.ingredient);
                        ByteBufCodecs.ITEM_STACK.encode(buf, recipe.result);
                    },
                    buf -> factory.create(buf.readUtf(), ByteBufCodecs.INGREDIENT.decode(buf), ByteBufCodecs.ITEM_STACK.decode(buf)));
        }

        @Override
        public MapCodec<T> codec() {
            return this.codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
            return this.streamCodec;
        }
    }

    @FunctionalInterface
    private interface Factory<T> {
        T create(String group, Ingredient ingredient, ItemStack result);
    }
}
