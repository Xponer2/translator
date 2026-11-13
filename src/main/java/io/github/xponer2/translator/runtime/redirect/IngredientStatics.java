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
package io.github.xponer2.translator.runtime.redirect;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.util.Codecs;

import net.minecraft.item.crafting.Ingredient;

/** The static fields 1.21 {@code Ingredient} has and 1.12.2's doesn't (runtime): its codecs, in the 1.21 formats. */
public final class IngredientStatics {
	/**
	 * 1.21 {@code Ingredient.of(ItemLike...)}: the items (or blocks) as an ingredient. The 1.21.1 code of it builds the
	 * ingredient with a constructor 1.12.2's lacks, so it was null: every mob's breeding food (Twilight Forest's boars,
	 * deer, penguins...) failed when the mob was made.
	 */
	public static Ingredient of(io.github.xponer2.translator.util.ItemLike... items) {
		var list = new net.minecraft.item.Item[items.length];
		int n = 0;
		for (Object value : (Object[]) items) {
			var item = io.github.xponer2.translator.util.ItemLike.item(value);
			if (item != null && item != net.minecraft.init.Items.AIR) {
				list[n++] = item;
			}
		}
		return n == 0 ? Ingredient.EMPTY : io.github.xponer2.translator.recipe.ingredient.Ingredients.of(java.util.Arrays.copyOf(list, n));
	}

	/** 1.21 {@code Ingredient.of()}: the empty ingredient. */
	public static Ingredient of() {
		return Ingredient.EMPTY;
	}

	/** 1.21 {@code Ingredient.of(Stream<ItemStack>)}. */
	public static Ingredient of(java.util.stream.Stream<net.minecraft.item.ItemStack> stacks) {
		return io.github.xponer2.translator.recipe.ingredient.Ingredients.of(stacks);
	}

	/** 1.21 {@code Ingredient.of(TagKey<Item>)}. */
	public static Ingredient of(io.github.xponer2.translator.util.TagKey<net.minecraft.item.Item> tag) {
		return io.github.xponer2.translator.recipe.ingredient.Ingredients.of(tag);
	}

	public static final Codec<Ingredient> CODEC = Codecs.INGREDIENT;
	public static final Codec<Ingredient> CODEC_NONEMPTY = Codecs.INGREDIENT_NONEMPTY;
	public static final Codec<List<Ingredient>> LIST_CODEC = CODEC.listOf();
	public static final Codec<List<Ingredient>> LIST_CODEC_NONEMPTY = LIST_CODEC.flatXmap(IngredientStatics::nonEmpty, IngredientStatics::nonEmpty);
	public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC = ByteBufCodecs.INGREDIENT;

	private IngredientStatics() {
	}

	private static DataResult<List<Ingredient>> nonEmpty(List<Ingredient> list) {
		return list.isEmpty() ? DataResult.error("Recipe has no ingredients") : DataResult.success(list);
	}
}
