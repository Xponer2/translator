/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import ladder.rung121.provided.RegisterMaterialAtlasesEvent;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoader;

/** The model manager's atlases are the game's and those the mods register (NeoForge 21.1's RegisterMaterialAtlasesEvent). */
@Mixin(ModelManager.class)
abstract class ModelManagerAtlasesMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/AtlasSet;<init>(Ljava/util/Map;Lnet/minecraft/client/renderer/texture/TextureManager;)V"), index = 0)
    private Map<ResourceLocation, ResourceLocation> ladder$modAtlases(Map<ResourceLocation, ResourceLocation> vanilla) {
        Map<ResourceLocation, ResourceLocation> atlases = new HashMap<>(vanilla);
        ModLoader.postEvent(new RegisterMaterialAtlasesEvent(atlases));
        return Map.copyOf(atlases);
    }
}
