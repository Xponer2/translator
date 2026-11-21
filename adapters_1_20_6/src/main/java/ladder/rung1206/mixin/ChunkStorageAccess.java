/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

/** A chunk storage's region info, which 1.21 passes to ChunkSerializer.read (ChunkArgs). */
@Mixin(ChunkStorage.class)
public interface ChunkStorageAccess {
    @Invoker("storageInfo")
    RegionStorageInfo ladder$storageInfo();
}
