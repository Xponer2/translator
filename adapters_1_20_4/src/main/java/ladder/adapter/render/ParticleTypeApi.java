/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;

import net.minecraft.client.renderer.texture.TextureManager;

/**
 * 1.21's {@code ParticleRenderType.begin(tesselator, textures)} (a batch of the type's particles begun, its builder
 * given back; the engine draws it): a converted mod's render type gets this interface (an O row). 1.20.4's engine begins
 * a type with the tesselator's builder and ends it with the tesselator (mixin.ParticleEngineMixin calls this, then draws).
 */
public interface ParticleTypeApi {
    BufferBuilder begin(Tesselator tesselator, TextureManager textures);

    /** the game's own (a super call): never reached, 1.21's types are interfaces */
    static BufferBuilder begin(net.minecraft.client.particle.ParticleRenderType self, Tesselator tesselator, TextureManager textures) {
        throw new UnsupportedOperationException("ParticleRenderType.begin has no game implementation");
    }
}
