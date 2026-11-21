/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided.event;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * NeoForge 21's {@code BlockEntityTypeAddBlocksEvent} (a P row): more blocks a block entity type is valid for (a mod's
 * chest variant for the chest's type...). Posted at the common setup, before the worlds load.
 */
public class BlockEntityTypeAddBlocksEvent extends Event implements IModBusEvent {
    private static Field validBlocks;

    public BlockEntityTypeAddBlocksEvent() {
    }

    public void modify(BlockEntityType<?> type, Block... blocks) {
        try {
            if (validBlocks == null) {
                Field f = BlockEntityType.class.getDeclaredField("validBlocks");
                f.setAccessible(true);
                validBlocks = f;
            }
            @SuppressWarnings("unchecked")
            Set<Block> set = new HashSet<>((Set<Block>) validBlocks.get(type));
            java.util.Collections.addAll(set, blocks);
            validBlocks.set(type, set);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("can't add blocks to the block entity type " + type, e);
        }
    }

    /** the adapters' listener: the event, once, at the common setup */
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ModLoader.postEvent(new BlockEntityTypeAddBlocksEvent()));
    }
}
