/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ladder.rung121.provided.ExtendPoiTypesEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * NeoForge 21.1's point of interest type extensions on 21.0: the added states are the type's in the game's state
 * table (PoiTypes.forState) and in its own check (PoiType.is, mixin.PoiTypeMixin).
 */
public final class PoiExtensions {
    /** a type -> the states the mods added to it */
    public static final Map<PoiType, Set<BlockState>> ADDED = new ConcurrentHashMap<>();

    private PoiExtensions() {
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ModLoader.postEvent(new ExtendPoiTypesEvent(PoiExtensions::add)));
    }

    private static void add(ResourceKey<PoiType> key, Set<BlockState> states) {
        Holder<PoiType> holder = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolderOrThrow(key);
        ADDED.computeIfAbsent(holder.value(), type -> ConcurrentHashMap.newKeySet()).addAll(states);
        try {
            Method register = PoiTypes.class.getDeclaredMethod("registerBlockStates", Holder.class, Set.class);
            register.setAccessible(true);
            register.invoke(null, holder, states);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("can't add block states to the point of interest type " + key, e);
        }
    }
}
