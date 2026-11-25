/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.client;

import java.util.function.BiFunction;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * NeoForge 21's model locations on 1.20.6: 1.21's ModelResourceLocation is a record of an id and a variant, and an
 * extra model is a "standalone" one; 1.20.6's is a ResourceLocation with a variant, and an extra model is registered and
 * kept under its plain ResourceLocation. A standalone location is that plain id below.
 */
public final class ModelAdapters {
    static final String STANDALONE = "standalone";

    private ModelAdapters() {
    }

    /** 1.21's {@code location.id()} */
    public static ResourceLocation id(ModelResourceLocation location) {
        return new ResourceLocation(location.getNamespace(), location.getPath());
    }

    /** 1.21's {@code location.variant()} */
    public static String variant(ModelResourceLocation location) {
        return location.getVariant();
    }

    /** NeoForge 21's {@code RegisterAdditional.register(location)}: a standalone model by its plain id */
    public static void register(ModelEvent.RegisterAdditional event, ModelResourceLocation location) {
        event.register(STANDALONE.equals(location.getVariant()) ? id(location) : location);
    }

    /** 1.21's {@code models.getModel(location)}: a standalone model is kept under its plain id */
    public static BakedModel getModel(ModelManager models, ModelResourceLocation location) {
        return STANDALONE.equals(location.getVariant()) ? models.getModel(id(location)) : models.getModel(location);
    }

    /** 1.21's sprite getter of ModelBakery.bakeModels (an A row's plan), over 1.20.6's function of a location and a material */
    public static ladder.rung1206.provided.TextureGetter textureGetter(
            BiFunction<ResourceLocation, Material, TextureAtlasSprite> sprites) {
        return sprites::apply;
    }

    /** the model location 1.20.6's bake takes and 1.21's doesn't (a D row's default: 1.21's callers name none) */
    public static ResourceLocation unknownModel() {
        return UNKNOWN;
    }

    private static final ResourceLocation UNKNOWN = new ResourceLocation("ladder", "unknown_model");

    /** NeoForge 21's {@code baker.bakeUncached(model, state, sprites)}: the model baked as it is (1.20.6 takes a location) */
    public static BakedModel bakeUncached(net.neoforged.neoforge.client.extensions.IModelBakerExtension baker,
            net.minecraft.client.resources.model.UnbakedModel model, net.minecraft.client.resources.model.ModelState state,
            java.util.function.Function<Material, TextureAtlasSprite> sprites) {
        return model.bake((net.minecraft.client.resources.model.ModelBaker) baker, sprites, state, UNKNOWN);
    }

    /** 1.21's package-private {@code bakeUncached(model, state)}: with the baker's own sprites */
    public static BakedModel bakeUncached(net.neoforged.neoforge.client.extensions.IModelBakerExtension baker,
            net.minecraft.client.resources.model.UnbakedModel model, net.minecraft.client.resources.model.ModelState state) {
        return bakeUncached(baker, model, state, baker.getModelTextureGetter());
    }

    /** NeoForge 21's {@code baker.getTopLevelModel(location)}: 1.20.6's baker finds it by its location */
    public static net.minecraft.client.resources.model.UnbakedModel getTopLevelModel(net.neoforged.neoforge.client.extensions.IModelBakerExtension baker,
            ModelResourceLocation location) {
        return ((net.minecraft.client.resources.model.ModelBaker) baker).getModel(location);
    }

    /** 1.21's {@code ModelBakery.MISSING_MODEL_VARIANT} */
    public static ModelResourceLocation missingModelVariant() {
        return net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_LOCATION;
    }

    /** 1.21's {@code Sheets.createDecoratedPotMaterial(id)}: a pot texture of the decorated pot sheet */
    public static Material createDecoratedPotMaterial(ResourceLocation id) {
        return new Material(net.minecraft.client.renderer.Sheets.DECORATED_POT_SHEET, id.withPrefix("entity/decorated_pot/"));
    }

    /** 1.21's {@code Sheets.DECORATED_POT_BASE} */
    public static Material decoratedPotBase() {
        return createDecoratedPotMaterial(new ResourceLocation("minecraft", "decorated_pot_base"));
    }

    /** 1.21's {@code Sheets.DECORATED_POT_SIDE} */
    public static Material decoratedPotSide() {
        return createDecoratedPotMaterial(new ResourceLocation("minecraft", "decorated_pot_side"));
    }

    // 1.21's model bakery baker (public in NeoForge 21; 1.20.6's made public by the adapters' access transformer)

    /** 1.21's {@code bakery.new ModelBakerImpl(sprites, location)}: 1.20.6's takes the sprites by model id */
    public static net.minecraft.client.resources.model.ModelBakery.ModelBakerImpl modelBaker(net.minecraft.client.resources.model.ModelBakery bakery,
            ladder.rung1206.provided.TextureGetter sprites, ModelResourceLocation location) {
        return bakery.new ModelBakerImpl((id, material) -> sprites.get(new ModelResourceLocation(id, location.getVariant()), material), location);
    }

    public static BakedModel bakeUncached(net.minecraft.client.resources.model.ModelBakery.ModelBakerImpl baker,
            net.minecraft.client.resources.model.UnbakedModel model, net.minecraft.client.resources.model.ModelState state) {
        return bakeUncached((net.neoforged.neoforge.client.extensions.IModelBakerExtension) baker, model, state);
    }

    public static BakedModel bakeUncached(net.minecraft.client.resources.model.ModelBakery.ModelBakerImpl baker,
            net.minecraft.client.resources.model.UnbakedModel model, net.minecraft.client.resources.model.ModelState state,
            java.util.function.Function<Material, TextureAtlasSprite> sprites) {
        return bakeUncached((net.neoforged.neoforge.client.extensions.IModelBakerExtension) baker, model, state, sprites);
    }

    public static net.minecraft.client.resources.model.UnbakedModel getTopLevelModel(net.minecraft.client.resources.model.ModelBakery.ModelBakerImpl baker,
            ModelResourceLocation location) {
        return getTopLevelModel((net.neoforged.neoforge.client.extensions.IModelBakerExtension) baker, location);
    }

    /** 1.21's {@code format.contains(element)} */
    public static boolean contains(com.mojang.blaze3d.vertex.VertexFormat format, com.mojang.blaze3d.vertex.VertexFormatElement element) {
        return format.getElements().contains(element);
    }
}
