/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.provided;

import java.util.Set;
import java.util.function.BiConsumer;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * NeoForge 21.1's {@code ExtendPoiTypesEvent} (a P row): more block states of an existing point of interest type (a
 * mod's block as a villager workstation...). Posted at the common setup (ladder.rung121.PoiExtensions).
 */
public class ExtendPoiTypesEvent extends Event implements IModBusEvent {
    private final BiConsumer<ResourceKey<PoiType>, Set<BlockState>> adder;

    public ExtendPoiTypesEvent(BiConsumer<ResourceKey<PoiType>, Set<BlockState>> adder) {
        this.adder = adder;
    }

    public void addBlockToPoi(ResourceKey<PoiType> poi, Block block) {
        this.addStatesToPoi(poi, Set.copyOf(block.getStateDefinition().getPossibleStates()));
    }

    public void addStatesToPoi(ResourceKey<PoiType> poi, Set<BlockState> states) {
        this.adder.accept(poi, states);
    }
}
