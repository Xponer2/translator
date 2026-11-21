/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.RenderType;

/** A render type's sortOnUpload, which 1.21 makes public (RenderTypes). */
@Mixin(RenderType.class)
public interface RenderTypeAccess {
    @Accessor("sortOnUpload")
    boolean ladder$sortOnUpload();
}
