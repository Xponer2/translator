/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;

/** the chunk map's tracked entities (an entity's attachment updates go to the players that track it) */
@Mixin(ChunkMap.class)
public interface ChunkMapAccess {
    @Accessor("entityMap")
    Int2ObjectMap<?> ladder$entityMap();
}
