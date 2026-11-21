/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.world.phys.Vec3;

/** The position a tracker's deltas are relative to (1.21's VecDeltaCodec.getBase). */
@Mixin(VecDeltaCodec.class)
public interface VecDeltaCodecAccess {
    @Accessor("base")
    Vec3 ladder$base();
}
