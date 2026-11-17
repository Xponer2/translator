/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import com.mojang.serialization.DynamicOps;

import ladder.adapter.bridge.Conversions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

/**
 * A 1.20.4 stack's components, as 1.20.5's {@code PatchedDataComponentMap} has them: the item's own values (its
 * prototype: ItemDefaults) and the stack's patch over them, which is kept in the stack's NBT (the one place its data
 * lives: copies, comparisons, saving and the network carry it as 1.20.4 carries any tag).
 *
 * <p>The patch, under {@value #KEY}: a component's id with its value (encoded by the type's codec), or {@code "!id"}
 * when the stack has the item's value removed. Setting the item's own value removes the stack's entry (as 1.20.5 does);
 * removing a value the item has leaves the marker. The game's own components read and write the stack's 1.20.4 data
 * (DataComponents). Nothing is cached: each read decodes what is in the tag now.</p>
 */
public final class ComponentStore {
    public static final String KEY = "ladder:components";

    private ComponentStore() {
    }

    /** the custom name an item's properties give its stacks, when the stack has none of its own and hasn't it removed */
    @Nullable
    public static net.minecraft.network.chat.Component defaultName(ItemStack stack) {
        Object own = ItemDefaults.of(stack.getItem()).get(DataComponents.CUSTOM_NAME);
        if (own == null || DataComponents.CUSTOM_NAME.vanilla().get(stack) != null || removed(stack, DataComponents.CUSTOM_NAME)) {
            return null;
        }
        return (net.minecraft.network.chat.Component) own;
    }

    private static ItemStack stack(Object holder) {
        if (holder instanceof ItemStack stack) {
            return stack;
        }
        throw new IllegalStateException("components of " + holder.getClass().getName() + " (only item stacks have them on 1.20.4 yet)");
    }

    private static DynamicOps<Tag> ops() {
        return RegistryOps.create(NbtOps.INSTANCE, Conversions.registries());
    }

    private static String id(DataComponentType<?> type) {
        if (ComponentRegistry.registry().getKey(type) == null) {
            throw new IllegalStateException("a component type that isn't registered: " + type);
        }
        return ComponentRegistry.name(type);
    }

    private static void persistent(DataComponentType<?> type) {
        if (type.isTransient()) {
            throw new IllegalStateException("transient component " + type + ": 1.20.4 stacks keep only what their tag holds (not yet supported)");
        }
    }

    @Nullable
    private static <T> T decode(DataComponentType<T> type, Tag tag) {
        return type.codecOrThrow().parse(ops(), tag).resultOrPartial(e -> {
            throw new IllegalStateException("component " + type + " doesn't decode: " + e);
        }).orElse(null);
    }

    private static <T> Tag encode(DataComponentType<T> type, T value) {
        return type.codecOrThrow().encodeStart(ops(), value).resultOrPartial(e -> {
            throw new IllegalStateException("component " + type + " doesn't encode: " + e);
        }).orElseThrow();
    }

    /** the item's own value: its properties' (Item.Properties.component), else the game's for the item */
    @Nullable
    @SuppressWarnings("unchecked")
    static <T> T prototype(ItemStack stack, DataComponentType<T> type) {
        T own = (T) ItemDefaults.of(stack.getItem()).get(type);
        if (own != null) {
            return own;
        }
        DataComponentType.Vanilla<T> vanilla = type.vanilla();
        return vanilla != null ? vanilla.prototype(stack) : null;
    }

    /** whether the stack has the item's value of a component removed (the marker in its patch) */
    static boolean removed(ItemStack stack, DataComponentType<?> type) {
        CompoundTag root = stack.getTag();
        return root != null && root.getCompound(KEY).contains("!" + id(type));
    }

    // ------------------------------------------------------------------ reading

    /** 1.20.5's {@code get(type)} */
    @Nullable
    @SuppressWarnings("unchecked")
    public static Object get(DataComponentHolder holder, DataComponentType<?> type) {
        return read(stack(holder), (DataComponentType<Object>) type);
    }

    @Nullable
    static <T> T read(ItemStack stack, DataComponentType<T> type) {
        DataComponentType.Vanilla<T> vanilla = type.vanilla();
        if (vanilla != null) {
            T value = vanilla.get(stack);
            if (value != null && !value.equals(vanilla.prototype(stack))) {
                return value;
            }
            // nothing of the stack's own: the item's value, unless the stack has it removed
            return removed(stack, type) ? null : value != null ? value : prototype(stack, type);
        }
        CompoundTag patch = stack.getTag() == null ? null : stack.getTag().getCompound(KEY);
        if (patch != null && !patch.isEmpty()) {
            String id = id(type);
            if (patch.contains("!" + id)) {
                return null;
            }
            if (patch.contains(id)) {
                persistent(type);
                return decode(type, patch.get(id));
            }
        }
        return prototype(stack, type);
    }

    /** 1.20.5's {@code getOrDefault(type, fallback)} */
    public static Object getOrDefault(DataComponentHolder holder, DataComponentType<?> type, Object fallback) {
        Object value = get(holder, type);
        return value != null ? value : fallback;
    }

    /** 1.20.5's {@code has(type)} */
    public static boolean has(DataComponentHolder holder, DataComponentType<?> type) {
        return get(holder, type) != null;
    }

