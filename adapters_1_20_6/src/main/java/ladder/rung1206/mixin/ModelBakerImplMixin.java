/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import ladder.rung1206.client.ModelAdapters;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.21's model baker bakes a model in bakeUncached (the model as it is, no location), which bake calls after the
 * cache; 1.20.6's bakes inside bake. The baker gets 1.21's bakeUncached (the mods' mixins aim at it: fusion's own
 * models) and its bake makes that call; the location 1.20.6's baking takes is the one bake was asked for (a direct
 * call of bakeUncached has none: a placeholder). 1.20.6's caching stays (a generated item model isn't cached).
 */
@Mixin(value = ModelBakery.ModelBakerImpl.class, priority = 500)
abstract class ModelBakerImplMixin {
    @Shadow
    @Final
    private Function<Material, TextureAtlasSprite> modelTextureGetter;

    @Shadow
    @Final
    ModelBakery this$0;

    /** the location bake was asked for (1.20.6's baking takes it; 1.21's bakeUncached hasn't) */
    @Unique
    private ResourceLocation ladder$location;

    @Shadow
    public abstract UnbakedModel getModel(ResourceLocation location);

    // an overwrite, not an injector: the mods' points are 1.21's bakeUncached call here (fabric's model loading)
    /**
     * @author ladder
     * @reason 1.21's bake: the cache, then bakeUncached of the location's model (1.20.6's baking, its caching)
     */
    @Overwrite
    public BakedModel bake(ResourceLocation location, ModelState state, Function<Material, TextureAtlasSprite> sprites) {
        ModelBakery.BakedCacheKey key = new ModelBakery.BakedCacheKey(location, state.getRotation(), state.isUvLocked());
        BakedModel cached = this.this$0.bakedCache.get(key);
        if (cached != null) {
            return cached;
        }
        UnbakedModel unbaked = this.getModel(location);
        ResourceLocation outer = this.ladder$location;
        this.ladder$location = location;
        BakedModel baked;
        try {
            baked = this.bakeUncached(unbaked, state, sprites);
        } finally {
            this.ladder$location = outer;
        }
        if (!(unbaked instanceof BlockModel block && block.getRootModel() == ModelBakery.GENERATION_MARKER)) {
            this.this$0.bakedCache.put(key, baked);
        }
        return baked;
    }

    /** 1.21's bakeUncached(model, state): with the baker's own sprites */
    public BakedModel bakeUncached(UnbakedModel model, ModelState state) {
        return this.bakeUncached(model, state, this.modelTextureGetter);
    }

    /** 1.21's bakeUncached(model, state, sprites): 1.20.6's baking of a model (a generated item model, or the model's own) */
    public BakedModel bakeUncached(UnbakedModel model, ModelState state, Function<Material, TextureAtlasSprite> sprites) {
        // (taken once: a model's own direct call of bakeUncached while it bakes has no location of its own)
        ResourceLocation location = this.ladder$location != null ? this.ladder$location : ModelAdapters.unknownModel();
        this.ladder$location = null;
        ModelBaker baker = (ModelBaker) (Object) this;
        if (model instanceof BlockModel block && block.getRootModel() == ModelBakery.GENERATION_MARKER) {
            return ModelBakery.ITEM_MODEL_GENERATOR.generateBlockModel(sprites, block).bake(baker, block, sprites, state, location, false);
        }
        return model.bake(baker, sprites, state, location);
    }
}
