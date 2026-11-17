/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package ladder.adapter.component;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import ladder.carried.net.minecraft.network.codec.StreamCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

/**
 * 1.20.5's {@code DataComponents}, the game's own components, on 1.20.4's data of the stack: each reads and writes what
 * 1.20.4 keeps for it (the custom name in its display tag, the damage, the stack's own NBT), so 1.20.4's code and a
 * converted mod see one value. Only these are here yet; a mod using another one is a conversion problem (it isn't in
 * this class), not a silent loss. Values 1.20.4 can't keep per stack (another maximum damage or stack size than the
 * item's) fail when set.
 */
public final class DataComponents {
    /** where custom data keys that are also the game's own keys are kept (they'd overwrite the stack's state) */
    static final String CUSTOM_KEPT = "ladder:custom_data";

    /** the keys of a stack's tag that are the game's own data in 1.20.4 (not a mod's custom data) */
    static final Set<String> GAME_KEYS = Set.of("display", "Damage", "Unbreakable", "Enchantments", "StoredEnchantments", "AttributeModifiers",
            "HideFlags", "RepairCost", "CanDestroy", "CanPlaceOn", "CustomModelData", "BlockEntityTag", "BlockStateTag", "EntityTag", "Potion",
            "CustomPotionEffects", "CustomPotionColor", "Trim", "SkullOwner", "Fireworks", "Explosion", "pages", "author", "title", "generation",
            "resolved", "Decorations", "map", "map_scale_direction", "LodestoneDimension", "LodestonePos", "LodestoneTracked", "Items", "Charged",
            "ChargedProjectiles", "Recipes", "DebugProperty", "BucketVariantTag", "instrument", "ladder:components", CUSTOM_KEPT);

    public static final DataComponentType<CustomData> CUSTOM_DATA = new Game<>("custom_data", CustomData.CODEC, new DataComponentType.Vanilla<>() {
        @Override
        public CustomData get(ItemStack stack) {
            CompoundTag root = stack.getTag();
            if (root == null) {
                return null;
            }
            CompoundTag own = new CompoundTag();
            for (String key : root.getAllKeys()) {
                if (!GAME_KEYS.contains(key)) {
                    own.put(key, root.get(key).copy());
                }
            }
            CompoundTag kept = root.getCompound(CUSTOM_KEPT);
            for (String key : kept.getAllKeys()) {
                own.put(key, kept.get(key).copy());
            }
            return own.isEmpty() ? null : CustomData.of(own);
        }

        @Override
        public CustomData prototype(ItemStack stack) {
            return null;
        }

        @Override
        public void set(ItemStack stack, CustomData value) {
            CompoundTag root = stack.getOrCreateTag();
            for (String key : List.copyOf(root.getAllKeys())) {
                if (!GAME_KEYS.contains(key)) {
                    root.remove(key);
                }
            }
            root.remove(CUSTOM_KEPT);
            if (value != null) {
                CompoundTag own = value.unsafe();
                CompoundTag kept = new CompoundTag();
                for (String key : own.getAllKeys()) {
                    // a key the game uses for its own data would change the stack (its damage, its name): kept apart
                    (GAME_KEYS.contains(key) ? kept : root).put(key, own.get(key).copy());
                }
                if (!kept.isEmpty()) {
                    root.put(CUSTOM_KEPT, kept);
                }
            }
            if (root.isEmpty()) {
                stack.setTag(null);
            }
        }
    });

    public static final DataComponentType<Component> CUSTOM_NAME = new Game<>("custom_name", ComponentSerialization.FLAT_CODEC, new DataComponentType.Vanilla<>() {
        // the stack's display name as 1.20.4 keeps it (display.Name, JSON): read and written as data, never through
        // getHoverName (a mod's mixin or override there may read this component: it would come back here)
        @Override
        public Component get(ItemStack stack) {
            CompoundTag display = stack.getTagElement("display");
            if (display == null || !display.contains("Name", net.minecraft.nbt.Tag.TAG_STRING)) {
                return null;
            }
            try {
                return Component.Serializer.fromJson(display.getString("Name"));
            } catch (RuntimeException e) {
                return null;
            }
        }

        @Override
        public Component prototype(ItemStack stack) {
            return null;
        }

        @Override
        public void set(ItemStack stack, Component value) {
            if (value == null) {
                CompoundTag display = stack.getTagElement("display");
                if (display != null) {
                    display.remove("Name");
                    if (display.isEmpty()) {
                        stack.removeTagKey("display");
                    }
                }
                if (stack.getTag() != null && stack.getTag().isEmpty()) {
                    stack.setTag(null);
                }
            } else {
                stack.getOrCreateTagElement("display").putString("Name", Component.Serializer.toJson(value));
            }
        }
    });

