/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import ladder.rung1206.shape.AddedTo;

/**
 * The members an adapters' mixin adds to the model baker (ModelBakerImplMixin), for the other adapters' calls: the build
 * turns a cast to this class and its calls into ModelBakery$ModelBakerImpl's own. Never instantiated.
 */
@AddedTo("net/minecraft/client/resources/model/ModelBakery$ModelBakerImpl")
public abstract class ModelBakerImplAdded {
    private ModelBakerImplAdded() {
    }

    /** 1.21's {@code bakeUncached(model, state)} */
    public abstract BakedModel bakeUncached(UnbakedModel model, ModelState state);

    /** the next bakeUncached bakes this top-level location's model: 1.20.6's baking takes it, and caches as bake does */
    public abstract void ladder$topLevel(ResourceLocation location);
}
