/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.recipe;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import ladder.carried.net.minecraft.network.codec.StreamCodec;
import ladder.carried.net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/**
 * NeoForge 20.5's custom ingredients on 20.4: a 20.5 custom ingredient (an ICustomIngredient, carried) is an ingredient
 * here through {@link Wrapped}, a subclass of 20.4's Ingredient that asks it; its type (20.5: a map codec and a stream
 * codec) is a 20.4 IngredientType whose codec reads and writes the wrapped ingredient with the mod's map codec, so 20.4's
 * ingredient codec (a "type" dispatch over the same registry) reads the mod's data files. On the network a simple one
 * goes by its items, as 20.5's does; another by its codec.
 */
public final class CustomIngredients {
    private CustomIngredients() {
    }

    /** {@code new Ingredient(custom)} and {@code custom.toVanilla()} */
    public static Ingredient of(ICustomIngredient custom) {
        return new Wrapped(custom);
    }

    /** {@code new IngredientType(codec)} */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static IngredientType<?> type(MapCodec<? extends ICustomIngredient> codec) {
        Codec<Ingredient> wrapped = ((MapCodec) codec).codec().xmap(custom -> of((ICustomIngredient) custom),
                ingredient -> ((Wrapped) ingredient).custom);
        return new IngredientType(wrapped);
    }

    /** {@code new IngredientType(codec, streamCodec)}: 20.4 sends a custom ingredient by its codec (or its items) */
    public static IngredientType<?> type(MapCodec<? extends ICustomIngredient> codec, StreamCodec<?, ?> streamCodec) {
        return type(codec);
    }

    /** {@code ingredient.isCustom()} */
    public static boolean isCustom(Ingredient ingredient) {
        return ingredient instanceof Wrapped;
    }

    /** {@code ingredient.getCustomIngredient()} */
    @Nullable
    public static ICustomIngredient getCustomIngredient(Ingredient ingredient) {
        return ingredient instanceof Wrapped wrapped ? wrapped.custom : null;
    }

    /** {@code ingredient.hasNoItems()} */
    public static boolean hasNoItems(Ingredient ingredient) {
        return ingredient.getItems().length == 0;
    }

    /** a 20.5 custom ingredient as a 20.4 ingredient */
    public static final class Wrapped extends Ingredient {
        final ICustomIngredient custom;
        private ItemStack[] items;

        Wrapped(ICustomIngredient custom) {
            super(Stream.empty(), () -> (IngredientType<?>) (Object) custom.getType());
            this.custom = custom;
        }

        public ICustomIngredient custom() {
            return this.custom;
        }

        @Override
        public ItemStack[] getItems() {
            if (this.items == null) {
                this.items = this.custom.getItems().toArray(ItemStack[]::new);
            }
            return this.items;
        }

        @Override
        public boolean test(@Nullable ItemStack stack) {
            return stack != null && this.custom.test(stack);
        }

        @Override
        public boolean isSimple() {
            return this.custom.isSimple();
        }

        @Override
        public boolean isEmpty() {
            // 20.5: a custom ingredient is never empty
            return false;
        }

        @Override
        public boolean synchronizeWithContents() {
            return this.custom.isSimple();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Wrapped wrapped && wrapped.custom.equals(this.custom);
        }

        @Override
        public int hashCode() {
            return this.custom.hashCode();
        }
    }
}
