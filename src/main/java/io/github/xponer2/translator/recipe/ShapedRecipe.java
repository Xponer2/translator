package io.github.xponer2.translator.recipe;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.registry.HolderLookup;
import io.github.xponer2.translator.util.Codecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

/**
 * A shaped crafting recipe (1.21 {@code ShapedRecipe}, type {@code minecraft:crafting_shaped}).
 */
public class ShapedRecipe implements CraftingRecipe {
    protected final ShapedRecipePattern pattern;
    protected final ItemStack result;
    protected final String group;
    protected final CraftingBookCategory category;
    protected final boolean showNotification;

    public ShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification) {
        this.group = group;
        this.category = category;
        this.pattern = pattern;
        this.result = result;
        this.showNotification = showNotification;
    }

    public ShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result) {
        this(group, category, pattern, result, true);
    }

    @Override
    public boolean matches(CraftingInput input, World level) {
        return this.pattern.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return this.getResultItem(registries).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.pattern.width() && height >= this.pattern.height();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.pattern.ingredients();
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public boolean showNotification() {
        return this.showNotification;
    }

    public int getWidth() {
        return this.pattern.width();
    }

    public int getHeight() {
        return this.pattern.height();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return VanillaRecipes.SHAPED;
    }

    public static class Serializer implements RecipeSerializer<ShapedRecipe> {
        public static final MapCodec<ShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(r -> r.category),
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
                Codecs.ITEM_STACK.fieldOf("result").forGetter(r -> r.result),
                Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(r -> r.showNotification)
        ).apply(instance, ShapedRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, recipe) -> {
                    buf.writeUtf(recipe.group);
                    buf.writeEnumValue(recipe.category);
                    ShapedRecipePattern.STREAM_CODEC.encode(buf, recipe.pattern);
                    ByteBufCodecs.ITEM_STACK.encode(buf, recipe.result);
                    buf.writeBoolean(recipe.showNotification);
                },
                buf -> new ShapedRecipe(buf.readUtf(), buf.readEnumValue(CraftingBookCategory.class), ShapedRecipePattern.STREAM_CODEC.decode(buf),
                        ByteBufCodecs.ITEM_STACK.decode(buf), buf.readBoolean()));

        @Override
        public MapCodec<ShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
