/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.provided;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.VillagerType;

/** NeoForge 21.1's {@code BiomeVillagerType} (a P row): the villager type of a biome, a value of the villager types data map. */
public record BiomeVillagerType(VillagerType type) {
    public static final Codec<BiomeVillagerType> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(BuiltInRegistries.VILLAGER_TYPE.byNameCodec().fieldOf("villager_type").forGetter(BiomeVillagerType::type))
            .apply(instance, BiomeVillagerType::new));
}
