/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import java.util.Map;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import ladder.rung1206.client.ModelAdapters;
import ladder.rung1206.client.ModelBakerImplAdded;
import ladder.rung1206.provided.TextureGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.21's {@code ModelBakery.bakeModels}: each top-level model is baked in its own method (1.21's lambda$bakeModels$6 of
 * the sprites, the model's location and the model) by a new baker's bakeUncached; the mods' mixins aim at that call
 * (athena's connected textures, fabric's model loading). 1.20.6 bakes each by its location (bake: the cache, then the
 * model): the baker takes the location over and caches as bake did, a model baked already comes from the cache, and a
 * plain (not a model resource) location is baked as 1.20.6 does.
 */
@Mixin(value = ModelBakery.class, priority = 500)
abstract class ModelBakeryBakeMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private Map<ResourceLocation, UnbakedModel> topLevelModels;

    @Shadow
    @Final
    private Map<ResourceLocation, BakedModel> bakedTopLevelModels;

    /** the sprites of the bake in progress (1.21's lambda takes them as its texture getter) */
    private BiFunction<ResourceLocation, Material, TextureAtlasSprite> ladder$sprites;

    // an overwrite, not an injector: the mods' points are in 1.21's per-model method, which this calls
    /**
     * @author ladder
     * @reason 1.21's shape of baking the top-level models (one method per model, a baker's bakeUncached), 1.20.6's baking
     */
    @Overwrite
    public void bakeModels(BiFunction<ResourceLocation, Material, TextureAtlasSprite> sprites) {
        this.ladder$sprites = sprites;
        TextureGetter getter = ModelAdapters.textureGetter(sprites);
        // (1.20.6's order, its keys; the model is the one 1.20.6's bake gets for the location, a failure to get it warned as
        // 1.20.6 warns a failed bake)
        this.topLevelModels.keySet().forEach(location -> {
            if (location instanceof ModelResourceLocation modelLocation) {
                UnbakedModel model;
                try {
                    model = ((ModelBakery) (Object) this).getModel(location);
                } catch (Exception exception) {
                    LOGGER.warn("Unable to bake model: '{}': {}", location, exception);
                    return;
                }
                this.lambda$bakeModels$6(getter, modelLocation, model);
            } else {
                this.ladder$bakeByLocation(sprites, location);
            }
        });
    }

    /** 1.21's lambda$bakeModels$6: a top-level model baked by a new baker of its location */
    private void lambda$bakeModels$6(TextureGetter getter, ModelResourceLocation location, UnbakedModel model) {
        BakedModel baked = null;
        try {
            ModelBakery.BakedCacheKey key = new ModelBakery.BakedCacheKey(location, BlockModelRotation.X0_Y0.getRotation(),
                    BlockModelRotation.X0_Y0.isUvLocked());
            baked = ((ModelBakery) (Object) this).bakedCache.get(key);
            if (baked == null) {
                ModelBakery.ModelBakerImpl baker = ((ModelBakery) (Object) this).new ModelBakerImpl(this.ladder$sprites, location);
                ((ModelBakerImplAdded) (Object) baker).ladder$topLevel(location);
                baked = ((ModelBakerImplAdded) (Object) baker).bakeUncached(model, BlockModelRotation.X0_Y0);
            }
        } catch (Exception exception) {
            LOGGER.warn("Unable to bake model: '{}': {}", location, exception);
        }
        if (baked != null) {
            this.bakedTopLevelModels.put(location, baked);
        }
    }

    /** 1.20.6's baking of a top-level model by its location (a plain location: no 1.21 model resource location) */
    private void ladder$bakeByLocation(BiFunction<ResourceLocation, Material, TextureAtlasSprite> sprites, ResourceLocation location) {
        BakedModel baked = null;
        try {
            baked = ((ModelBakery) (Object) this).new ModelBakerImpl(sprites, location).bake(location, BlockModelRotation.X0_Y0);
        } catch (Exception exception) {
            LOGGER.warn("Unable to bake model: '{}': {}", location, exception);
        }
        if (baked != null) {
            this.bakedTopLevelModels.put(location, baked);
        }
    }
}
