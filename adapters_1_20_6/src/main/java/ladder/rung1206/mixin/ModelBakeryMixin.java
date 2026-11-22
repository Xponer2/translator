/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.21's model registration on 1.20.6's bakery (the members are added to the class, as 1.21 has them): a top-level
 * model under its location ({@code registerModel}: 1.20.6 keeps it in the unbaked cache too, which the baking reads),
 * with its dependencies loaded first, a special item model and an item model by their location, and the missing model.
 * Mods call them from their own mixins into the bakery (extra item models).
 */
@Mixin(value = ModelBakery.class, priority = 500)
abstract class ModelBakeryMixin {
    @Shadow
    @Final
    private Map<ResourceLocation, UnbakedModel> unbakedCache;
    @Shadow
    @Final
    private Map<ResourceLocation, UnbakedModel> topLevelModels;

    /** 1.21's missing model (1.20.6 keeps it in the unbaked cache under its location) */
    private UnbakedModel missingModel;

    @Shadow
    public abstract UnbakedModel getModel(ResourceLocation location);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ladder$missingModel(CallbackInfo ci) {
        this.missingModel = this.unbakedCache.get(ModelBakery.MISSING_MODEL_LOCATION);
    }

    private void registerModel(ModelResourceLocation location, UnbakedModel model) {
        this.unbakedCache.put(location, model);
        this.topLevelModels.put(location, model);
    }

    private void registerModelAndLoadDependencies(ModelResourceLocation location, UnbakedModel model) {
        for (ResourceLocation dependency : model.getDependencies()) {
            this.getModel(dependency);
        }
        this.registerModel(location, model);
    }

    private void loadSpecialItemModelAndDependencies(ModelResourceLocation location) {
        ResourceLocation item = new ResourceLocation(location.getNamespace(), "item/" + location.getPath());
        this.registerModelAndLoadDependencies(location, this.getModel(item));
    }

    private void loadItemModelAndDependencies(ResourceLocation id) {
        ModelResourceLocation location = new ModelResourceLocation(id, "inventory");
        ResourceLocation item = new ResourceLocation(id.getNamespace(), "item/" + id.getPath());
        this.registerModelAndLoadDependencies(location, this.getModel(item));
    }
}
