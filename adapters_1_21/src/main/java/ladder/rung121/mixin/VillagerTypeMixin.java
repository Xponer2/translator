/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ladder.rung121.VillagerTypes;
import ladder.rung121.provided.BiomeVillagerType;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.biome.Biome;

/** A biome's villager type from NeoForge 21.1's data map first (a mod's biome names its type there), then 21.0's own table. */
@Mixin(VillagerType.class)
abstract class VillagerTypeMixin {
    @Inject(method = "byBiome", at = @At("HEAD"), cancellable = true)
    private static void ladder$dataMap(Holder<Biome> biome, CallbackInfoReturnable<VillagerType> cir) {
        BiomeVillagerType own = biome.getData(VillagerTypes.VILLAGER_TYPES);
        if (own != null) {
            cir.setReturnValue(own.type());
        }
    }
}
