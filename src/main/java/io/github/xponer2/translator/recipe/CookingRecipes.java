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

import java.lang.reflect.Method;

/**
 * 1.21 {@code AbstractCookingRecipe} getters on any cooking recipe: the furnace's (a facade, no subclass of the carried
 * class) and the carried blasting, smoking and campfire ones. JEI's smelting recipe extends the furnace's and names the
 * getters by the carried class in its codec: the method reference failed to link and JEI did not load.
 */
public final class CookingRecipes {
	private CookingRecipes() {
	}

	public static float getExperience(Object recipe) {
		if (recipe instanceof VanillaRecipes.SmeltingRecipe smelting) {
			return smelting.getExperience();
		}
		Object value = call(recipe, "getExperience");
		return value instanceof Float f ? f : 0.0F;
	}

	public static int getCookingTime(Object recipe) {
		if (recipe instanceof VanillaRecipes.SmeltingRecipe smelting) {
			return smelting.getCookingTime();
		}
		Object value = call(recipe, "getCookingTime");
		return value instanceof Integer i ? i : 200;
	}

	private static Object call(Object recipe, String name) {
		if (recipe == null) {
			return null;
		}
		try {
			Method method = recipe.getClass().getMethod(name);
			method.setAccessible(true);
			return method.invoke(recipe);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
