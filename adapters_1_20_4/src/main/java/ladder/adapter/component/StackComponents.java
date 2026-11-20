/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Multimap;

import ladder.carried.net.minecraft.world.entity.EquipmentSlotGroup;
import ladder.carried.net.minecraft.world.item.alchemy.PotionContents;
import ladder.carried.net.minecraft.world.item.component.CustomModelData;
import ladder.carried.net.minecraft.world.item.component.ItemAttributeModifiers;
import ladder.carried.net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;

/**
 * More of the game's components on 1.20.4's data of the stack (DataComponents), each where 1.20.4 keeps it: potions
 * in Potion / CustomPotionColor / CustomPotionEffects, lore in display.Lore, the model number in CustomModelData,
 * attribute modifiers in AttributeModifiers (the item's own when the stack has none), food on the item.
 */
final class StackComponents {
    /** 1.20.4's HideFlags bit of the attribute modifiers' tooltip */
    static final int HIDE_MODIFIERS = 2;

    private StackComponents() {
    }

    /** 1.21's potion_contents; the potion items and tipped arrows have an empty one by default. */
    static final class Potions implements DataComponentType.Vanilla<PotionContents> {
        @Override
        public PotionContents get(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains("Potion", Tag.TAG_STRING) && !tag.contains("CustomPotionColor", Tag.TAG_ANY_NUMERIC)
                    && !tag.contains("CustomPotionEffects", Tag.TAG_LIST)) {
                return null;
            }
            Optional<Holder<Potion>> potion = Optional.empty();
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("Potion"));
            if (id != null) {
                // 1.20.4's empty potion is 1.21's "no potion"
                potion = BuiltInRegistries.POTION.getOptional(id).filter(p -> p != net.minecraft.world.item.alchemy.Potions.EMPTY)
                        .map(BuiltInRegistries.POTION::wrapAsHolder);
            }
            Optional<Integer> color = tag.contains("CustomPotionColor", Tag.TAG_ANY_NUMERIC) ? Optional.of(tag.getInt("CustomPotionColor")) : Optional.empty();
            List<MobEffectInstance> effects = new ArrayList<>();
            ListTag list = tag.getList("CustomPotionEffects", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                MobEffectInstance effect = ladder.adapter.effect.EffectAdapters.load(list.getCompound(i));
                if (effect != null) {
                    effects.add(effect);
                }
            }
            return new PotionContents(potion, color, List.copyOf(effects));
        }

        @Override
        public PotionContents prototype(ItemStack stack) {
            return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION) || stack.is(Items.TIPPED_ARROW)
                    ? PotionContents.EMPTY : null;
        }

        @Override
        public void set(ItemStack stack, PotionContents value) {
            stack.removeTagKey("Potion");
            stack.removeTagKey("CustomPotionColor");
            stack.removeTagKey("CustomPotionEffects");
            if (value != null) {
                value.potion().ifPresent(p -> stack.getOrCreateTag().putString("Potion", BuiltInRegistries.POTION.getKey(p.value()).toString()));
                value.customColor().ifPresent(c -> stack.getOrCreateTag().putInt("CustomPotionColor", c));
                if (!value.customEffects().isEmpty()) {
                    ListTag list = new ListTag();
                    for (MobEffectInstance effect : value.customEffects()) {
                        list.add(effect.save(new CompoundTag()));
                    }
                    stack.getOrCreateTag().put("CustomPotionEffects", list);
                }
            }
            DataComponents.dropEmptyTag(stack);
        }
    }

    /** 1.21's lore: 1.20.4's display.Lore, one JSON text per line; every item has an empty one by default. */
    static final class Lore implements DataComponentType.Vanilla<ItemLore> {
        @Override
        public ItemLore get(ItemStack stack) {
            CompoundTag display = stack.getTagElement("display");
            if (display == null || !display.contains("Lore", Tag.TAG_LIST)) {
                return null;
            }
            ListTag list = display.getList("Lore", Tag.TAG_STRING);
            List<Component> lines = new ArrayList<>();
            for (int i = 0; i < list.size() && lines.size() < ItemLore.MAX_LINES; i++) {
                try {
                    Component line = Component.Serializer.fromJson(list.getString(i));
                    if (line != null) {
                        lines.add(line);
                    }
                } catch (RuntimeException e) {
                    // 1.20.4's tooltip leaves a line it can't read out too
                }
            }
            return new ItemLore(List.copyOf(lines));
        }

        @Override
        public ItemLore prototype(ItemStack stack) {
            return ItemLore.EMPTY;
        }

        @Override
        public void set(ItemStack stack, ItemLore value) {
            if (value == null || value.lines().isEmpty()) {
                CompoundTag display = stack.getTagElement("display");
                if (display != null) {
                    display.remove("Lore");
                    if (display.isEmpty()) {
                        stack.removeTagKey("display");
                    }
                }
            } else {
                ListTag list = new ListTag();
                for (Component line : value.lines()) {
                    list.add(StringTag.valueOf(Component.Serializer.toJson(line)));
                }
                stack.getOrCreateTagElement("display").put("Lore", list);
            }
            DataComponents.dropEmptyTag(stack);
        }
    }

    /** 1.21's custom_model_data: 1.20.4's CustomModelData number. */
    static final class ModelData implements DataComponentType.Vanilla<CustomModelData> {
        @Override
        public CustomModelData get(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            return tag != null && tag.contains("CustomModelData", Tag.TAG_ANY_NUMERIC) ? new CustomModelData(tag.getInt("CustomModelData")) : null;
        }

        @Override
        public CustomModelData prototype(ItemStack stack) {
            return null;
        }

        @Override
        public void set(ItemStack stack, CustomModelData value) {
            if (value == null) {
                stack.removeTagKey("CustomModelData");
                DataComponents.dropEmptyTag(stack);
            } else {
                stack.getOrCreateTag().putInt("CustomModelData", value.value());
            }
        }
    }

    /** 1.21's food: 1.20.4 keeps it on the item (its getFoodProperties); a stack can't have another one. */
    static final class Food implements DataComponentType.Vanilla<FoodProperties> {
        @Override
        public FoodProperties get(ItemStack stack) {
            return this.prototype(stack);
        }

        @Override
        public FoodProperties prototype(ItemStack stack) {
            // the item's own food, not the stack hook (a mod's getFoodProperties may read this component)
            return stack.getItem().getFoodProperties();
        }

        @Override
        public void set(ItemStack stack, FoodProperties value) {
            if (value != this.prototype(stack)) {
                throw new IllegalStateException("1.20.4 keeps the food on the item, not on the stack: can't set " + value);
            }
        }
    }

    /**
     * 1.21's attribute_modifiers: 1.20.4's AttributeModifiers list of the stack ({AttributeName, Slot, the modifier's
     * own keys}), which replaces the item's own modifiers as the component does; shown unless HideFlags has bit 2. A
     * group of several slots is one entry per slot below (no Slot for any slot); the body slot is the chest.
     */
    static final class Attributes implements DataComponentType.Vanilla<ItemAttributeModifiers> {
        private static final Map<EquipmentSlotGroup, List<String>> SLOTS = Map.of(
                EquipmentSlotGroup.HAND, List.of("mainhand", "offhand"),
                EquipmentSlotGroup.ARMOR, List.of("feet", "legs", "chest", "head"),
                EquipmentSlotGroup.BODY, List.of("chest"));

        @Override
        public ItemAttributeModifiers get(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains("AttributeModifiers", Tag.TAG_LIST)) {
                return null;
            }
            List<ItemAttributeModifiers.Entry> entries = new ArrayList<>();
            ListTag list = tag.getList("AttributeModifiers", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(entry.getString("AttributeName"));
                Optional<Attribute> attribute = id == null ? Optional.empty() : BuiltInRegistries.ATTRIBUTE.getOptional(id);
                AttributeModifier modifier = AttributeModifier.load(entry);
                // as 1.20.4's ItemStack.getAttributeModifiers: an unknown attribute or a modifier without its UUID is skipped
                if (attribute.isEmpty() || modifier == null || modifier.getId().getLeastSignificantBits() == 0L
                        || modifier.getId().getMostSignificantBits() == 0L) {
                    continue;
                }
                EquipmentSlotGroup group = entry.contains("Slot", Tag.TAG_STRING) ? group(entry.getString("Slot")) : EquipmentSlotGroup.ANY;
                if (group != null) {
                    entries.add(new ItemAttributeModifiers.Entry(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute.get()), modifier, group));
                }
            }
            return new ItemAttributeModifiers(List.copyOf(entries), DataComponents.shown(stack, HIDE_MODIFIERS));
        }

        /** the item's own modifiers, as 1.20.4's item gives them for each slot */
        @Override
        public ItemAttributeModifiers prototype(ItemStack stack) {
            if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem) {
                // 1.20.5's armour gives its modifiers by getDefaultAttributeModifiers(), not in the component (empty)
                return ItemAttributeModifiers.EMPTY;
            }
            List<ItemAttributeModifiers.Entry> entries = new ArrayList<>();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                Multimap<Attribute, AttributeModifier> modifiers = stack.getItem().getDefaultAttributeModifiers(slot);
                EquipmentSlotGroup group = group(slot.getName());
                modifiers.forEach((attribute, modifier) ->
                        entries.add(new ItemAttributeModifiers.Entry(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute), modifier, group)));
            }
            return entries.isEmpty() ? ItemAttributeModifiers.EMPTY : new ItemAttributeModifiers(List.copyOf(entries), true);
        }

        @Override
        public void set(ItemStack stack, ItemAttributeModifiers value) {
            ItemAttributeModifiers own = ComponentStore.prototype(stack, DataComponents.ATTRIBUTE_MODIFIERS);
            if (value == null || own != null && DataComponents.same(value, own.withTooltip(value.showInTooltip())) && value.showInTooltip()) {
                // the item's own: nothing of the stack's
                stack.removeTagKey("AttributeModifiers");
                DataComponents.show(stack, HIDE_MODIFIERS, true);
            } else {
                ListTag list = new ListTag();
                for (ItemAttributeModifiers.Entry entry : value.modifiers()) {
                    ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(entry.attribute().value());
                    if (id == null) {
                        continue;
                    }
                    for (String slot : SLOTS.getOrDefault(entry.slot(), entry.slot() == EquipmentSlotGroup.ANY
                            ? java.util.Collections.singletonList(null) : List.of(entry.slot().getSerializedName()))) {
                        CompoundTag tag = entry.modifier().save();
                        tag.putString("AttributeName", id.toString());
                        if (slot != null) {
                            tag.putString("Slot", slot);
                        }
                        list.add(tag);
                    }
                }
                stack.getOrCreateTag().put("AttributeModifiers", list);
                DataComponents.show(stack, HIDE_MODIFIERS, value.showInTooltip());
            }
            DataComponents.dropEmptyTag(stack);
        }

        /** the group of one slot, by 1.20.4's slot name (EquipmentSlotGroup.bySlot would give BODY for the chest) */
        static EquipmentSlotGroup group(String slot) {
            for (EquipmentSlotGroup group : EquipmentSlotGroup.values()) {
                if (group != EquipmentSlotGroup.BODY && group.getSerializedName().equals(slot)) {
                    return group;
                }
            }
            return null;
        }
    }

    /** 1.21's rarity: on the item in 1.20.4 (its rarity of a plain stack; enchanting raises the stack's, not this) */
    public static final class ItemRarity implements DataComponentType.Vanilla<net.minecraft.world.item.Rarity> {
        static final com.mojang.serialization.Codec<net.minecraft.world.item.Rarity> CODEC = com.mojang.serialization.Codec.STRING.comapFlatMap(name -> {
            for (net.minecraft.world.item.Rarity rarity : net.minecraft.world.item.Rarity.values()) {
                if (rarity.name().toLowerCase(java.util.Locale.ROOT).equals(name)) {
                    return com.mojang.serialization.DataResult.success(rarity);
                }
            }
            return com.mojang.serialization.DataResult.error(() -> "Unknown rarity: " + name);
        }, rarity -> rarity.name().toLowerCase(java.util.Locale.ROOT));

        /** a stack's own rarity (1.21 sets it per stack: a loot table's set_components), kept beside the item's */
        public static final String KEY = "ladder:rarity";

        /** the stack's own rarity, or null (the item's) */
        @javax.annotation.Nullable
        public static net.minecraft.world.item.Rarity own(ItemStack stack) {
            net.minecraft.nbt.CompoundTag tag = stack.getTag();
            return tag == null || !tag.contains(KEY) ? null : CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get(KEY)).result().orElse(null);
        }

        @Override
        public net.minecraft.world.item.Rarity get(ItemStack stack) {
            net.minecraft.world.item.Rarity own = own(stack);
            return own != null ? own : this.prototype(stack);
        }

        @Override
        public net.minecraft.world.item.Rarity prototype(ItemStack stack) {
            return stack.getItem().getRarity(new ItemStack(stack.getItem()));
        }

        @Override
        public void set(ItemStack stack, net.minecraft.world.item.Rarity value) {
            if (value == null || value == this.prototype(stack)) {
                if (stack.getTag() != null) {
                    stack.getTag().remove(KEY);
                    if (stack.getTag().isEmpty()) {
                        stack.setTag(null);
                    }
                }
                return;
            }
            stack.getOrCreateTag().putString(KEY, value.name().toLowerCase(java.util.Locale.ROOT));
        }
    }
}
