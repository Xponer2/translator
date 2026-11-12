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

import com.mojang.serialization.Codec;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.recipe.Recipe;
import io.github.xponer2.translator.recipe.RecipeSerializer;
import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.Registries;

/**
 * 1.21 {@code Recipe.CODEC} and {@code Recipe.STREAM_CODEC}, which the Translator's Recipe interface hasn't: a recipe
 * by its serializer (its "type" id) and that serializer's codec, as 1.21 dispatches them. Create's sequenced assembly
 * steps (SequencedRecipe's static initializer was left empty), JEI's and Moonlight's recipe readers.
 */
public final class RecipeCodecs {
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final Codec<Recipe<?>> CODEC = BuiltInRegistries.RECIPE_SERIALIZER.byNameCodec()
			.<Recipe<?>>dispatch(recipe -> recipe.getSerializer(), serializer -> (Codec) ((RecipeSerializer) serializer).codec().codec());
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final StreamCodec<RegistryFriendlyByteBuf, Recipe<?>> STREAM_CODEC = (StreamCodec) ByteBufCodecs.registry(Registries.RECIPE_SERIALIZER)
			.dispatch(recipe -> ((Recipe<?>) recipe).getSerializer(), serializer -> (StreamCodec) ((RecipeSerializer) serializer).streamCodec());

	private RecipeCodecs() {
	}

	/**
	 * 1.21 {@code RecipeSerializer.BLASTING_RECIPE}, {@code SMOKING_RECIPE}, {@code CAMPFIRE_COOKING_RECIPE} and
	 * {@code SMITHING_TRANSFORM}: the carried 1.21 serializers (SimpleCookingSerializer of the carried cooking recipes,
	 * SmithingTransformRecipe's), which the Translator's interface hasn't. Null when the carried classes can't be made.
	 * VanillaRecipes registers them, so the mods' blasting, smoking, campfire and smithing recipes load.
	 */
	public static final class Serializers {
		public static final RecipeSerializer<?> BLASTING_RECIPE = cooking("BlastingRecipe", 100);
		public static final RecipeSerializer<?> SMOKING_RECIPE = cooking("SmokingRecipe", 100);
		public static final RecipeSerializer<?> CAMPFIRE_COOKING_RECIPE = cooking("CampfireCookingRecipe", 100);
		public static final RecipeSerializer<?> SMITHING_TRANSFORM = carried("net.minecraft.world.item.crafting.SmithingTransformRecipe$Serializer");

		private Serializers() {
		}

		@javax.annotation.Nullable
		private static RecipeSerializer<?> cooking(String recipe, int cookingTime) {
			try {
				ClassLoader loader = net.minecraft.launchwrapper.Launch.classLoader;
				Class<?> factory = Class.forName("net.minecraft.world.item.crafting.AbstractCookingRecipe$Factory", true, loader);
				Class<?> type = Class.forName("net.minecraft.world.item.crafting." + recipe, true, loader);
				java.lang.reflect.Constructor<?> constructor = null;
				for (java.lang.reflect.Constructor<?> candidate : type.getConstructors()) {
					if (candidate.getParameterCount() == 6) {
						constructor = candidate;
					}
				}
				if (constructor == null) {
					return null;
				}
				java.lang.reflect.Constructor<?> make = constructor;
				// 1.21 BlastingRecipe::new (group, category, ingredient, result, experience, cooking time)
				Object create = java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[] {factory}, (proxy, method, args) -> {
					switch (method.getName()) {
						case "create":
							return make.newInstance(args);
						case "hashCode":
							return System.identityHashCode(proxy);
						case "equals":
							return proxy == args[0];
						default:
							return recipe + "::new";
					}
				});
				Class<?> serializer = Class.forName("net.minecraft.world.item.crafting.SimpleCookingSerializer", true, loader);
				return (RecipeSerializer<?>) serializer.getConstructor(factory, int.class).newInstance(create, cookingTime);
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				org.apache.logging.log4j.LogManager.getLogger("Translator Runtime").warn("No 1.21 {} serializer: {}", recipe, e.toString());
				return null;
			}
		}

		@javax.annotation.Nullable
		private static RecipeSerializer<?> carried(String name) {
			try {
				return (RecipeSerializer<?>) Class.forName(name, true, net.minecraft.launchwrapper.Launch.classLoader).getConstructor().newInstance();
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				org.apache.logging.log4j.LogManager.getLogger("Translator Runtime").warn("No 1.21 serializer {}: {}", name, e.toString());
				return null;
			}
		}
	}
}
