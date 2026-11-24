/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/** another renderer's block light at a position (the leash's far end: its holder's renderer) */
@Mixin(EntityRenderer.class)
public interface EntityRendererLightAccess {
    @Invoker("getBlockLightLevel")
    int ladder$blockLightLevel(Entity entity, BlockPos pos);
}
