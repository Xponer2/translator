/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import ladder.rung121.provided.BiomeVillagerType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * NeoForge 21.1's villager types data map (neoforge:villager_types, a biome's villager type): registered by the
 * adapters, and asked first where 21.0 picks a biome's villager type (mixin.VillagerTypeMixin).
 */
public final class VillagerTypes {
    public static final DataMapType<Biome, BiomeVillagerType> VILLAGER_TYPES = DataMapType
            .builder(ResourceLocation.fromNamespaceAndPath("neoforge", "villager_types"), Registries.BIOME, BiomeVillagerType.CODEC)
            .synced(BiomeVillagerType.CODEC, false).build();

    private VillagerTypes() {
    }

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(VILLAGER_TYPES);
    }

    /** {@code NeoForgeDataMaps.VILLAGER_TYPES} */
    public static DataMapType<Biome, BiomeVillagerType> villagerTypes() {
        return VILLAGER_TYPES;
    }
}
