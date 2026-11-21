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
    public static ladder.carried_1_20_6.net.minecraft.client.resources.model.ModelBakery$TextureGetter textureGetter(
            BiFunction<ResourceLocation, Material, TextureAtlasSprite> sprites) {
        return sprites::apply;
    }

    /** the model location 1.20.6's bake takes and 1.21's doesn't (a D row's default: 1.21's callers name none) */
    public static ResourceLocation unknownModel() {
        return UNKNOWN;
    }

    private static final ResourceLocation UNKNOWN = new ResourceLocation("ladder", "unknown_model");
}
