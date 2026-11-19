/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A mod's own recipes as 1.21 makes them: a recipe type of its own (infusing: a count of an ingredient into a result),
 * its serializer (a map codec for the data file, a stream codec for the network), and a crafting recipe of its own
 * class (doubling: one stack in the grid becomes twice as many). Their data files are in data/ladder_test_c/recipe.
 */
final class Recipes {
    static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, LadderTestC.MODID);
    static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, LadderTestC.MODID);

    static final DeferredHolder<RecipeType<?>, RecipeType<Infusing>> INFUSING = TYPES.register("infusing",
            () -> RecipeType.simple(Payloads.id("infusing")));
    static final DeferredHolder<RecipeSerializer<?>, InfusingSerializer> INFUSING_SERIALIZER = SERIALIZERS.register("infusing", InfusingSerializer::new);
    static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<Doubling>> DOUBLING = SERIALIZERS.register("doubling",
            () -> new SimpleCraftingRecipeSerializer<>(Doubling::new));

    private Recipes() {
    }

    static void register(IEventBus modBus) {
        TYPES.register(modBus);
        SERIALIZERS.register(modBus);
    }

    /** infusing: at least {@code count} of the ingredient (one stack) becomes the result */
    record Infusing(Ingredient ingredient, int count, ItemStack result) implements Recipe<SingleRecipeInput> {
        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
            return this.ingredient.test(input.item()) && input.item().getCount() >= this.count;
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            ItemStack out = this.result.copy();
            out.setCount(this.result.getCount() * (input.item().getCount() / this.count));
            return out;
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
        public NonNullList<ItemStack> getRemainingItems(SingleRecipeInput input) {
            NonNullList<ItemStack> left = NonNullList.withSize(1, ItemStack.EMPTY);
            ItemStack rest = input.item().copy();
            rest.shrink(this.count * (input.item().getCount() / this.count));
            left.set(0, rest);
            return left;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return INFUSING_SERIALIZER.get();
        }

        @Override
        public RecipeType<?> getType() {
            return INFUSING.get();
        }
    }

    static final class InfusingSerializer implements RecipeSerializer<Infusing> {
        private static final MapCodec<Infusing> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(Infusing::ingredient),
                com.mojang.serialization.Codec.INT.optionalFieldOf("count", 1).forGetter(Infusing::count),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(Infusing::result)).apply(i, Infusing::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, Infusing> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, Infusing::ingredient,
                ByteBufCodecs.VAR_INT, Infusing::count,
                ItemStack.STREAM_CODEC, Infusing::result,
                Infusing::new);

        @Override
        public MapCodec<Infusing> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Infusing> streamCodec() {
            return STREAM_CODEC;
        }
    }

    /** doubling: one stack alone in the grid becomes twice as many (a crafting recipe of the mod's own class) */
    static final class Doubling extends CustomRecipe {
        Doubling(CraftingBookCategory category) {
            super(category);
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return input.ingredientCount() == 1 && input.size() == 1 && input.getItem(0).getCount() * 2 <= input.getItem(0).getMaxStackSize();
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            return input.getItem(0).copyWithCount(input.getItem(0).getCount() * 2);
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return true;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return DOUBLING.get();
        }
    }
}
