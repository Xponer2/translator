/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testa;

import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.level.block.Blocks;

/** Small members of 1.20.5 / 1.21: holders' names and equality, a registry's holder by location, food, state values. */
final class SmallApiTest {
    private SmallApiTest() {
    }

    static void run(Map<String, String> results) {
        results.put("registeredName", MobEffects.REGENERATION.getRegisteredName());
        results.put("holderIsSame", String.valueOf(MobEffects.REGENERATION.is(MobEffects.REGENERATION)));
        results.put("holderIsOther", String.valueOf(MobEffects.REGENERATION.is(MobEffects.POISON)));
        results.put("holderByLocation", BuiltInRegistries.ITEM.getHolder(ResourceLocation.withDefaultNamespace("apple"))
                .map(h -> h.getRegisteredName()).orElse("none"));
        FoodProperties apple = Foods.APPLE;
        results.put("foodNutrition", String.valueOf(apple.nutrition()));
        results.put("foodSaturation", String.valueOf(apple.saturation()));
        results.put("foodEatSeconds", String.valueOf(apple.eatSeconds()));
        results.put("stateValues", String.valueOf(Blocks.OAK_STAIRS.defaultBlockState().getValues().size()));
    }

    /** a block entity's own save and load through the registries (overrides with super calls, the entries) */
    static void blockEntity(net.minecraft.server.level.ServerLevel level, Map<String, String> results) {
        ProbeChest chest = new ProbeChest(net.minecraft.core.BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        net.minecraft.nbt.CompoundTag tag = chest.saveWithoutMetadata(level.registryAccess());
        results.put("beSavedKeys", String.valueOf(new java.util.TreeSet<>(tag.getAllKeys())));
        results.put("beSavedValue", tag.getString("ladder"));
        ProbeChest other = new ProbeChest(net.minecraft.core.BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        other.loadWithComponents(tag, level.registryAccess());
        results.put("beLoaded", other.loaded);
        results.put("beCustomKeys", String.valueOf(new java.util.TreeSet<>(chest.saveCustomOnly(level.registryAccess()).getAllKeys())));
        // a tag with block entity components: loaded and saved again, they are still there
        net.minecraft.nbt.CompoundTag withComponents = tag.copy();
        net.minecraft.nbt.CompoundTag components = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.CompoundTag custom = new net.minecraft.nbt.CompoundTag();
        custom.putString("ladderComponentValue", "kept");
        components.put("minecraft:custom_data", custom);
        withComponents.put("components", components);
        ProbeChest third = new ProbeChest(net.minecraft.core.BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        third.loadWithComponents(withComponents, level.registryAccess());
        results.put("beComponentsKept", String.valueOf(third.saveWithoutMetadata(level.registryAccess()).toString().contains("ladderComponentValue")));
    }
}
