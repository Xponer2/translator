/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ladder.carried.net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.20.5's codecs of stacks and component maps over the store: a stack is {id, count, components} (its components
 * the patch over its item's: ComponentPatches), as 1.21's data files and commands write it.
 */
public final class ComponentCodecs {
    /** component id -> value: a patch of values only (1.21's map codec writes the same map) */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static final Codec<DataComponentMap> MAP_CODEC = DataComponentPatch.CODEC.flatXmap(patch -> {
        Map<DataComponentType<?>, Object> values = new LinkedHashMap<>();
        for (Map.Entry<DataComponentType<?>, java.util.Optional<?>> entry : patch.entrySet()) {
            if (entry.getValue().isEmpty()) {
                return DataResult.error(() -> "A component map can't remove " + entry.getKey());
            }
            values.put(entry.getKey(), entry.getValue().get());
        }
        return DataResult.success((DataComponentMap) new DataComponentMap.Snapshot(Map.copyOf(values)));
    }, components -> {
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        for (TypedDataComponent component : components) {
            if (!component.type().isTransient()) {
                builder.set(component.type(), component.value());
            }
        }
        return DataResult.success(builder.build());
    });

    /** 1.20.5's {@code ItemStack.CODEC}: a stack that isn't empty */
    public static final Codec<ItemStack> ITEM_STACK = RecordCodecBuilder.<ItemStack>create(i -> i.group(
            BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id").forGetter(ItemStack::getItemHolder),
            Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(ItemStack::getCount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ComponentPatches::getComponentsPatch))
            .apply(i, ComponentPatches::create)).flatXmap(ComponentCodecs::notEmpty, ComponentCodecs::notEmpty);

    private static DataResult<ItemStack> notEmpty(ItemStack stack) {
        return stack.isEmpty() ? DataResult.error(() -> "Empty item stack") : DataResult.success(stack);
    }

    /** 1.20.5's {@code ItemStack.OPTIONAL_CODEC}: an empty map is the empty stack */
    public static final Codec<ItemStack> OPTIONAL_ITEM_STACK = Codec.of(new Encoder<ItemStack>() {
        @Override
        public <T> DataResult<T> encode(ItemStack stack, DynamicOps<T> ops, T prefix) {
            return stack.isEmpty() ? DataResult.success(ops.emptyMap()) : ITEM_STACK.encode(stack, ops, prefix);
        }
    }, new Decoder<ItemStack>() {
        @Override
        public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
            boolean empty = ops.getMap(input).result().map(map -> map.entries().findAny().isEmpty()).orElse(false);
            return empty ? DataResult.success(Pair.of(ItemStack.EMPTY, input)) : ITEM_STACK.decode(ops, input);
        }
    });

    private ComponentCodecs() {
    }

    public static Codec<ItemStack> itemStack() {
        return ITEM_STACK;
    }

    public static Codec<ItemStack> optionalItemStack() {
        return OPTIONAL_ITEM_STACK;
    }

    /** 1.20.5's {@code ItemStack.consume(amount, entity)}: shrunk unless the entity has infinite materials (creative) */
    public static void consume(ItemStack stack, int amount, net.minecraft.world.entity.LivingEntity entity) {
        if (entity == null || !(entity instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().instabuild)) {
            stack.shrink(amount);
        }
    }

    /** 1.20.5's {@code ItemStack.consumeAndReturn(amount, entity)}: the consumed part as its own stack */
    public static ItemStack consumeAndReturn(ItemStack stack, int amount, net.minecraft.world.entity.LivingEntity entity) {
        ItemStack part = stack.copyWithCount(amount);
        consume(stack, amount, entity);
        return part;
    }

    /** 1.20.5's {@code ItemStack.getEnchantments()}: its enchantments component */
    public static Object getEnchantments(ItemStack stack) {
        return ComponentStore.getOrDefault((DataComponentHolder) (Object) stack, DataComponents.ENCHANTMENTS,
                ladder.carried.net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
    }
}
