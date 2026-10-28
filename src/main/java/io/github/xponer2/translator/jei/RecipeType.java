package io.github.xponer2.translator.jei;

/**
 * A JEI recipe category id with its recipe class (JEI 1.21 {@code RecipeType}); JEI 4.16 knows categories by a
 * string uid, {@link #getUid()}.
 */
public final class RecipeType<T> {
    private final String uid;
    private final Class<? extends T> recipeClass;

    private RecipeType(String uid, Class<? extends T> recipeClass) {
        this.uid = uid;
        this.recipeClass = recipeClass;
    }

    public static <T> RecipeType<T> create(String namespace, String path, Class<? extends T> recipeClass) {
        return new RecipeType<>(namespace + ":" + path, recipeClass);
    }

    public String getUid() {
        return this.uid;
    }

    public Class<? extends T> getRecipeClass() {
        return this.recipeClass;
    }

    @Override
    public String toString() {
        return this.uid;
    }
}
