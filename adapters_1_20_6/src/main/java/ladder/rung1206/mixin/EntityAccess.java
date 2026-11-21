/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/** The entity's own random, for 1.21's {@code Entity.getRandom()} (MiscAdapters). */
@Mixin(Entity.class)
public interface EntityAccess {
    @Accessor("random")
    RandomSource ladder$random();
}
