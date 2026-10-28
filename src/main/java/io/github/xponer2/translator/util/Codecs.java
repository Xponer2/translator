package io.github.xponer2.translator.util;

import io.github.xponer2.translator.component.DataComponentPatch;
import io.github.xponer2.translator.component.StackComponents;
import io.github.xponer2.translator.recipe.ingredient.CustomIngredient;
import io.github.xponer2.translator.recipe.ingredient.ICustomIngredient;
import io.github.xponer2.translator.recipe.ingredient.IngredientType;
import io.github.xponer2.translator.registry.Holder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.RecipeItemHelper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Codecs that 1.21 keeps on the types themselves ({@code ItemStack.CODEC}, {@code Ingredient.CODEC},
 * {@code ResourceLocation.CODEC}, ...; the converter rewrites those uses to these fields), in the 1.21 formats.
 * Items are found by their 1.21 ids ({@link ItemIds}).
 * <ul>
 * <li>{@link #ITEM_STACK}: {@code {"id", "count" (1), "components"}}; the components are those of
 * {@link StackComponents}, so 1.12.2 NBT travels as {@code minecraft:custom_data}.</li>
 * <li>{@link #INGREDIENT}: {@code {"item": id}}, {@code {"tag": id}}, a list of these, or a {@code "type"} object of
 * a registered custom ingredient ({@link #registerIngredientType}). Tags are resolved lazily on first use through
 * {@link #setItemTagResolver} (the tag loader). An item without subtypes matches any damage.</li>
 * </ul>
 */
public final class Codecs {
    private static final Map<ResourceLocation, Codec<? extends Ingredient>> INGREDIENT_TYPES = new ConcurrentHashMap<>();
    private static volatile Function<ResourceLocation, List<ItemStack>> itemTagResolver = tag -> {
        org.apache.logging.log4j.LogManager.getLogger("Translator").debug("Item tag {} used before the tags were loaded", tag, new Throwable());
        return Collections.emptyList();
    };
    private static volatile int tagGeneration;

    public static final Codec<ResourceLocation> RESOURCE_LOCATION = Codec.STRING.comapFlatMap(Codecs::parseLocation, ResourceLocation::toString);

    public static final Codec<NBTTagCompound> COMPOUND_TAG = Codec.PASSTHROUGH.comapFlatMap(Codecs::readCompound,
            tag -> new Dynamic<>(NbtOps.INSTANCE, tag.copy()));

    public static final Codec<ITextComponent> COMPONENT = Codec.PASSTHROUGH.comapFlatMap(dynamic -> {
        try {
            var component = ITextComponent.Serializer.fromJsonLenient(dynamic.convert(JsonOps.INSTANCE).getValue().toString());
            return component != null ? DataResult.success(component) : DataResult.error("Not a text component: " + dynamic);
        } catch (RuntimeException e) {
            return DataResult.error("Not a text component: " + e.getMessage());
        }
    }, component -> new Dynamic<>(JsonOps.INSTANCE, new JsonParser().parse(ITextComponent.Serializer.componentToJson(component))));

    public static final Codec<BlockPos> BLOCK_POS = Codec.INT_STREAM.comapFlatMap(stream -> {
        int[] values = stream.limit(4).toArray();
        return values.length == 3 ? DataResult.success(new BlockPos(values[0], values[1], values[2]))
                : DataResult.error("A block position needs 3 numbers: " + java.util.Arrays.toString(values));
    }, pos -> IntStream.of(pos.getX(), pos.getY(), pos.getZ()));

    public static final Codec<EnumFacing> DIRECTION = Codec.STRING.comapFlatMap(name -> {
        var facing = EnumFacing.byName(name);
        return facing != null ? DataResult.success(facing) : DataResult.error("Unknown direction " + name);
    }, EnumFacing::getName);

    public static final Codec<IBlockState> BLOCK_STATE = COMPOUND_TAG.xmap(NBTUtil::readBlockState,
            state -> NBTUtil.writeBlockState(new NBTTagCompound(), state));

    public static final Codec<Item> ITEM = RESOURCE_LOCATION.comapFlatMap(id -> {
        var item = ItemIds.item(id);
        return item != null ? DataResult.success(item) : DataResult.error("Unknown item " + id);
    }, Item::getRegistryName);

    /**
     * An item that isn't air, as a holder (1.21 {@code ItemStack.ITEM_NON_AIR_CODEC}).
     */
    public static final Codec<Holder<Item>> ITEM_NON_AIR = ITEM.flatXmap(
            item -> item == Items.AIR ? DataResult.error("Item must not be air") : DataResult.success(BuiltInRegistries.ITEM.wrapAsHolder(item)),
            holder -> holder.value() == Items.AIR ? DataResult.error("Item must not be air") : DataResult.success(holder.value()));

    public static final Codec<Block> BLOCK = BuiltInRegistries.BLOCK.byNameCodec();

    public static final Codec<Holder<Enchantment>> ENCHANTMENT = BuiltInRegistries.ENCHANTMENT.byNameCodec()
            .xmap(BuiltInRegistries.ENCHANTMENT::wrapAsHolder, Holder::value);

    /**
     * A 1.12.2 fluid by name; 1.21 ids like {@code minecraft:water} are read as {@code water}.
     */
    public static final Codec<Fluid> FLUID = Codec.STRING.comapFlatMap(name -> {
        var fluid = FluidRegistry.getFluid(name);
        if (fluid == null && name.indexOf(':') >= 0) {
            fluid = FluidRegistry.getFluid(name.substring(name.indexOf(':') + 1));
        }
        return fluid != null ? DataResult.success(fluid) : DataResult.error("Unknown fluid " + name);
    }, Fluid::getName);

    public static final Codec<FluidStack> FLUID_STACK = RecordCodecBuilder.create(instance -> instance.group(
            FLUID.fieldOf("id").forGetter(FluidStack::getFluid),
            Codec.INT.fieldOf("amount").forGetter(stack -> stack.amount)
    ).apply(instance, FluidStack::new));

    private static final Codec<ItemStack> ANY_ITEM_STACK = RecordCodecBuilder.create(instance -> instance.group(
            RESOURCE_LOCATION.fieldOf("id").forGetter(ItemIds::id),
            Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(ItemStack::getCount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(StackComponents::getComponentsPatch)
    ).apply(instance, Codecs::createStack));

    /**
     * A stack that isn't empty (1.21 {@code ItemStack.CODEC}).
     */
    public static final Codec<ItemStack> ITEM_STACK = ANY_ITEM_STACK.flatXmap(Codecs::nonEmpty, Codecs::nonEmpty);

    /**
     * A stack, empty as {@code {}} (1.21 {@code ItemStack.OPTIONAL_CODEC}).
     */
    public static final Codec<ItemStack> OPTIONAL_ITEM_STACK = Codec.PASSTHROUGH.comapFlatMap(
            dynamic -> dynamic.getMapValues().result().map(Map::isEmpty).orElse(false)
                    ? DataResult.success(ItemStack.EMPTY) : ITEM_STACK.parse(dynamic),
            stack -> stack.isEmpty() ? new Dynamic<>(JsonOps.INSTANCE, new JsonObject())
                    : new Dynamic<>(JsonOps.INSTANCE, ITEM_STACK.encodeStart(JsonOps.INSTANCE, stack).getOrThrow(false, e -> { })));

    /**
     * One item with components (1.21 {@code ItemStack.SINGLE_ITEM_CODEC}).
     */
    public static final Codec<ItemStack> SINGLE_ITEM = RecordCodecBuilder.<ItemStack>create(instance -> instance.group(
            RESOURCE_LOCATION.fieldOf("id").forGetter(ItemIds::id),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(StackComponents::getComponentsPatch)
    ).apply(instance, (id, components) -> createStack(id, 1, components))).flatXmap(Codecs::nonEmpty, Codecs::nonEmpty);

    /**
     * 1.21 {@code Ingredient.CODEC}; may be empty ({@code []}).
     */
    public static final Codec<Ingredient> INGREDIENT = Codec.PASSTHROUGH.comapFlatMap(Codecs::decodeIngredient,
            ingredient -> new Dynamic<>(JsonOps.INSTANCE, encodeIngredient(ingredient)));

    /**
     * 1.21 {@code Ingredient.CODEC_NONEMPTY}.
     */
    public static final Codec<Ingredient> INGREDIENT_NONEMPTY = INGREDIENT.flatXmap(Codecs::nonEmpty, Codecs::nonEmpty);

    private Codecs() { }

    /**
     * Sets where the items of an item tag come from (the tag loader); until then tag ingredients match nothing.
     */
    public static void setItemTagResolver(Function<ResourceLocation, List<ItemStack>> resolver) {
        itemTagResolver = resolver;
        // tag ingredients resolved before (empty, or with the old tags) resolve again
        tagGeneration++;
    }

    /**
     * Registers the codec of a custom ingredient type ({@code {"type": type, ...}} objects; NeoForge
     * {@code IngredientType}).
     */
    public static void registerIngredientType(ResourceLocation type, Codec<? extends Ingredient> codec) {
        INGREDIENT_TYPES.put(type, codec);
    }

    private static DataResult<ResourceLocation> parseLocation(String location) {
        try {
            return DataResult.success(io.github.xponer2.translator.util.ResourceLocations.parse(location));
        } catch (RuntimeException e) {
            return DataResult.error("Not a valid resource location: " + location);
        }
    }

    private static DataResult<NBTTagCompound> readCompound(Dynamic<?> dynamic) {
        var string = dynamic.asString().result();
        if (string.isPresent()) {
            try {
                return DataResult.success(JsonToNBT.getTagFromJson(string.get()));
            } catch (NBTException e) {
                return DataResult.error("Bad NBT: " + e.getMessage());
            }
        }
        NBTBase nbt = dynamic.convert(NbtOps.INSTANCE).getValue();
        return nbt instanceof NBTTagCompound ? DataResult.success(((NBTTagCompound) nbt).copy())
                : DataResult.error("Not a compound tag: " + nbt);
    }

    private static ItemStack createStack(ResourceLocation id, int count, DataComponentPatch components) {
        var stack = ItemIds.resultStack(id, count);
        if (!stack.isEmpty()) {
            StackComponents.applyComponents(stack, components);
        }
        return stack;
    }

    private static DataResult<ItemStack> nonEmpty(ItemStack stack) {
        return stack.isEmpty() ? DataResult.error("Empty or unknown ItemStack not allowed") : DataResult.success(stack);
    }

    private static DataResult<Ingredient> nonEmpty(Ingredient ingredient) {
        return ingredient == Ingredient.EMPTY ? DataResult.error("Empty ingredient not allowed") : DataResult.success(ingredient);
    }

    private static DataResult<Ingredient> decodeIngredient(Dynamic<?> input) {
        var list = input.asStreamOpt().result();
        if (list.isPresent()) {
            var parts = new ArrayList<Ingredient>();
            for (var element : list.get().collect(Collectors.toList())) {
                var part = decodeIngredient(element);
                if (!part.result().isPresent()) {
                    return part;
                }
                parts.add(part.result().get());
            }
            return DataResult.success(parts.isEmpty() ? Ingredient.EMPTY : parts.size() == 1 ? parts.get(0) : Ingredient.merge(parts));
        }
        var type = input.get("type").asString().result();
        if (type.isPresent()) {
            var id = io.github.xponer2.translator.util.ResourceLocations.parse(type.get());
            var codec = INGREDIENT_TYPES.get(id);
            if (codec != null) {
                return codec.parse(input).map(i -> (Ingredient) i);
            }
            var ingredientType = IngredientType.byId(id);
            return ingredientType == null ? DataResult.error("Unknown ingredient type " + id)
                    : ingredientType.codec().codec().parse(input).map(ICustomIngredient::toVanilla);
        }
        var item = input.get("item").asString().result();
        if (item.isPresent()) {
            var stack = ItemIds.stack(io.github.xponer2.translator.util.ResourceLocations.parse(item.get()), 1);
            if (stack.isEmpty()) {
                return DataResult.error("Unknown item " + item.get());
            }
            if (!stack.getHasSubtypes()) {
                stack.setItemDamage(OreDictionary.WILDCARD_VALUE);
            }
            return DataResult.success(Ingredient.fromStacks(stack));
        }
        var tag = input.get("tag").asString().result();
        if (tag.isPresent()) {
            return DataResult.success(new TagIngredient(io.github.xponer2.translator.util.ResourceLocations.parse(tag.get())));
        }
        return DataResult.error("Not an ingredient: " + input.getValue());
    }

    private static JsonElement encodeIngredient(Ingredient ingredient) {
        if (ingredient instanceof CustomIngredient) {
            var custom = ((CustomIngredient) ingredient).getCustomIngredient();
            var id = custom.getType().id();
            var json = encodeCustom(custom.getType(), custom);
            if (id != null && json.isJsonObject()) {
                json.getAsJsonObject().addProperty("type", id.toString());
                return json;
            }
        }
        if (ingredient instanceof TagIngredient) {
            var json = new JsonObject();
            json.addProperty("tag", ((TagIngredient) ingredient).tag.toString());
            return json;
        }
        var stacks = ingredient.getMatchingStacks();
        var list = new JsonArray();
        for (var stack : stacks) {
            var json = new JsonObject();
            var id = ItemIds.id(stack);
            json.addProperty("item", id != null ? id.toString() : "minecraft:air");
            list.add(json);
        }
        return list.size() == 1 ? list.get(0) : list;
    }

    @SuppressWarnings("unchecked")
    private static <T extends ICustomIngredient> JsonElement encodeCustom(IngredientType<T> type, ICustomIngredient custom) {
        return type.codec().codec().encodeStart(JsonOps.INSTANCE, (T) custom).result().orElseGet(JsonObject::new);
    }

    /**
     * The items of an item tag, by the tag loader ({@link #setItemTagResolver}).
     */
    public static List<ItemStack> itemsOfTag(ResourceLocation tag) {
        return itemTagResolver.apply(tag);
    }

    /**
     * An ingredient of an item tag, resolved on first use.
     */
    public static Ingredient tagIngredient(ResourceLocation tag) {
        return new TagIngredient(tag);
    }

    /**
     * An ingredient of an item tag, resolved on first use.
     */
    private static final class TagIngredient extends Ingredient {
        private final ResourceLocation tag;
        @Nullable
        private ItemStack[] stacks;
        @Nullable
        private IntList packed;
        private int generation = -1;

        TagIngredient(ResourceLocation tag) {
            super(0);
            this.tag = tag;
        }

        @Override
        public ItemStack[] getMatchingStacks() {
            if (this.stacks == null || this.generation != tagGeneration) {
                this.generation = tagGeneration;
                this.stacks = itemTagResolver.apply(this.tag).toArray(new ItemStack[0]);
                this.packed = null;
            }
            return this.stacks;
        }

        @Override
        public boolean apply(@Nullable ItemStack input) {
            if (input == null || input.isEmpty()) {
                return false;
            }
            for (var stack : this.getMatchingStacks()) {
                if (OreDictionary.itemMatches(stack, input, false)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public IntList getValidItemStacksPacked() {
            if (this.packed == null || this.generation != tagGeneration) {
                this.getMatchingStacks();
                var packed = new IntArrayList();
                for (var stack : this.getMatchingStacks()) {
                    packed.add(RecipeItemHelper.pack(stack));
                }
                packed.sort(null);
                this.packed = packed;
            }
            return this.packed;
        }

        @Override
        public boolean isSimple() {
            return false;
        }

        @Override
        protected void invalidate() {
            this.stacks = null;
            this.packed = null;
        }
    }
}
