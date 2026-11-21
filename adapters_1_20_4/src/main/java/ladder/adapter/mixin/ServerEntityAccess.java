/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.phys.Vec3;

/** What the tracker last sent (1.21's getPositionBase, getLastSent*): its position codec, packed rotations, movement. */
@Mixin(ServerEntity.class)
public interface ServerEntityAccess {
    @Accessor("positionCodec")
    VecDeltaCodec ladder$positionCodec();

    @Accessor("yRotp")
    int ladder$yRot();

    @Accessor("xRotp")
    int ladder$xRot();

    @Accessor("yHeadRotp")
    int ladder$yHeadRot();

    @Accessor("ap")
    Vec3 ladder$movement();
}
