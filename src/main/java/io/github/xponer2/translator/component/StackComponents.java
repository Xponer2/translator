package io.github.xponer2.translator.component;

import io.github.xponer2.translator.item.ItemProperties;
import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.NbtOps;
import io.github.xponer2.translator.util.Registries;
import com.google.common.collect.MapMaker;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * The data components of item stacks (1.21 {@code ItemStack#get/set/has/remove/update/getOrDefault}, called as
 * {@code StackComponents.get(stack, type)}; the converter rewrites the calls). A value lives in the stack's NBT under
 * {@value #TAG}, by component type id, encoded with the type's codec; without one the item's default
 * ({@link ItemProperties#defaults}) applies. Decoded values are cached per NBT tag. Types without a codec keep their
 * values in memory only (lost when the stack is copied), types with a {@link StackStorage} where 1.12.2 keeps them.
 * The same calls work on a {@link DataComponentMap} (and its builder), so a call the converter can't type still
 * compiles.
 */
public final class StackComponents {
    public static final String TAG = "Components";
    private static final Logger LOGGER = LogManager.getLogger("Cucumber Components");
    private static final Map<NBTBase, Object> DECODED = new MapMaker().weakKeys().makeMap();
    private static final Map<ItemStack, Map<DataComponentType<?>, Object>> TRANSIENT = new MapMaker().weakKeys().makeMap();

    private StackComponents() { }

    @Nullable
    public static <T> T get(ItemStack stack, DataComponentType<? extends T> type) {
        T value = stored(stack, type);
        return value != null ? value : defaultValue(stack, type);
    }

    @Nullable
    public static <T> T get(ItemStack stack, Supplier<? extends DataComponentType<? extends T>> type) {
        return get(stack, type.get());
    }

    public static <T> T getOrDefault(ItemStack stack, DataComponentType<? extends T> type, T defaultValue) {
        T value = get(stack, type);
        return value != null ? value : defaultValue;
    }

    public static <T> T getOrDefault(ItemStack stack, Supplier<? extends DataComponentType<? extends T>> type, T defaultValue) {
        return getOrDefault(stack, type.get(), defaultValue);
    }

    public static boolean has(ItemStack stack, DataComponentType<?> type) {
        return get(stack, type) != null;
    }

    public static boolean has(ItemStack stack, Supplier<? extends DataComponentType<?>> type) {
        return has(stack, type.get());
    }

    /**
     * Sets a value (null removes it) and returns the previous value of the stack.
     */
    @Nullable
    public static <T> T set(ItemStack stack, DataComponentType<T> type, @Nullable T value) {
        T previous = stored(stack, type);
        if (stack.isEmpty()) {
            return previous;
        }
        if (type.storage() != null) {
            type.storage().set(stack, value);
        } else if (type.isTransient()) {
            var values = TRANSIENT.computeIfAbsent(stack, s -> new LinkedHashMap<>());
            if (value == null) {
                values.remove(type);
            } else {
                values.put(type, value);
            }
        } else if (value == null) {
            removeStored(stack, type);
        } else {
            var nbt = type.codecOrThrow().encodeStart(NbtOps.INSTANCE, value)
                    .getOrThrow(false, error -> LOGGER.error("Can't encode data component {}: {}", type, error));
            componentsTag(stack, true).setTag(idOrThrow(type), nbt);
            DECODED.put(nbt, value);
        }
        return previous;
    }

    @Nullable
    public static <T> T set(ItemStack stack, Supplier<? extends DataComponentType<T>> type, @Nullable T value) {
        return set(stack, type.get(), value);
    }

    /**
     * Removes the stack's value (the item's default applies again) and returns it.
     */
    @Nullable
    public static <T> T remove(ItemStack stack, DataComponentType<? extends T> type) {
        T previous = stored(stack, type);
        if (previous != null) {
            removeValue(stack, type);
        }
        return previous;
    }

    @Nullable
    public static <T> T remove(ItemStack stack, Supplier<? extends DataComponentType<? extends T>> type) {
        return remove(stack, type.get());
    }

    @Nullable
    public static <T> T update(ItemStack stack, DataComponentType<T> type, T defaultValue, UnaryOperator<T> updater) {
        return set(stack, type, updater.apply(getOrDefault(stack, type, defaultValue)));
    }

    @Nullable
    public static <T> T update(ItemStack stack, Supplier<? extends DataComponentType<T>> type, T defaultValue, UnaryOperator<T> updater) {
        return update(stack, type.get(), defaultValue, updater);
    }

    @Nullable
    public static <T, U> T update(ItemStack stack, DataComponentType<T> type, T defaultValue, U argument, BiFunction<T, U, T> updater) {
        return set(stack, type, updater.apply(getOrDefault(stack, type, defaultValue), argument));
    }

    @Nullable
    public static <T, U> T update(ItemStack stack, Supplier<? extends DataComponentType<T>> type, T defaultValue, U argument,
                                  BiFunction<T, U, T> updater) {
        return update(stack, type.get(), defaultValue, argument, updater);
    }

    /**
     * Copies a component from another stack (1.21 {@code copyFrom}).
     */
    public static <T> void copyFrom(ItemStack stack, DataComponentType<T> type, ItemStack source) {
        set(stack, type, get(source, type));
    }

    /**
     * The item's defaults with the stack's values on top (1.21 {@code ItemStack#getComponents}).
     */
    public static DataComponentMap getComponents(ItemStack stack) {
        var values = new LinkedHashMap<DataComponentType<?>, Object>(ItemProperties.defaults(stack.getItem()));
        values.putAll(getComponentsPatch(stack).values());
        return values.isEmpty() ? DataComponentMap.EMPTY : new DataComponentMap(Collections.unmodifiableMap(values));
    }

    /**
     * The stack's own values (1.21 {@code ItemStack#getComponentsPatch}).
     */
    public static DataComponentPatch getComponentsPatch(ItemStack stack) {
        var values = new LinkedHashMap<DataComponentType<?>, Object>();
        for (var type : DataComponents.WITH_STORAGE) {
            var value = type.storage().get(stack);
            if (value != null) {
                values.put(type, value);
            }
        }
        var components = componentsTag(stack, false);
        if (components != null) {
            var registry = BuiltInRegistries.byKey(Registries.DATA_COMPONENT_TYPE);
            for (var key : components.getKeySet()) {
                var type = registry.get(new net.minecraft.util.ResourceLocation(key));
                Object value = type == null ? null : stored(stack, type);
                if (value != null) {
                    values.put(type, value);
                }
            }
        }
        var transientValues = TRANSIENT.get(stack);
        if (transientValues != null) {
            values.putAll(transientValues);
        }
        return values.isEmpty() ? DataComponentPatch.EMPTY : new DataComponentPatch(Collections.unmodifiableMap(values));
    }

    public static void applyComponents(ItemStack stack, DataComponentMap components) {
        components.values().forEach((type, value) -> setUnchecked(stack, type, value));
    }

    public static void applyComponents(ItemStack stack, DataComponentPatch patch) {
        patch.values().forEach((type, value) -> setUnchecked(stack, type, value));
    }

    /**
     * Same item, damage and NBT (1.21 {@code ItemStack.isSameItemSameComponents}).
     */
    public static boolean isSameItemSameComponents(ItemStack a, ItemStack b) {
        return ItemStack.areItemsEqual(a, b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    /**
     * A copy with another count (1.21 {@code ItemStack#copyWithCount}).
     */
    public static ItemStack copyWithCount(ItemStack stack, int count) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    // The same calls on component maps, for receivers the converter can't tell from stacks

    @Nullable
    public static <T> T get(DataComponentMap map, DataComponentType<? extends T> type) {
        return map.get(type);
    }

    @Nullable
    public static <T> T get(DataComponentMap map, Supplier<? extends DataComponentType<? extends T>> type) {
        return map.get(type);
    }

    public static <T> T getOrDefault(DataComponentMap map, DataComponentType<? extends T> type, T defaultValue) {
        return map.getOrDefault(type, defaultValue);
    }

    public static <T> T getOrDefault(DataComponentMap map, Supplier<? extends DataComponentType<? extends T>> type, T defaultValue) {
        return map.getOrDefault(type, defaultValue);
    }

    public static boolean has(DataComponentMap map, DataComponentType<?> type) {
        return map.has(type);
    }

    public static boolean has(DataComponentMap map, Supplier<? extends DataComponentType<?>> type) {
        return map.has(type);
    }

    public static <T> DataComponentMap.Builder set(DataComponentMap.Builder builder, DataComponentType<T> type, @Nullable T value) {
        return builder.set(type, value);
    }

    public static <T> DataComponentMap.Builder set(DataComponentMap.Builder builder, Supplier<? extends DataComponentType<T>> type,
                                                   @Nullable T value) {
        return builder.set(type, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> void setUnchecked(ItemStack stack, DataComponentType<T> type, Object value) {
        set(stack, type, (T) value);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T> T defaultValue(ItemStack stack, DataComponentType<? extends T> type) {
        return stack.isEmpty() ? null : (T) ItemProperties.defaults(stack.getItem()).get(type);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T> T stored(ItemStack stack, DataComponentType<? extends T> type) {
        if (stack.isEmpty()) {
            return null;
        }
        if (type.storage() != null) {
            return type.storage().get(stack);
        }
        if (type.isTransient()) {
            var values = TRANSIENT.get(stack);
            return values == null ? null : (T) values.get(type);
        }
        var components = componentsTag(stack, false);
        var id = type.id();
        if (components == null || id == null) {
            return null;
        }
        var nbt = components.getTag(id.toString());
        if (nbt == null) {
            return null;
        }
        var cached = DECODED.get(nbt);
        if (cached != null) {
            return (T) cached;
        }
        T value = type.codecOrThrow().parse(NbtOps.INSTANCE, nbt)
                .resultOrPartial(error -> LOGGER.warn("Bad data component {} on {}: {}", id, stack, error)).orElse(null);
        if (value != null) {
            DECODED.put(nbt, value);
        }
        return value;
    }

    private static void removeValue(ItemStack stack, DataComponentType<?> type) {
        if (type.storage() != null) {
            type.storage().set(stack, null);
        } else if (type.isTransient()) {
            var values = TRANSIENT.get(stack);
            if (values != null) {
                values.remove(type);
            }
        } else {
            removeStored(stack, type);
        }
    }

    private static void removeStored(ItemStack stack, DataComponentType<?> type) {
        var components = componentsTag(stack, false);
        var id = type.id();
        if (components == null || id == null) {
            return;
        }
        components.removeTag(id.toString());
        if (components.isEmpty()) {
            var tag = stack.getTagCompound();
            tag.removeTag(TAG);
            if (tag.isEmpty()) {
                stack.setTagCompound(null);
            }
        }
    }

    @Nullable
    private static NBTTagCompound componentsTag(ItemStack stack, boolean create) {
        var tag = stack.getTagCompound();
        if (tag == null) {
            if (!create) {
                return null;
            }
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        if (!tag.hasKey(TAG, Constants.NBT.TAG_COMPOUND)) {
            if (!create) {
                return null;
            }
            tag.setTag(TAG, new NBTTagCompound());
        }
        return tag.getCompoundTag(TAG);
    }

    private static String idOrThrow(DataComponentType<?> type) {
        var id = type.id();
        if (id == null) {
            throw new IllegalStateException("Data component type " + type + " isn't registered");
        }
        return id.toString();
    }
}
