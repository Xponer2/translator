/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;

/**
 * 1.21's {@code ModelBakery.TextureGetter} (a P row): a model's sprite of a material. The adapters' own (not carried), so
 * the bakery's mixin may take it: the pack carries 1.21's classes only when its mods name them.
 */
@FunctionalInterface
public interface TextureGetter {
    TextureAtlasSprite get(ModelResourceLocation location, Material material);
}
