package io.github.xponer2.translator.recipe;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.util.Codecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The grid of a shaped recipe (1.21 {@code ShapedRecipePattern}): JSON {@code "key"} + {@code "pattern"}.
 */
public final class ShapedRecipePattern {
    private static final int MAX = 3;
    private static final Codec<Character> SYMBOL = Codec.STRING.comapFlatMap(s -> s.length() == 1 && !" ".equals(s)
            ? DataResult.success(s.charAt(0)) : DataResult.error("Invalid key entry: '" + s + "' is an invalid symbol (must be 1 character only)."), String::valueOf);

    public static final MapCodec<ShapedRecipePattern> MAP_CODEC = Data.MAP_CODEC.flatXmap(ShapedRecipePattern::unpack,
            pattern -> pattern.data.map(DataResult::success).orElseGet(() -> DataResult.error("Cannot encode unpacked recipe")));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipePattern> STREAM_CODEC = StreamCodec.of(
            (buf, pattern) -> {
                buf.writeVarInt(pattern.width);
                buf.writeVarInt(pattern.height);
                for (var ingredient : pattern.ingredients) {
                    ByteBufCodecs.INGREDIENT.encode(buf, ingredient);
                }
            },
            buf -> {
                int width = buf.readVarInt();
                int height = buf.readVarInt();
                var ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
                for (int i = 0; i < ingredients.size(); i++) {
                    ingredients.set(i, ByteBufCodecs.INGREDIENT.decode(buf));
                }
                return new ShapedRecipePattern(width, height, ingredients, Optional.empty());
            });

    private final int width;
    private final int height;
    private final NonNullList<Ingredient> ingredients;
    public final Optional<Data> data;
    private final int ingredientCount;
    private final boolean symmetrical;

    public ShapedRecipePattern(int width, int height, NonNullList<Ingredient> ingredients, Optional<Data> data) {
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.data = data;
        int count = 0;
        for (var ingredient : ingredients) {
            if (ingredient != Ingredient.EMPTY) {
                count++;
            }
        }
        this.ingredientCount = count;
        boolean symmetrical = true;
        for (int y = 0; y < height && symmetrical; y++) {
            for (int x = 0; x < width / 2; x++) {
                if (ingredients.get(x + y * width) != ingredients.get(width - 1 - x + y * width)) {
                    symmetrical = false;
                    break;
                }
            }
        }
        this.symmetrical = symmetrical;
    }

    public static int getMaxWidth() {
        return MAX;
    }

    public static int getMaxHeight() {
        return MAX;
    }

    public static ShapedRecipePattern of(Map<Character, Ingredient> key, String... pattern) {
        return of(key, Arrays.asList(pattern));
    }

    public static ShapedRecipePattern of(Map<Character, Ingredient> key, List<String> pattern) {
        var data = new Data(key, pattern);
        return unpack(data).result().orElseThrow(() -> new IllegalArgumentException(unpack(data).error().get().message()));
    }

    public static DataResult<ShapedRecipePattern> unpack(Data data) {
        var pattern = data.pattern;
        int height = pattern.size();
        int width = height == 0 ? 0 : pattern.get(0).length();
        if (height == 0 || height > MAX || width == 0 || width > MAX) {
            return DataResult.error("Invalid pattern: size must be between 1x1 and " + MAX + "x" + MAX);
        }
        var ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
        var unused = new HashSet<>(data.key.keySet());
        for (int y = 0; y < height; y++) {
            var row = pattern.get(y);
            if (row.length() != width) {
                return DataResult.error("Invalid pattern: each row must be the same width");
            }
            for (int x = 0; x < width; x++) {
                char c = row.charAt(x);
                if (c == ' ') {
                    continue;
                }
                var ingredient = data.key.get(c);
                if (ingredient == null) {
                    return DataResult.error("Pattern references symbol '" + c + "' but it's not defined in the key");
                }
                unused.remove(c);
                ingredients.set(x + y * width, ingredient);
            }
        }
        if (!unused.isEmpty()) {
            return DataResult.error("Key defines symbols that aren't used in pattern: " + unused);
        }
        return DataResult.success(new ShapedRecipePattern(width, height, ingredients, Optional.of(data)));
    }

    public boolean matches(CraftingInput input) {
        if (input.ingredientCount() != this.ingredientCount || input.width() != this.width || input.height() != this.height) {
            return false;
        }
        return this.matches(input, false) || (!this.symmetrical && this.matches(input, true));
    }

    private boolean matches(CraftingInput input, boolean mirrored) {
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                var ingredient = mirrored ? this.ingredients.get(this.width - 1 - x + y * this.width) : this.ingredients.get(x + y * this.width);
                if (!ingredient.apply(input.getItem(x, y))) {
                    return false;
                }
            }
        }
        return true;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public NonNullList<Ingredient> ingredients() {
        return this.ingredients;
    }

    public Optional<Data> data() {
        return this.data;
    }

    /**
     * The JSON form: symbols and rows.
     */
    public static final class Data {
        public static final MapCodec<Data> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.unboundedMap(SYMBOL, Codecs.INGREDIENT_NONEMPTY).fieldOf("key").forGetter(Data::key),
                Codec.STRING.listOf().fieldOf("pattern").forGetter(Data::pattern)
        ).apply(instance, Data::new));

        private final Map<Character, Ingredient> key;
        private final List<String> pattern;

        public Data(Map<Character, Ingredient> key, List<String> pattern) {
            this.key = new LinkedHashMap<>(key);
            this.pattern = pattern;
        }

        public Map<Character, Ingredient> key() {
            return this.key;
        }

        public List<String> pattern() {
            return this.pattern;
        }
    }
}
