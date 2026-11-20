/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.client;

import java.util.Map;

import com.mojang.datafixers.util.Pair;

import ladder.rung121.client.DimensionTransitionScreenManager.ReceivingLevelScreenFactory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/** NeoForge 21.1's RegisterDimensionTransitionScreenEvent (a P row), on the mod bus: a mod's screens for its dimensions. */
public class RegisterDimensionTransitionScreenEvent extends Event implements IModBusEvent {
    private final Map<Pair<ResourceKey<Level>, ResourceKey<Level>>, ReceivingLevelScreenFactory> conditional;
    private final Map<ResourceKey<Level>, ReceivingLevelScreenFactory> to;
    private final Map<ResourceKey<Level>, ReceivingLevelScreenFactory> from;

    RegisterDimensionTransitionScreenEvent(Map<Pair<ResourceKey<Level>, ResourceKey<Level>>, ReceivingLevelScreenFactory> conditional,
            Map<ResourceKey<Level>, ReceivingLevelScreenFactory> to, Map<ResourceKey<Level>, ReceivingLevelScreenFactory> from) {
        this.conditional = conditional;
        this.to = to;
        this.from = from;
    }

    public boolean registerIncomingEffect(ResourceKey<Level> dimension, ReceivingLevelScreenFactory screen) {
        return this.to.putIfAbsent(dimension, screen) == null;
    }

    public boolean registerOutgoingEffect(ResourceKey<Level> dimension, ReceivingLevelScreenFactory screen) {
        return this.from.putIfAbsent(dimension, screen) == null;
    }

    public boolean registerConditionalEffect(ResourceKey<Level> toDimension, ResourceKey<Level> fromDimension, ReceivingLevelScreenFactory screen) {
        return this.conditional.putIfAbsent(Pair.of(toDimension, fromDimension), screen) == null;
    }
}
