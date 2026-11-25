/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * 1.21's render types of a dying dragon's rays (1.20.6 draws them as lightning): made as 1.21 makes them, on first use
 * (only mods ask for them; 1.20.6's renderer keeps drawing its rays with lightning()).
 */
public final class DragonRayTypes {
    private DragonRayTypes() {
    }

    private static final class Made {
        static final RenderType RAYS = RenderType.create("dragon_rays", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 1536,
                false, false, RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE).setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                        .createCompositeState(false));
        static final RenderType RAYS_DEPTH = RenderType.create("dragon_rays_depth", DefaultVertexFormat.POSITION, VertexFormat.Mode.TRIANGLES, 1536,
                false, false, RenderType.CompositeState.builder().setShaderState(RenderStateShard.POSITION_SHADER)
                        .setWriteMaskState(RenderStateShard.DEPTH_WRITE).createCompositeState(false));
    }

    /** 1.21's {@code RenderType.dragonRays()} */
    public static RenderType dragonRays() {
        return Made.RAYS;
    }

    /** 1.21's {@code RenderType.dragonRaysDepth()} */
    public static RenderType dragonRaysDepth() {
        return Made.RAYS_DEPTH;
    }
}
