/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.loot;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ladder.adapter.component.ComponentPatches;
import ladder.carried.net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 1.20.5's {@code minecraft:set_components} loot function on 1.20.4 ({@code ladder:set_components}, the converter
 * renames it in a mod's loot tables): the patch (values set, {@code !type} removed) applied to the stack as 1.21's
 * {@code ItemStack.applyComponentsAndValidate} does, through the component store.
 */
public final class SetComponentsFunction extends LootItemConditionalFunction {
    public static final Codec<SetComponentsFunction> CODEC = RecordCodecBuilder.create(i -> commonFields(i)
            .and(DataComponentPatch.CODEC.fieldOf("components").forGetter(f -> f.components))
            .apply(i, SetComponentsFunction::new));

    private static final DeferredRegister<LootItemFunctionType> TYPES = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, "ladder");
    public static final DeferredHolder<LootItemFunctionType, LootItemFunctionType> TYPE = TYPES.register("set_components",
            () -> new LootItemFunctionType(CODEC));

    private final DataComponentPatch components;

    private SetComponentsFunction(List<LootItemCondition> conditions, DataComponentPatch components) {
        super(conditions);
        this.components = components;
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }

    @Override
    public LootItemFunctionType getType() {
        return TYPE.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        ComponentPatches.applyComponentsAndValidate(stack, this.components);
        return stack;
    }
}
