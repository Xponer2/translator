/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import ladder.carried.net.minecraft.world.item.component.DyedItemColor;
import ladder.carried.net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * 1.20.5's {@code DataComponents}, the game's own components, on 1.20.4's data of the stack: each reads and writes what
 * 1.20.4 keeps for it (the custom name in its display tag, the damage, the stack's own NBT), so 1.20.4's code and a
 * converted mod see one value. Only these are here yet; a mod using another one is a conversion problem (it isn't in
 * this class), not a silent loss. Values 1.20.4 can't keep per stack (another maximum damage or stack size than the
 * item's) fail when set.
 */
public final class DataComponents {
    /** where a stack's enchantment_glint_override is kept (1.20.4 has none: the ItemStack mixin reads it in hasFoil) */
    static final String GLINT_KEY = "ladder:enchantment_glint_override";

    /** where custom data keys that are also the game's own keys are kept (they'd overwrite the stack's state) */
    static final String CUSTOM_KEPT = "ladder:custom_data";

    /** the keys of a stack's tag that are the game's own data in 1.20.4 (not a mod's custom data) */
    static final Set<String> GAME_KEYS = Set.of("display", "Damage", "Unbreakable", "Enchantments", "StoredEnchantments", "AttributeModifiers",
            "HideFlags", "RepairCost", "CanDestroy", "CanPlaceOn", "CustomModelData", "BlockEntityTag", "BlockStateTag", "EntityTag", "Potion",
            "CustomPotionEffects", "CustomPotionColor", "Trim", "SkullOwner", "Fireworks", "Explosion", "pages", "author", "title", "generation",
            "resolved", "Decorations", "map", "map_scale_direction", "LodestoneDimension", "LodestonePos", "LodestoneTracked", "Items", "Charged",
            "ChargedProjectiles", "Recipes", "DebugProperty", "BucketVariantTag", "instrument", "ladder:components", CUSTOM_KEPT, GLINT_KEY);

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
    /** 1.20.4's HideFlags bits that 1.20.5 turned into each component's show_in_tooltip (ItemStackComponentizationFix) */
    static final int HIDE_ENCHANTMENTS = 1, HIDE_ADDITIONAL = 32, HIDE_DYE = 64;

    static boolean shown(ItemStack stack, int bit) {
        CompoundTag tag = stack.getTag();
        return tag == null || (tag.getInt("HideFlags") & bit) == 0;
    }

    static void show(ItemStack stack, int bit, boolean shown) {
        CompoundTag tag = stack.getTag();
        int flags = tag == null ? 0 : tag.getInt("HideFlags");
        int changed = shown ? flags & ~bit : flags | bit;
        if (changed == flags) {
            return;
        }
        if (changed == 0) {
            stack.removeTagKey("HideFlags");
        } else {
            stack.getOrCreateTag().putInt("HideFlags", changed);
        }
    }

    /**
     * Whether two values of a component are the same as 1.21 compares them: its records compare every field, but
     * 1.20.4's AttributeModifier (inside attribute_modifiers) compares only its UUID, so those are compared whole.
     */
    static boolean same(Object a, Object b) {
        if (a instanceof ladder.carried.net.minecraft.world.item.component.ItemAttributeModifiers x
                && b instanceof ladder.carried.net.minecraft.world.item.component.ItemAttributeModifiers y) {
            if (x.showInTooltip() != y.showInTooltip() || x.modifiers().size() != y.modifiers().size()) {
                return false;
            }
            for (int i = 0; i < x.modifiers().size(); i++) {
                var e = x.modifiers().get(i);
                var f = y.modifiers().get(i);
                if (e.attribute().value() != f.attribute().value() || e.slot() != f.slot() || !e.modifier().getId().equals(f.modifier().getId())
                        || e.modifier().getAmount() != f.modifier().getAmount() || e.modifier().getOperation() != f.modifier().getOperation()
                        || !e.modifier().save().getString("Name").equals(f.modifier().save().getString("Name"))) {
                    return false;
                }
            }
            return true;
        }
        return java.util.Objects.equals(a, b);
    }

    static void dropEmptyTag(ItemStack stack) {
        if (stack.getTag() != null && stack.getTag().isEmpty()) {
            stack.setTag(null);
        }
    }

    /** 1.21's dyed_color: 1.20.4's display.color, shown unless HideFlags has the dye bit. */
    public static final DataComponentType<DyedItemColor> DYED_COLOR = new Game<>("dyed_color", DyedItemColor.CODEC, new DataComponentType.Vanilla<>() {
        @Override
        public DyedItemColor get(ItemStack stack) {
            CompoundTag display = stack.getTagElement("display");
            if (display == null || !display.contains("color", Tag.TAG_ANY_NUMERIC)) {
                return null;
            }
            return new DyedItemColor(display.getInt("color"), shown(stack, HIDE_DYE));
        }

        @Override
        public DyedItemColor prototype(ItemStack stack) {
            return null;
        }

        @Override
        public void set(ItemStack stack, DyedItemColor value) {
            if (value == null) {
                CompoundTag display = stack.getTagElement("display");
                if (display != null) {
                    display.remove("color");
                    if (display.isEmpty()) {
                        stack.removeTagKey("display");
                    }
                }
                show(stack, HIDE_DYE, true);
            } else {
                stack.getOrCreateTagElement("display").putInt("color", value.rgb());
                show(stack, HIDE_DYE, value.showInTooltip());
            }
            dropEmptyTag(stack);
        }
    });

    /** 1.21's enchantments: 1.20.4's Enchantments list ({id, lvl}); every item has an empty one by default. */
    public static final DataComponentType<ItemEnchantments> ENCHANTMENTS = new Game<>("enchantments", ItemEnchantments.CODEC,
            new Enchantments("Enchantments", HIDE_ENCHANTMENTS, true));

    /** 1.21's stored_enchantments (an enchanted book's): 1.20.4's StoredEnchantments list. */
    public static final DataComponentType<ItemEnchantments> STORED_ENCHANTMENTS = new Game<>("stored_enchantments", ItemEnchantments.CODEC,
            new Enchantments("StoredEnchantments", HIDE_ADDITIONAL, false));

    /** An enchantment list of the stack's tag as ItemEnchantments, both ways (ids of 1.20.4's enchantment registry). */
    static final class Enchantments implements DataComponentType.Vanilla<ItemEnchantments> {
        private final String key;
        private final int hideBit;
        private final boolean everyItem;

        Enchantments(String key, int hideBit, boolean everyItem) {
            this.key = key;
            this.hideBit = hideBit;
            this.everyItem = everyItem;
        }

        @Override
        public ItemEnchantments get(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains(this.key, Tag.TAG_LIST)) {
                return null;
            }
            ItemEnchantments.Mutable value = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            ListTag list = tag.getList(this.key, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                ResourceLocation id = EnchantmentHelper.getEnchantmentId(entry);
                Enchantment enchantment = id == null ? null : BuiltInRegistries.ENCHANTMENT.get(id);
                if (enchantment != null) {
                    value.set(enchantment, EnchantmentHelper.getEnchantmentLevel(entry));
                }
            }
            return value.toImmutable().withTooltip(shown(stack, this.hideBit));
        }

        @Override
        public ItemEnchantments prototype(ItemStack stack) {
            return this.everyItem || stack.is(Items.ENCHANTED_BOOK) ? ItemEnchantments.EMPTY : null;
        }

        @Override
        public void set(ItemStack stack, ItemEnchantments value) {
            if (value == null || value.isEmpty()) {
                stack.removeTagKey(this.key);
            } else {
                ListTag list = new ListTag();
                for (Object2IntMap.Entry<Holder<Enchantment>> e : value.entrySet()) {
                    ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(e.getKey().value());
                    if (id != null) {
                        list.add(EnchantmentHelper.storeEnchantment(id, e.getIntValue()));
                    }
                }
                stack.getOrCreateTag().put(this.key, list);
            }
            show(stack, this.hideBit, value == null || value.equals(value.withTooltip(true)));
            dropEmptyTag(stack);
        }
    }

    public static final DataComponentType<ladder.carried.net.minecraft.world.item.alchemy.PotionContents> POTION_CONTENTS = new Game<>("potion_contents",
            ladder.carried.net.minecraft.world.item.alchemy.PotionContents.CODEC, new StackComponents.Potions());

    public static final DataComponentType<ladder.carried.net.minecraft.world.item.component.ItemLore> LORE = new Game<>("lore",
            ladder.carried.net.minecraft.world.item.component.ItemLore.CODEC, new StackComponents.Lore());

    public static final DataComponentType<ladder.carried.net.minecraft.world.item.component.CustomModelData> CUSTOM_MODEL_DATA = new Game<>("custom_model_data",
            ladder.carried.net.minecraft.world.item.component.CustomModelData.CODEC, new StackComponents.ModelData());

    public static final DataComponentType<net.minecraft.world.food.FoodProperties> FOOD = new Game<>("food",
            ladder.adapter.food.FoodAdapters.directCodec(), new StackComponents.Food());

    public static final DataComponentType<ladder.carried.net.minecraft.world.item.component.ItemAttributeModifiers> ATTRIBUTE_MODIFIERS = new Game<>("attribute_modifiers",
            ladder.carried.net.minecraft.world.item.component.ItemAttributeModifiers.CODEC, new StackComponents.Attributes());

    public static final DataComponentType<net.minecraft.world.item.Rarity> RARITY = new Game<>("rarity", StackComponents.ItemRarity.CODEC,
            new StackComponents.ItemRarity());

    public static final DataComponentType<ladder.carried.net.minecraft.world.item.component.ItemContainerContents> CONTAINER = new Game<>("container",
            ladder.carried.net.minecraft.world.item.component.ItemContainerContents.CODEC, new BlockComponents.Container());

    public static final DataComponentType<CustomData> BLOCK_ENTITY_DATA = new Game<>("block_entity_data", CustomData.CODEC,
            new BlockComponents.BlockEntityData());

    public static final DataComponentType<CustomData> ENTITY_DATA = new Game<>("entity_data", CustomData.CODEC, new BlockComponents.Compound("EntityTag"));

    public static final DataComponentType<ladder.carried.net.minecraft.world.item.component.BlockItemStateProperties> BLOCK_STATE = new Game<>("block_state",
            ladder.carried.net.minecraft.world.item.component.BlockItemStateProperties.CODEC, new BlockComponents.BlockState());

    public static final DataComponentType<ladder.carried.net.minecraft.world.item.component.BundleContents> BUNDLE_CONTENTS = new Game<>("bundle_contents",
            ladder.carried.net.minecraft.world.item.component.BundleContents.CODEC, new BlockComponents.Bundle());

    public static final DataComponentType<ladder.carried.net.minecraft.world.item.component.Unbreakable> UNBREAKABLE = new Game<>("unbreakable",
            ladder.carried.net.minecraft.world.item.component.Unbreakable.CODEC, new BlockComponents.Unbreaking());

    public static final DataComponentType<Integer> REPAIR_COST = new Game<>("repair_cost", ExtraCodecs.NON_NEGATIVE_INT, new BlockComponents.RepairCost());

    public static final DataComponentType<net.minecraft.util.Unit> HIDE_ADDITIONAL_TOOLTIP = new Game<>("hide_additional_tooltip",
            Codec.unit(net.minecraft.util.Unit.INSTANCE), new BlockComponents.HideAdditional());

    public static final DataComponentType<List<ladder.carried.net.minecraft.world.level.block.entity.BeehiveBlockEntity$Occupant>> BEES = new Game<>("bees",
            ladder.carried.net.minecraft.world.level.block.entity.BeehiveBlockEntity$Occupant.LIST_CODEC, new BlockComponents.Bees());

    /** 1.21's enchantment_glint_override; 1.21.1's own items with it (tools/item_defaults.py) have true */
    public static final DataComponentType<Boolean> ENCHANTMENT_GLINT_OVERRIDE = new Game<>("enchantment_glint_override", Codec.BOOL,
            new DataComponentType.Vanilla<>() {
                private final Set<net.minecraft.world.item.Item> shining = Set.of(Items.ENCHANTED_GOLDEN_APPLE, Items.EXPERIENCE_BOTTLE,
                        Items.WRITTEN_BOOK, Items.NETHER_STAR, Items.END_CRYSTAL, Items.ENCHANTED_BOOK, Items.DEBUG_STICK);

                @Override
                public Boolean get(ItemStack stack) {
                    CompoundTag tag = stack.getTag();
                    return tag != null && tag.contains(GLINT_KEY, Tag.TAG_BYTE) ? tag.getBoolean(GLINT_KEY) : null;
                }

                @Override
                public Boolean prototype(ItemStack stack) {
                    return this.shining.contains(stack.getItem()) ? Boolean.TRUE : null;
                }

                @Override
                public void set(ItemStack stack, Boolean value) {
                    if (value == null) {
                        stack.removeTagKey(GLINT_KEY);
                        dropEmptyTag(stack);
                    } else {
                        stack.getOrCreateTag().putBoolean(GLINT_KEY, value);
                    }
                }
            });

    /** 1.21's firework_explosion: 1.20.4's Explosion ({Type, Colors, FadeColors, Trail, Flicker}) of a firework star */
    public static final DataComponentType<ladder.carried.net.minecraft.world.item.component.FireworkExplosion> FIREWORK_EXPLOSION = new Game<>(
            "firework_explosion", ladder.carried.net.minecraft.world.item.component.FireworkExplosion.CODEC, new DataComponentType.Vanilla<>() {
                @Override
                public ladder.carried.net.minecraft.world.item.component.FireworkExplosion get(ItemStack stack) {
                    CompoundTag tag = stack.getTagElement("Explosion");
                    return tag == null ? null : explosion(tag);
                }

                @Override
                public ladder.carried.net.minecraft.world.item.component.FireworkExplosion prototype(ItemStack stack) {
                    return null;
                }

                @Override
                public void set(ItemStack stack, ladder.carried.net.minecraft.world.item.component.FireworkExplosion value) {
                    if (value == null) {
                        stack.removeTagKey("Explosion");
                        dropEmptyTag(stack);
                    } else {
                        stack.getOrCreateTag().put("Explosion", explosion(value));
                    }
                }
            });

    static ladder.carried.net.minecraft.world.item.component.FireworkExplosion explosion(CompoundTag tag) {
        return new ladder.carried.net.minecraft.world.item.component.FireworkExplosion(
                ladder.carried.net.minecraft.world.item.component.FireworkExplosion.Shape.byId(tag.getByte("Type")),
                new it.unimi.dsi.fastutil.ints.IntArrayList(tag.getIntArray("Colors")), new it.unimi.dsi.fastutil.ints.IntArrayList(tag.getIntArray("FadeColors")),
                tag.getBoolean("Trail"), tag.getBoolean("Flicker"));
    }

    static CompoundTag explosion(ladder.carried.net.minecraft.world.item.component.FireworkExplosion value) {
        CompoundTag tag = new CompoundTag();
        tag.putByte("Type", (byte) value.shape().getId());
        tag.putIntArray("Colors", value.colors().toIntArray());
        tag.putIntArray("FadeColors", value.fadeColors().toIntArray());
        tag.putBoolean("Trail", value.hasTrail());
        tag.putBoolean("Flicker", value.hasTwinkle());
        return tag;
    }

    /** 1.21's fireworks: 1.20.4's Fireworks ({Flight, Explosions}); a rocket has a flight of 1 and no explosion by default */
    public static final DataComponentType<ladder.carried.net.minecraft.world.item.component.Fireworks> FIREWORKS = new Game<>("fireworks",
            ladder.carried.net.minecraft.world.item.component.Fireworks.CODEC, new DataComponentType.Vanilla<>() {
                @Override
                public ladder.carried.net.minecraft.world.item.component.Fireworks get(ItemStack stack) {
                    CompoundTag tag = stack.getTagElement("Fireworks");
                    if (tag == null) {
                        return null;
                    }
                    List<ladder.carried.net.minecraft.world.item.component.FireworkExplosion> explosions = new java.util.ArrayList<>();
                    ListTag list = tag.getList("Explosions", Tag.TAG_COMPOUND);
                    for (int i = 0; i < list.size(); i++) {
                        explosions.add(explosion(list.getCompound(i)));
                    }
                    return new ladder.carried.net.minecraft.world.item.component.Fireworks(tag.getByte("Flight") & 255, List.copyOf(explosions));
                }

                @Override
                public ladder.carried.net.minecraft.world.item.component.Fireworks prototype(ItemStack stack) {
                    return stack.is(Items.FIREWORK_ROCKET) ? new ladder.carried.net.minecraft.world.item.component.Fireworks(1, List.of()) : null;
                }

                @Override
                public void set(ItemStack stack, ladder.carried.net.minecraft.world.item.component.Fireworks value) {
                    if (value == null) {
                        stack.removeTagKey("Fireworks");
                        dropEmptyTag(stack);
                    } else {
                        CompoundTag tag = new CompoundTag();
                        tag.putByte("Flight", (byte) value.flightDuration());
                        ListTag list = new ListTag();
                        for (ladder.carried.net.minecraft.world.item.component.FireworkExplosion explosion : value.explosions()) {
                            list.add(explosion(explosion));
                        }
                        tag.put("Explosions", list);
                        stack.getOrCreateTag().put("Fireworks", tag);
                    }
                }
            });

    /** 1.21's profile (a player's head): 1.20.4's SkullOwner (a profile compound, or a bare name) */
    public static final DataComponentType<ladder.carried.net.minecraft.world.item.component.ResolvableProfile> PROFILE = new Game<>("profile",
            ladder.carried.net.minecraft.world.item.component.ResolvableProfile.CODEC, new DataComponentType.Vanilla<>() {
                @Override
                public ladder.carried.net.minecraft.world.item.component.ResolvableProfile get(ItemStack stack) {
                    CompoundTag tag = stack.getTag();
                    if (tag == null || !tag.contains("SkullOwner")) {
                        return null;
                    }
                    if (tag.contains("SkullOwner", Tag.TAG_STRING)) {
                        return new ladder.carried.net.minecraft.world.item.component.ResolvableProfile(java.util.Optional.of(tag.getString("SkullOwner")),
                                java.util.Optional.empty(), new com.mojang.authlib.properties.PropertyMap());
                    }
                    CompoundTag owner = tag.getCompound("SkullOwner");
                    java.util.Optional<String> name = owner.contains("Name", Tag.TAG_STRING) ? java.util.Optional.of(owner.getString("Name"))
                            : java.util.Optional.empty();
                    java.util.Optional<java.util.UUID> id = owner.hasUUID("Id") ? java.util.Optional.of(owner.getUUID("Id")) : java.util.Optional.empty();
                    com.mojang.authlib.properties.PropertyMap properties = new com.mojang.authlib.properties.PropertyMap();
                    com.mojang.authlib.GameProfile read = net.minecraft.nbt.NbtUtils.readGameProfile(owner);
                    if (read != null) {
                        properties.putAll(read.getProperties());
                    }
                    return new ladder.carried.net.minecraft.world.item.component.ResolvableProfile(name, id, properties);
                }

                @Override
                public ladder.carried.net.minecraft.world.item.component.ResolvableProfile prototype(ItemStack stack) {
                    return null;
                }

                @Override
                public void set(ItemStack stack, ladder.carried.net.minecraft.world.item.component.ResolvableProfile value) {
                    if (value == null) {
                        stack.removeTagKey("SkullOwner");
                        dropEmptyTag(stack);
                        return;
                    }
                    CompoundTag owner = new CompoundTag();
                    value.name().ifPresent(name -> owner.putString("Name", name));
                    value.id().ifPresent(id -> owner.putUUID("Id", id));
                    if (!value.properties().isEmpty()) {
                        com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(value.id().orElse(net.minecraft.Util.NIL_UUID),
                                value.name().orElse(""));
                        profile.getProperties().putAll(value.properties());
                        CompoundTag written = net.minecraft.nbt.NbtUtils.writeGameProfile(new CompoundTag(), profile);
                        if (written.contains("Properties")) {
                            owner.put("Properties", written.get("Properties"));
                        }
                    }
                    stack.getOrCreateTag().put("SkullOwner", owner);
                }
            });

    /** 1.20.4's HideFlags bit of the upgrades (armour trim) tooltip */
    static final int HIDE_UPGRADES = 128;

    /**
     * 1.21's trim: 1.20.4's Trim ({material, pattern}, read and written with the current registries, as 1.20.4's own
     * ArmorTrim.getTrim/setTrim do), shown unless HideFlags has bit 128 (the value's show_in_tooltip is that bit).
     */
    public static final DataComponentType<net.minecraft.world.item.armortrim.ArmorTrim> TRIM = new Game<>("trim",
            net.minecraft.world.item.armortrim.ArmorTrim.CODEC, new DataComponentType.Vanilla<>() {
                @Override
                public net.minecraft.world.item.armortrim.ArmorTrim get(ItemStack stack) {
                    if (stack.getTagElement("Trim") == null) {
                        return null;
                    }
                    net.minecraft.core.RegistryAccess registries = ladder.adapter.bridge.Conversions.registries();
                    return registries == null ? null : net.minecraft.world.item.armortrim.ArmorTrim.getTrim(registries, stack, true).orElse(null);
                }

                @Override
                public net.minecraft.world.item.armortrim.ArmorTrim prototype(ItemStack stack) {
                    return null;
                }

                @Override
                public void set(ItemStack stack, net.minecraft.world.item.armortrim.ArmorTrim value) {
                    if (value == null) {
                        stack.removeTagKey("Trim");
                        show(stack, HIDE_UPGRADES, true);
                        dropEmptyTag(stack);
                        return;
                    }
                    net.minecraft.core.RegistryAccess registries = ladder.adapter.bridge.Conversions.registries();
                    if (registries == null || !net.minecraft.world.item.armortrim.ArmorTrim.setTrim(registries, stack, value)) {
                        throw new IllegalStateException("1.20.4 can't keep the trim " + value + " on " + stack + " (no registries, or not trimmable)");
                    }
                }
            });

    public static final DataComponentType<ladder.carried.net.minecraft.world.item.component.ChargedProjectiles> CHARGED_PROJECTILES = new Game<>(
            "charged_projectiles", ladder.carried.net.minecraft.world.item.component.ChargedProjectiles.CODEC, new BlockComponents.Projectiles());

    public static final DataComponentType<ladder.carried.net.minecraft.world.item.component.WritableBookContent> WRITABLE_BOOK_CONTENT = new Game<>(
            "writable_book_content", ladder.carried.net.minecraft.world.item.component.WritableBookContent.CODEC, new BlockComponents.WritableBook());

    public static final DataComponentType<ladder.carried.net.minecraft.world.level.saveddata.maps.MapId> MAP_ID = new Game<>("map_id",
            ladder.carried.net.minecraft.world.level.saveddata.maps.MapId.CODEC, new BlockComponents.MapNumber());

    public static final DataComponentType<ladder.carried.net.minecraft.world.item.component.SuspiciousStewEffects> SUSPICIOUS_STEW_EFFECTS = new Game<>(
            "suspicious_stew_effects", ladder.carried.net.minecraft.world.item.component.SuspiciousStewEffects.CODEC, new BlockComponents.Stew());

    public static final List<DataComponentType<?>> ALL = List.of(CHARGED_PROJECTILES, WRITABLE_BOOK_CONTENT, MAP_ID, SUSPICIOUS_STEW_EFFECTS, TRIM, PROFILE, FIREWORKS, ENCHANTMENT_GLINT_OVERRIDE, FIREWORK_EXPLOSION, BEES, CONTAINER, BLOCK_ENTITY_DATA, ENTITY_DATA, BLOCK_STATE, BUNDLE_CONTENTS, UNBREAKABLE,
            REPAIR_COST, HIDE_ADDITIONAL_TOOLTIP, RARITY, CUSTOM_DATA, CUSTOM_NAME, ITEM_NAME, DAMAGE, MAX_DAMAGE, MAX_STACK_SIZE,
            DYED_COLOR, ENCHANTMENTS, STORED_ENCHANTMENTS, POTION_CONTENTS, LORE, CUSTOM_MODEL_DATA, FOOD, ATTRIBUTE_MODIFIERS);

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
