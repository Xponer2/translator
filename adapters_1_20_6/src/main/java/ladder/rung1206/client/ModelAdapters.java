/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.client;

import net.minecraft.client.resources.model.BakedModel;
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
}
