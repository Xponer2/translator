/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;

/** The bee's hive position, package-private in 1.20.4, for 1.20.5's {@code Bee.setHivePos} (BeeAdapters). */
@Mixin(Bee.class)
public interface BeeAccess {
    @Accessor("hivePos")
    void ladder$setHivePos(BlockPos pos);
}
