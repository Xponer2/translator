/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.render;

import ladder.carried.net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.world.level.block.SkullBlock;

/** SkullBlockRenderer.getRenderType(type, profile): 1.20.4 takes the GameProfile the ResolvableProfile holds. */
public final class SkullRenderAdapters {
    private SkullRenderAdapters() {
    }

    public static RenderType getRenderType(SkullBlock.Type type, ResolvableProfile profile) {
        return SkullBlockRenderer.getRenderType(type, profile == null ? null : profile.gameProfile());
    }
}
