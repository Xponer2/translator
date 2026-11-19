/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ladder.carried.net.minecraft.world.item.component.BlockItemStateProperties;
import ladder.carried.net.minecraft.world.level.block.entity.BeehiveBlockEntity$Occupant;
import ladder.carried.net.minecraft.world.item.component.BundleContents;
import ladder.carried.net.minecraft.world.item.component.ItemContainerContents;
import ladder.carried.net.minecraft.world.item.component.Unbreakable;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The game's components of blocks as items, and a few flags, on 1.20.4's data of the stack (as Mojang's
 * ItemStackComponentizationFix moves them): a container's items in BlockEntityTag.Items, the rest of the block entity's
 * data in BlockEntityTag (with the block entity's id, as 1.21 keeps it), an entity's in EntityTag, block state
 * properties in BlockStateTag, a bundle's items in Items, the unbreakable flag, the repair cost, HideFlags 32.
 */
final class BlockComponents {
    /** BlockEntityTag keys other components keep (not the block entity data's) */
    static final Set<String> BLOCK_ENTITY_KEPT = Set.of("Items", "Bees");
    static final int HIDE_UNBREAKABLE = 4;

    private BlockComponents() {
    }

    static void dropEmptyBlockEntityTag(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null && tag.isEmpty()) {
            stack.removeTagKey("BlockEntityTag");
        }
        DataComponents.dropEmptyTag(stack);
    }

    /**
     * 1.21's container: BlockEntityTag.Items ({Slot, the stack}). The items 1.21.1 gives an empty one by default
     * (tools/item_defaults.py over its Items): the shulker boxes and these.
     */
    static final class Container implements DataComponentType.Vanilla<ItemContainerContents> {
        private static final Set<net.minecraft.world.item.Item> CONTAINERS = Set.of(Items.CHISELED_BOOKSHELF, Items.CHEST, Items.FURNACE,
                Items.ENDER_CHEST, Items.HOPPER, Items.DISPENSER, Items.DROPPER, Items.TRAPPED_CHEST, Items.CRAFTER, Items.BREWING_STAND,
                Items.BARREL, Items.SMOKER, Items.BLAST_FURNACE, Items.CAMPFIRE, Items.SOUL_CAMPFIRE);

        @Override
        public ItemContainerContents get(ItemStack stack) {
            CompoundTag block = stack.getTagElement("BlockEntityTag");
            if (block == null || !block.contains("Items", Tag.TAG_LIST)) {
                return null;
            }
            ListTag list = block.getList("Items", Tag.TAG_COMPOUND);
            Map<Integer, ItemStack> slots = new HashMap<>();
            int size = 0;
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int slot = entry.getByte("Slot") & 255;
                ItemStack item = ItemStack.of(entry);
                if (!item.isEmpty()) {
                    slots.put(slot, item);
                    size = Math.max(size, slot + 1);
                }
            }
            List<ItemStack> items = new ArrayList<>(java.util.Collections.nCopies(size, ItemStack.EMPTY));
            slots.forEach(items::set);
            return ItemContainerContents.fromItems(items);
        }

        @Override
        public ItemContainerContents prototype(ItemStack stack) {
            return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock || CONTAINERS.contains(stack.getItem())
                    ? ItemContainerContents.EMPTY : null;
        }

        @Override
        public void set(ItemStack stack, ItemContainerContents value) {
            if (value == null || value.getSlots() == 0) {
                CompoundTag block = stack.getTagElement("BlockEntityTag");
                if (block != null) {
                    block.remove("Items");
                }
            } else {
                NonNullList<ItemStack> items = NonNullList.withSize(value.getSlots(), ItemStack.EMPTY);
                value.copyInto(items);
                ListTag list = new ListTag();
                for (int slot = 0; slot < items.size(); slot++) {
                    if (!items.get(slot).isEmpty()) {
                        CompoundTag entry = new CompoundTag();
                        entry.putByte("Slot", (byte) slot);
                        items.get(slot).save(entry);
                        list.add(entry);
                    }
                }
                stack.getOrCreateTagElement("BlockEntityTag").put("Items", list);
            }
            dropEmptyBlockEntityTag(stack);
        }
    }

    /** 1.21's block_entity_data: BlockEntityTag but the keys other components keep, with the block entity's id. */
    static final class BlockEntityData implements DataComponentType.Vanilla<CustomData> {
        private static final Map<Block, String> IDS = new java.util.concurrent.ConcurrentHashMap<>();

        /** the id of the block entity the item's block makes, or "" */
        static String id(ItemStack stack) {
            if (!(stack.getItem() instanceof BlockItem item)) {
                return "";
            }
            return IDS.computeIfAbsent(item.getBlock(), block -> {
                for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
                    if (type.isValid(block.defaultBlockState())) {
                        ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
                        return id == null ? "" : id.toString();
                    }
                }
                return "";
            });
        }

        @Override
        public CustomData get(ItemStack stack) {
            CompoundTag block = stack.getTagElement("BlockEntityTag");
            if (block == null) {
                return null;
            }
            CompoundTag own = new CompoundTag();
            for (String key : block.getAllKeys()) {
                if (!BLOCK_ENTITY_KEPT.contains(key)) {
                    own.put(key, block.get(key).copy());
                }
            }
            if (own.isEmpty()) {
                return null;
            }
            String id = id(stack);
            if (!own.contains("id") && !id.isEmpty()) {
                own.putString("id", id);
            }
            return CustomData.of(own);
        }

        @Override
        public CustomData prototype(ItemStack stack) {
            return null;
        }

        @Override
        public void set(ItemStack stack, CustomData value) {
            CompoundTag block = stack.getTagElement("BlockEntityTag");
            if (block != null) {
                for (String key : List.copyOf(block.getAllKeys())) {
                    if (!BLOCK_ENTITY_KEPT.contains(key)) {
                        block.remove(key);
                    }
                }
            }
            if (value != null) {
                CompoundTag own = value.copyTag();
                // 1.20.4 places the block entity of the block itself: its own id needn't be kept
                if (own.getString("id").equals(id(stack))) {
                    own.remove("id");
                }
                CompoundTag target = stack.getOrCreateTagElement("BlockEntityTag");
                for (String key : own.getAllKeys()) {
                    target.put(key, own.get(key).copy());
                }
            }
            dropEmptyBlockEntityTag(stack);
        }
    }

    /** a compound of the stack's tag as custom data (1.21's entity_data: 1.20.4's EntityTag) */
    static final class Compound implements DataComponentType.Vanilla<CustomData> {
        private final String key;

        Compound(String key) {
            this.key = key;
        }

        @Override
        public CustomData get(ItemStack stack) {
            CompoundTag tag = stack.getTagElement(this.key);
            return tag == null ? null : CustomData.of(tag.copy());
        }

        @Override
        public CustomData prototype(ItemStack stack) {
            return null;
        }

        @Override
        public void set(ItemStack stack, CustomData value) {
            if (value == null) {
                stack.removeTagKey(this.key);
                DataComponents.dropEmptyTag(stack);
            } else {
                stack.getOrCreateTag().put(this.key, value.copyTag());
            }
        }
    }

    /** 1.21's block_state: 1.20.4's BlockStateTag (property name -> value, as strings). */
    static final class BlockState implements DataComponentType.Vanilla<BlockItemStateProperties> {
        @Override
        public BlockItemStateProperties get(ItemStack stack) {
            CompoundTag tag = stack.getTagElement("BlockStateTag");
            if (tag == null) {
                return null;
            }
            Map<String, String> properties = new LinkedHashMap<>();
            for (String key : tag.getAllKeys()) {
                Tag value = tag.get(key);
                properties.put(key, value instanceof net.minecraft.nbt.StringTag ? value.getAsString() : value.toString());
            }
            return new BlockItemStateProperties(Map.copyOf(properties));
        }

        @Override
        public BlockItemStateProperties prototype(ItemStack stack) {
            return null;
        }

        @Override
        public void set(ItemStack stack, BlockItemStateProperties value) {
            if (value == null || value.isEmpty()) {
                stack.removeTagKey("BlockStateTag");
                DataComponents.dropEmptyTag(stack);
            } else {
                CompoundTag tag = new CompoundTag();
                value.properties().forEach(tag::putString);
                stack.getOrCreateTag().put("BlockStateTag", tag);
            }
        }
    }

    /** 1.21's bundle_contents: 1.20.4's Items list of the bundle (the newest first, as both keep it). */
    static final class Bundle implements DataComponentType.Vanilla<BundleContents> {
        @Override
        public BundleContents get(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains("Items", Tag.TAG_LIST)) {
                return null;
            }
            ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                ItemStack item = ItemStack.of(list.getCompound(i));
                if (!item.isEmpty()) {
                    items.add(item);
                }
            }
            return new BundleContents(items);
        }

        @Override
        public BundleContents prototype(ItemStack stack) {
            return stack.is(Items.BUNDLE) ? BundleContents.EMPTY : null;
        }

        @Override
        public void set(ItemStack stack, BundleContents value) {
            if (value == null || value.isEmpty()) {
                stack.removeTagKey("Items");
                DataComponents.dropEmptyTag(stack);
            } else {
                ListTag list = new ListTag();
                for (ItemStack item : value.items()) {
                    list.add(item.save(new CompoundTag()));
                }
                stack.getOrCreateTag().put("Items", list);
            }
        }
    }

    /** 1.21's unbreakable: 1.20.4's Unbreakable flag, shown unless HideFlags has bit 4. */
    static final class Unbreaking implements DataComponentType.Vanilla<Unbreakable> {
        @Override
        public Unbreakable get(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            return tag != null && tag.getBoolean("Unbreakable") ? new Unbreakable(DataComponents.shown(stack, HIDE_UNBREAKABLE)) : null;
        }

        @Override
        public Unbreakable prototype(ItemStack stack) {
            return null;
        }

        @Override
        public void set(ItemStack stack, Unbreakable value) {
            if (value == null) {
                stack.removeTagKey("Unbreakable");
                DataComponents.show(stack, HIDE_UNBREAKABLE, true);
            } else {
                stack.getOrCreateTag().putBoolean("Unbreakable", true);
                DataComponents.show(stack, HIDE_UNBREAKABLE, value.showInTooltip());
            }
            DataComponents.dropEmptyTag(stack);
        }
    }

    /** 1.21's repair_cost: 1.20.4's RepairCost; every item has 0 by default. */
    static final class RepairCost implements DataComponentType.Vanilla<Integer> {
        @Override
        public Integer get(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            return tag != null && tag.contains("RepairCost", Tag.TAG_INT) ? tag.getInt("RepairCost") : null;
        }

        @Override
        public Integer prototype(ItemStack stack) {
            return 0;
        }

        @Override
        public void set(ItemStack stack, Integer value) {
            if (value == null || value == 0) {
                stack.removeTagKey("RepairCost");
                DataComponents.dropEmptyTag(stack);
            } else {
                stack.getOrCreateTag().putInt("RepairCost", value);
            }
        }
    }

    /** 1.21's hide_additional_tooltip: 1.20.4's HideFlags bit 32. */
    static final class HideAdditional implements DataComponentType.Vanilla<net.minecraft.util.Unit> {
        @Override
        public net.minecraft.util.Unit get(ItemStack stack) {
            return DataComponents.shown(stack, DataComponents.HIDE_ADDITIONAL) ? null : net.minecraft.util.Unit.INSTANCE;
        }

        @Override
        public net.minecraft.util.Unit prototype(ItemStack stack) {
            return null;
        }

        @Override
        public void set(ItemStack stack, net.minecraft.util.Unit value) {
            DataComponents.show(stack, DataComponents.HIDE_ADDITIONAL, value == null);
            DataComponents.dropEmptyTag(stack);
        }
    }

    /** 1.21's bees: BlockEntityTag.Bees ({EntityData, TicksInHive, MinOccupationTicks}); hives and nests have none by default. */
    static final class Bees implements DataComponentType.Vanilla<List<BeehiveBlockEntity$Occupant>> {
        @Override
        public List<BeehiveBlockEntity$Occupant> get(ItemStack stack) {
            CompoundTag block = stack.getTagElement("BlockEntityTag");
            if (block == null || !block.contains("Bees", Tag.TAG_LIST)) {
                return null;
            }
            ListTag list = block.getList("Bees", Tag.TAG_COMPOUND);
            List<BeehiveBlockEntity$Occupant> bees = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                CompoundTag bee = list.getCompound(i);
                bees.add(new BeehiveBlockEntity$Occupant(CustomData.of(bee.getCompound("EntityData")), bee.getInt("TicksInHive"),
                        bee.getInt("MinOccupationTicks")));
            }
            return List.copyOf(bees);
        }

        @Override
        public List<BeehiveBlockEntity$Occupant> prototype(ItemStack stack) {
            return stack.is(Items.BEEHIVE) || stack.is(Items.BEE_NEST) ? List.of() : null;
        }

        @Override
        public void set(ItemStack stack, List<BeehiveBlockEntity$Occupant> value) {
            if (value == null || value.isEmpty()) {
                CompoundTag block = stack.getTagElement("BlockEntityTag");
                if (block != null) {
                    block.remove("Bees");
                }
            } else {
                ListTag list = new ListTag();
                for (BeehiveBlockEntity$Occupant occupant : value) {
                    CompoundTag bee = new CompoundTag();
                    bee.put("EntityData", occupant.entityData().copyTag());
                    bee.putInt("TicksInHive", occupant.ticksInHive());
                    bee.putInt("MinOccupationTicks", occupant.minTicksInHive());
                    list.add(bee);
                }
                stack.getOrCreateTagElement("BlockEntityTag").put("Bees", list);
            }
            dropEmptyBlockEntityTag(stack);
        }
    }
}
