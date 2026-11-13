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
package io.github.xponer2.translator.component;

import io.github.xponer2.translator.network.ByteBufCodecs;
import io.github.xponer2.translator.registry.MappedRegistry;
import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.Codecs;
import io.github.xponer2.translator.util.Registries;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The vanilla component types the mods use (1.21 {@code net.minecraft.core.component.DataComponents}), registered
 * under their 1.21 ids. They keep their values where 1.12.2 keeps that data, so vanilla code sees them:
 * {@link #CUSTOM_DATA} is the stack's NBT (without {@link StackComponents#TAG}), {@link #UNBREAKABLE} the
 * "Unbreakable" tag. As defaults of an item ({@code ItemProperties#component}) they only exist for
 * {@link StackComponents}; the item itself has to act on them.
 */
public final class DataComponents {
    static final List<DataComponentType<?>> WITH_STORAGE = new ArrayList<>();

    public static final DataComponentType<NBTTagCompound> CUSTOM_DATA = register("custom_data", DataComponentType.<NBTTagCompound>builder()
            .persistent(Codecs.COMPOUND_TAG).networkSynchronized(ByteBufCodecs.COMPOUND_TAG).storage(new CustomDataStorage()).build());
    public static final DataComponentType<Unbreakable> UNBREAKABLE = register("unbreakable", DataComponentType.<Unbreakable>builder()
            .persistent(Unbreakable.CODEC).networkSynchronized(Unbreakable.STREAM_CODEC).storage(new UnbreakableStorage()).build());

    /** The stack's display name (1.12.2 "display"/"Name"). */
    public static final DataComponentType<net.minecraft.util.text.ITextComponent> CUSTOM_NAME = register("custom_name",
            DataComponentType.<net.minecraft.util.text.ITextComponent>builder().storage(new StackStorage<>() {
                @Nullable
                @Override
                public net.minecraft.util.text.ITextComponent get(ItemStack stack) {
                    return stack.hasDisplayName() ? new net.minecraft.util.text.TextComponentString(stack.getDisplayName()) : null;
                }

                @Override
                public void set(ItemStack stack, @Nullable net.minecraft.util.text.ITextComponent value) {
                    if (value == null) {
                        stack.clearCustomName();
                    } else {
                        stack.setStackDisplayName(value.getFormattedText());
                    }
                }
            }).build());
    /** The stack's damage (1.12.2 item damage). */
    public static final DataComponentType<Integer> DAMAGE = register("damage", DataComponentType.<Integer>builder().storage(new StackStorage<>() {
        @Nullable
        @Override
        public Integer get(ItemStack stack) {
            return stack.isItemStackDamageable() ? stack.getItemDamage() : null;
        }

        @Override
        public void set(ItemStack stack, @Nullable Integer value) {
            stack.setItemDamage(value == null ? 0 : value);
        }
    }).build());
    /** The data of the block entity a placed block item gets (1.12.2 "BlockEntityTag"). */
    public static final DataComponentType<NBTTagCompound> BLOCK_ENTITY_DATA = register("block_entity_data",
            DataComponentType.<NBTTagCompound>builder().storage(new StackStorage<>() {
                @Nullable
                @Override
                public NBTTagCompound get(ItemStack stack) {
                    return stack.getSubCompound("BlockEntityTag");
                }

                @Override
                public void set(ItemStack stack, @Nullable NBTTagCompound value) {
                    if (value == null) {
                        stack.removeSubCompound("BlockEntityTag");
                    } else {
                        stack.setTagInfo("BlockEntityTag", value.copy());
                    }
                }
            }).build());
    public static final DataComponentType<Integer> MAX_STACK_SIZE = plain("max_stack_size");
    public static final DataComponentType<Integer> MAX_DAMAGE = plain("max_damage");
    public static final DataComponentType<Integer> REPAIR_COST = plain("repair_cost");
    /**
     * Persistent with the carried ItemContainerContents codec (read lazily: the 1.21.1 classes are carried after this
     * class loads): Twilight Forest's travellers modifiers (swap_hotbar) name it in their data, and an item patch with a
     * component that isn't persistent doesn't read.
     */
    public static final DataComponentType<Object> CONTAINER = register("container",
            DataComponentType.<Object>builder().persistent(new CarriedCodec<>("net.minecraft.world.item.component.ItemContainerContents")).build());
    /** The potion of a stack (1.12.2's "Potion", "CustomPotionColor" and "CustomPotionEffects" tags). */
    public static final DataComponentType<PotionContents> POTION_CONTENTS = register("potion_contents",
            DataComponentType.<PotionContents>builder().storage(new StackStorage<>() {
                @Nullable
                @Override
                public PotionContents get(ItemStack stack) {
                    return PotionContents.read(stack);
                }

                @Override
                public void set(ItemStack stack, @Nullable PotionContents value) {
                    PotionContents.write(stack, value);
                }
            }).build());
    public static final DataComponentType<Object> ENCHANTMENTS = plain("enchantments");
    public static final DataComponentType<Object> STORED_ENCHANTMENTS = plain("stored_enchantments");
    public static final DataComponentType<Object> FOOD = plain("food");
    public static final DataComponentType<Object> ATTRIBUTE_MODIFIERS = plain("attribute_modifiers");
    public static final DataComponentType<Object> JUKEBOX_PLAYABLE = plain("jukebox_playable");
    public static final DataComponentType<Object> WRITTEN_BOOK_CONTENT = plain("written_book_content");
    public static final DataComponentType<Object> WRITABLE_BOOK_CONTENT = plain("writable_book_content");
    public static final DataComponentType<Object> DYED_COLOR = plain("dyed_color");
    public static final DataComponentType<Object> CONTAINER_LOOT = plain("container_loot");
    public static final DataComponentType<Object> TRIM = plain("trim");
    public static final DataComponentType<Object> MAP_ID = plain("map_id");
    public static final DataComponentType<Object> PROFILE = plain("profile");
    public static final DataComponentType<Object> FIRE_RESISTANT = plain("fire_resistant");
    public static final DataComponentType<Object> BLOCK_STATE = plain("block_state");
    public static final DataComponentType<Object> HIDE_TOOLTIP = plain("hide_tooltip");
    public static final DataComponentType<Object> HIDE_ADDITIONAL_TOOLTIP = plain("hide_additional_tooltip");
    public static final DataComponentType<Object> SUSPICIOUS_STEW_EFFECTS = plain("suspicious_stew_effects");
    public static final DataComponentType<Object> FIREWORKS = plain("fireworks");
    public static final DataComponentType<Object> LODESTONE_TRACKER = plain("lodestone_tracker");
    public static final DataComponentType<Object> LOCK = plain("lock");
    public static final DataComponentType<Object> NOTE_BLOCK_SOUND = plain("note_block_sound");
    public static final DataComponentType<Object> LORE = plain("lore");
    public static final DataComponentType<Object> INTANGIBLE_PROJECTILE = plain("intangible_projectile");
    public static final DataComponentType<Object> ENCHANTMENT_GLINT_OVERRIDE = plain("enchantment_glint_override");
    public static final DataComponentType<Object> RARITY = plain("rarity");
    public static final DataComponentType<Object> TOOL = plain("tool");
    public static final DataComponentType<Object> BUNDLE_CONTENTS = plain("bundle_contents");
    public static final DataComponentType<Object> CHARGED_PROJECTILES = plain("charged_projectiles");
    public static final DataComponentType<Object> ENTITY_DATA = plain("entity_data");
    public static final DataComponentType<Object> BUCKET_ENTITY_DATA = plain("bucket_entity_data");
    public static final DataComponentType<Object> CUSTOM_MODEL_DATA = plain("custom_model_data");
    public static final DataComponentType<Object> ITEM_NAME = plain("item_name");
    // the other 1.21.1 components (a mod reading one got null: Supplementaries' and Twilight Forest's banners, shields, fireworks)
    public static final DataComponentType<Object> CAN_PLACE_ON = plain("can_place_on");
    public static final DataComponentType<Object> CAN_BREAK = plain("can_break");
    public static final DataComponentType<Object> CREATIVE_SLOT_LOCK = plain("creative_slot_lock");
    public static final DataComponentType<Object> MAP_COLOR = plain("map_color");
    public static final DataComponentType<Object> MAP_DECORATIONS = plain("map_decorations");
    public static final DataComponentType<Object> MAP_POST_PROCESSING = plain("map_post_processing");
    public static final DataComponentType<Object> DEBUG_STICK_STATE = plain("debug_stick_state");
    public static final DataComponentType<Object> INSTRUMENT = plain("instrument");
    public static final DataComponentType<Object> OMINOUS_BOTTLE_AMPLIFIER = plain("ominous_bottle_amplifier");
    public static final DataComponentType<Object> RECIPES = plain("recipes");
    public static final DataComponentType<Object> FIREWORK_EXPLOSION = plain("firework_explosion");
    public static final DataComponentType<Object> BANNER_PATTERNS = plain("banner_patterns");
    public static final DataComponentType<Object> BASE_COLOR = plain("base_color");
    public static final DataComponentType<Object> POT_DECORATIONS = plain("pot_decorations");
    public static final DataComponentType<Object> BEES = plain("bees");

    private DataComponents() { }

    /**
     * A vanilla type 1.12.2 has no place for: its values stay with the stack in memory (no codec, see StackComponents),
     * the item's default otherwise.
     */
    private static <T> DataComponentType<T> plain(String name) {
        return register(name, DataComponentType.<T>builder().build());
    }

    private static <T> DataComponentType<T> register(String name, DataComponentType<T> type) {
        var registry = BuiltInRegistries.byKey(Registries.DATA_COMPONENT_TYPE);
        if (registry instanceof MappedRegistry) {
            ((MappedRegistry<DataComponentType<?>>) registry).register(new ResourceLocation("minecraft", name), type);
        }
        if (type.storage() != null) {
            WITH_STORAGE.add(type);
        }
        return type;
    }

    static List<DataComponentType<?>> withStorage() {
        return Collections.unmodifiableList(WITH_STORAGE);
    }

    /**
     * The NBT of the stack, without the components tag.
     */
    private static final class CustomDataStorage implements StackStorage<NBTTagCompound> {
        @Nullable
        @Override
        public NBTTagCompound get(ItemStack stack) {
            var tag = stack.getTagCompound();
            if (tag == null) {
                return null;
            }
            var copy = tag.copy();
            copy.removeTag(StackComponents.TAG);
            return copy.isEmpty() ? null : copy;
        }

        @Override
        public void set(ItemStack stack, @Nullable NBTTagCompound value) {
            var old = stack.getTagCompound();
            var tag = value == null ? new NBTTagCompound() : value.copy();
            if (old != null && old.hasKey(StackComponents.TAG, Constants.NBT.TAG_COMPOUND)) {
                tag.setTag(StackComponents.TAG, old.getTag(StackComponents.TAG));
            }
            stack.setTagCompound(tag.isEmpty() ? null : tag);
        }
    }

    /**
     * The vanilla "Unbreakable" tag; hidden in the tooltip by bit 4 of "HideFlags".
     */
    private static final class UnbreakableStorage implements StackStorage<Unbreakable> {
        private static final String UNBREAKABLE = "Unbreakable";
        private static final String HIDE_FLAGS = "HideFlags";
        private static final int HIDE_UNBREAKABLE = 4;

        @Nullable
        @Override
        public Unbreakable get(ItemStack stack) {
            var tag = stack.getTagCompound();
            if (tag == null || !tag.getBoolean(UNBREAKABLE)) {
                return null;
            }
            return new Unbreakable((tag.getInteger(HIDE_FLAGS) & HIDE_UNBREAKABLE) == 0);
        }

        @Override
        public void set(ItemStack stack, @Nullable Unbreakable value) {
            var tag = stack.getTagCompound();
            if (value == null) {
                if (tag != null) {
                    tag.removeTag(UNBREAKABLE);
                    if (tag.isEmpty()) {
                        stack.setTagCompound(null);
                    }
                }
                return;
            }
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }
            tag.setBoolean(UNBREAKABLE, true);
            int flags = tag.getInteger(HIDE_FLAGS);
            flags = value.showInTooltip() ? flags & ~HIDE_UNBREAKABLE : flags | HIDE_UNBREAKABLE;
            if (flags == 0) {
                tag.removeTag(HIDE_FLAGS);
            } else {
                tag.setInteger(HIDE_FLAGS, flags);
            }
        }
    }
}
