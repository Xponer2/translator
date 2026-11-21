/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.provided;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * NeoForge 21.1's {@code RegisterMaterialAtlasesEvent} (a P row): an atlas of the mod's own textures and its
 * definition, put next to the game's (chests, signs, banners...). Posted when the model manager is made
 * (mixin.ModelManagerAtlasesMixin), before its atlases are.
 */
public class RegisterMaterialAtlasesEvent extends Event implements IModBusEvent {
    private final Map<ResourceLocation, ResourceLocation> atlases;

    public RegisterMaterialAtlasesEvent(Map<ResourceLocation, ResourceLocation> atlases) {
        this.atlases = atlases;
    }

    public void register(ResourceLocation atlasLocation, ResourceLocation atlasInfoLocation) {
        ResourceLocation old = this.atlases.putIfAbsent(atlasLocation, atlasInfoLocation);
        if (old != null) {
            throw new IllegalStateException(String.format("Duplicate registration of atlas: %s (old info: %s, new info: %s)", atlasLocation, old,
                    atlasInfoLocation));
        }
    }
}