    public static final DataComponentType<Integer> DAMAGE = new Game<>("damage", ExtraCodecs.NON_NEGATIVE_INT, new DataComponentType.Vanilla<>() {
        // the Damage tag as 1.20.4 keeps it, read and written as data (an item's getDamage override may read this)
        @Override
        public Integer get(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("Damage", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
                return Math.max(0, tag.getInt("Damage"));
            }
            return this.prototype(stack);
        }

        @Override
        public Integer prototype(ItemStack stack) {
            return stack.getItem().canBeDepleted() ? 0 : null;
        }

        @Override
        public void set(ItemStack stack, Integer value) {
            if (value == null) {
                if (stack.getItem().canBeDepleted()) {
                    throw new IllegalStateException("1.20.4 can't take the damage away from a damageable stack");
                }
                stack.removeTagKey("Damage");
                if (stack.getTag() != null && stack.getTag().isEmpty()) {
                    stack.setTag(null);
                }
            } else {
                stack.getOrCreateTag().putInt("Damage", Math.max(0, value));
            }
        }
    });

    /** 1.20.5's item name: in 1.20.4 the item's own name (its description), kept on the item only */
    public static final DataComponentType<Component> ITEM_NAME = new Game<>("item_name", ComponentSerialization.FLAT_CODEC, new DataComponentType.Vanilla<>() {
        @Override
        public Component get(ItemStack stack) {
            return this.prototype(stack);
        }

        @Override
        public Component prototype(ItemStack stack) {
            return Component.translatable(stack.getItem().getDescriptionId(stack));
        }

        @Override
        public void set(ItemStack stack, Component value) {
            if (!java.util.Objects.equals(value, this.prototype(stack))) {
                throw new IllegalStateException("1.20.4 keeps the item name on the item, not on the stack: can't set " + value);
            }
        }
    });

    public static final DataComponentType<Integer> MAX_DAMAGE = new Game<>("max_damage", ExtraCodecs.POSITIVE_INT, itemOnly("max_damage", true));

    public static final DataComponentType<Integer> MAX_STACK_SIZE = new Game<>("max_stack_size", ExtraCodecs.intRange(1, 99), itemOnly("max_stack_size", false));

    /** the game's components here, registered under minecraft: (ComponentRegistry) */
    public static final List<DataComponentType<?>> ALL = List.of(CUSTOM_DATA, CUSTOM_NAME, ITEM_NAME, DAMAGE, MAX_DAMAGE, MAX_STACK_SIZE);

    private DataComponents() {
    }

    /** a value 1.20.4 keeps on the item only: read from it; setting another one on a stack fails */
    private static DataComponentType.Vanilla<Integer> itemOnly(String name, boolean damage) {
        return new DataComponentType.Vanilla<>() {
            @Override
            public Integer get(ItemStack stack) {
                return this.prototype(stack);
            }

            @Override
            public Integer prototype(ItemStack stack) {
                if (damage) {
                    return stack.getItem().canBeDepleted() ? stack.getMaxDamage() : null;
                }
                return stack.getMaxStackSize();
            }

            @Override
            public void set(ItemStack stack, Integer value) {
                if (!java.util.Objects.equals(value, this.prototype(stack))) {
                    throw new IllegalStateException("1.20.4 keeps " + name + " on the item, not on the stack: can't set " + value);
                }
            }
        };
    }

    /** one of the game's components */
    static final class Game<T> implements DataComponentType<T> {
        final String name;
        private final Codec<T> codec;
        private final Vanilla<T> vanilla;

        Game(String name, Codec<T> codec, Vanilla<T> vanilla) {
            this.name = name;
            this.codec = codec;
            this.vanilla = vanilla;
        }

        @Override
        public Codec<T> codec() {
            return this.codec;
        }

        @Override
        @Nullable
        public StreamCodec<?, T> streamCodec() {
            // 1.20.4 sends the stack's tag, which holds the value
            return null;
        }

        @Override
        public Vanilla<T> vanilla() {
            return this.vanilla;
        }

        @Override
        public String toString() {
            return "minecraft:" + this.name;
        }
    }
}
