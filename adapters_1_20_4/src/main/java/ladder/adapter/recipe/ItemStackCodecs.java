/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.recipe;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ladder.adapter.component.ComponentPatches;
import ladder.carried.net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1.20.5's item stack codecs beside ComponentCodecs' (CODEC, OPTIONAL_CODEC): STRICT_CODEC checks the stack size,
 * SINGLE_ITEM_CODEC is an object with the item's id and components (1.20.4: the id alone, 1.20.5's SIMPLE_ITEM_CODEC).
 * A mod's reads of these fields come here (F rows): its data in 1.21's form reads as on 1.21.
 */
public final class ItemStackCodecs {
    private ItemStackCodecs() {
    }

    public static final Codec<Holder<Item>> ITEM_NON_AIR = ExtraCodecs.validate(BuiltInRegistries.ITEM.holderByNameCodec(),
            item -> item.value() == Items.AIR ? DataResult.error(() -> "Item must not be minecraft:air") : DataResult.success(item));

    /** 1.20.5's ItemStack.CODEC (the component codecs') */
    public static final Codec<ItemStack> CODEC = ladder.adapter.component.ComponentCodecs.ITEM_STACK;

    public static final Codec<ItemStack> SINGLE_ITEM_CODEC = ExtraCodecs.lazyInitializedCodec(() -> RecordCodecBuilder.create(i -> i.group(
            ITEM_NON_AIR.fieldOf("id").forGetter(ItemStack::getItemHolder),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ComponentPatches::getComponentsPatch))
            .apply(i, (item, patch) -> ComponentPatches.create(item, 1, patch))));

    public static final Codec<ItemStack> STRICT_CODEC = ExtraCodecs.validate(CODEC, ItemStackCodecs::validateStrict);
    public static final Codec<ItemStack> STRICT_SINGLE_ITEM_CODEC = ExtraCodecs.validate(SINGLE_ITEM_CODEC, ItemStackCodecs::validateStrict);
    public static final Codec<ItemStack> OPTIONAL_CODEC = ladder.adapter.component.ComponentCodecs.OPTIONAL_ITEM_STACK;
    public static final Codec<ItemStack> SIMPLE_ITEM_CODEC = ITEM_NON_AIR.xmap(ItemStack::new, ItemStack::getItemHolder);

    /** 1.20.5's check of a stack read strictly: no more than its item stacks to */
    static DataResult<ItemStack> validateStrict(ItemStack stack) {
        return stack.getCount() > stack.getMaxStackSize()
                ? DataResult.error(() -> "Item stack with stack size of " + stack.getCount() + " was larger than maximum: " + stack.getMaxStackSize())
                : DataResult.success(stack);
    }

    /** {@code ItemStack.lenientOptionalFieldOf(name)} */
    public static MapCodec<ItemStack> lenientOptionalFieldOf(String name) {
        // (1.20.4's DFU: an optional field is lenient)
        return CODEC.optionalFieldOf(name).xmap(stack -> stack.orElse(ItemStack.EMPTY), stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));
    }

    public static Codec<ItemStack> codec() {
        return CODEC;
    }

    public static Codec<ItemStack> strictCodec() {
        return STRICT_CODEC;
    }

    public static Codec<ItemStack> optionalCodec() {
        return OPTIONAL_CODEC;
    }

    public static Codec<ItemStack> singleItemCodec() {
        return SINGLE_ITEM_CODEC;
    }

    public static Codec<ItemStack> strictSingleItemCodec() {
        return STRICT_SINGLE_ITEM_CODEC;
    }

    public static Codec<ItemStack> simpleItemCodec() {
        return SIMPLE_ITEM_CODEC;
    }
}