    /** 1.20.5's {@code getComponents()}: the stack's components now (its item's, its patch's, the game's own) */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DataComponentMap getComponents(ItemStack stack) {
        Map<DataComponentType<?>, Object> values = new LinkedHashMap<>();
        for (DataComponentType vanilla : DataComponents.ALL) {
            Object value = read(stack, vanilla);
            if (value != null) {
                values.put(vanilla, value);
            }
        }
        for (DataComponentType<?> type : ItemDefaults.of(stack.getItem()).keySet()) {
            Object value = read(stack, (DataComponentType<Object>) type);
            if (value != null) {
                values.put(type, value);
            }
        }
        CompoundTag patch = stack.getTag() == null ? null : stack.getTag().getCompound(KEY);
        if (patch != null) {
            for (String key : patch.getAllKeys()) {
                if (!key.startsWith("!")) {
                    DataComponentType<?> type = ComponentRegistry.registry().get(new net.minecraft.resources.ResourceLocation(key));
                    if (type != null && !values.containsKey(type)) {
                        values.put(type, read(stack, (DataComponentType<Object>) type));
                    }
                }
            }
        }
        return new DataComponentMap.Snapshot(values);
    }

    // ------------------------------------------------------------------ writing

    /** 1.20.5's {@code set(type, value)}: the value before */
    @Nullable
    @SuppressWarnings("unchecked")
    public static Object set(ItemStack stack, DataComponentType<?> type, @Nullable Object value) {
        DataComponentType<Object> t = (DataComponentType<Object>) type;
        if (value == null) {
            return remove(stack, type);
        }
        Object before = read(stack, t);
        DataComponentType.Vanilla<Object> vanilla = t.vanilla();
        if (vanilla != null) {
            clearMarker(stack, t);
            vanilla.set(stack, value);
            return before;
        }
        persistent(t);
        String id = id(t);
        CompoundTag patch = patchForWrite(stack);
        patch.remove("!" + id);
        if (Objects.equals(prototype(stack, t), value)) {
            // the item's own value: no entry (as 1.20.5's patched map has it)
            patch.remove(id);
        } else {
            patch.put(id, encode(t, value));
        }
        tidy(stack);
        return before;
    }

    /** 1.20.5's {@code remove(type)}: the value before */
    @Nullable
    @SuppressWarnings("unchecked")
    public static Object remove(ItemStack stack, DataComponentType<?> type) {
        DataComponentType<Object> t = (DataComponentType<Object>) type;
        Object before = read(stack, t);
        DataComponentType.Vanilla<Object> vanilla = t.vanilla();
        if (vanilla != null) {
            vanilla.set(stack, null);
            if (ItemDefaults.of(stack.getItem()).get(t) != null) {
                // the item's own value (its properties'): the stack keeps it removed
                patchForWrite(stack).put("!" + id(t), new CompoundTag());
                tidy(stack);
            }
            return before;
        }
        String id = id(t);
        CompoundTag patch = patchForWrite(stack);
        patch.remove(id);
        if (prototype(stack, t) != null) {
            // the item has a value: the stack has it removed
            patch.put("!" + id, new CompoundTag());
        } else {
            patch.remove("!" + id);
        }
        tidy(stack);
        return before;
    }

    /** 1.20.5's {@code update(type, fallback, updater)}: the value before */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object update(ItemStack stack, DataComponentType<?> type, Object fallback, UnaryOperator updater) {
        Object current = read(stack, (DataComponentType<Object>) type);
        return set(stack, type, updater.apply(current != null ? current : fallback));
    }

    /** 1.20.5's {@code update(type, fallback, argument, updater)} */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object update(ItemStack stack, DataComponentType<?> type, Object fallback, Object argument, BiFunction updater) {
        Object current = read(stack, (DataComponentType<Object>) type);
        return set(stack, type, updater.apply(current != null ? current : fallback, argument));
    }

    // ------------------------------------------------------------------ a stack's NBT (1.20.5's codec entries)

    /** 1.20.5's {@code ItemStack.save(registries)}: 1.20.4's NBT of the stack (the components are in its tag) */
    public static Tag save(ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty ItemStack");
        }
        return stack.save(new CompoundTag());
    }

    /** 1.20.5's {@code ItemStack.saveOptional(registries)}: an empty tag for an empty stack */
    public static Tag saveOptional(ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        return stack.isEmpty() ? new CompoundTag() : stack.save(new CompoundTag());
    }

    /** 1.20.5's {@code ItemStack.parseOptional(registries, tag)} */
    public static ItemStack parseOptional(net.minecraft.core.HolderLookup.Provider registries, CompoundTag tag) {
        return tag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(tag);
    }

    /** 1.20.5's {@code ItemStack.parse(registries, tag)} */
    public static java.util.Optional<ItemStack> parse(net.minecraft.core.HolderLookup.Provider registries, Tag tag) {
        if (!(tag instanceof CompoundTag compound)) {
            return java.util.Optional.empty();
        }
        ItemStack stack = ItemStack.of(compound);
        return stack.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(stack);
    }

    private static void clearMarker(ItemStack stack, DataComponentType<?> type) {
        CompoundTag root = stack.getTag();
        if (root != null && root.contains(KEY)) {
            root.getCompound(KEY).remove("!" + id(type));
            tidy(stack);
        }
    }

    private static CompoundTag patchForWrite(ItemStack stack) {
        CompoundTag root = stack.getOrCreateTag();
        if (!root.contains(KEY, Tag.TAG_COMPOUND)) {
            root.put(KEY, new CompoundTag());
        }
        return root.getCompound(KEY);
    }

    /** no empty patch, no empty tag left behind (an untouched stack stays equal to a new one) */
    private static void tidy(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) {
            return;
        }
        if (root.getCompound(KEY).isEmpty()) {
            root.remove(KEY);
        }
        if (root.isEmpty()) {
            stack.setTag(null);
        }
    }
}
