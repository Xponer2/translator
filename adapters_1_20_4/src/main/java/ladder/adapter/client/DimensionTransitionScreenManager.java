/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModLoader;

/**
 * NeoForge 21.1's DimensionTransitionScreenManager (a P row): the screen shown while a level loads, by the dimensions
 * left and entered (a mod's own for its dimension). The mods register theirs with RegisterDimensionTransitionScreenEvent,
 * posted the first time a screen is asked for (1.20.4 has no such event); ClientPacketListenerMixin asks where 1.20.4 makes
 * its ReceivingLevelScreen. The choice is 21.1's: the pair of dimensions, the one entered, the one left, else vanilla's.
 */
public final class DimensionTransitionScreenManager {
    private static final Map<Pair<ResourceKey<Level>, ResourceKey<Level>>, ReceivingLevelScreenFactory> CONDITIONAL = new HashMap<>();
    private static final Map<ResourceKey<Level>, ReceivingLevelScreenFactory> TO = new HashMap<>();
    private static final Map<ResourceKey<Level>, ReceivingLevelScreenFactory> FROM = new HashMap<>();
    private static boolean registered;

    private DimensionTransitionScreenManager() {
    }

    private static synchronized void register() {
        if (!registered) {
            registered = true;
            ModLoader.get().postEventWrapContainerInModOrder(new RegisterDimensionTransitionScreenEvent(CONDITIONAL, TO, FROM));
        }
    }

    /** {@code getScreenFromLevel(target, source)} */
    public static ReceivingLevelScreenFactory getScreenFromLevel(@Nullable Level target, @Nullable Level source) {
        if (source == null) {
            return getScreen(null, null);
        }
        return getScreen(target == null ? null : target.dimension(), source.dimension());
    }

    /** {@code getScreen(toDimension, fromDimension)} */
    public static ReceivingLevelScreenFactory getScreen(@Nullable ResourceKey<Level> to, @Nullable ResourceKey<Level> from) {
        register();
        ReceivingLevelScreenFactory screen = CONDITIONAL.get(Pair.of(to, from));
        if (screen == null) {
            screen = TO.get(to);
        }
        if (screen == null) {
            screen = FROM.get(from);
        }
        return screen != null ? screen : ladder.adapter.client.TransitionScreens::receivingLevelScreen;
    }

    /** 21.1's ReceivingLevelScreenFactory */
    public interface ReceivingLevelScreenFactory {
        ReceivingLevelScreen create(BooleanSupplier ready, ladder.adapter.provided.ReceivingLevelReason reason);
    }
}
