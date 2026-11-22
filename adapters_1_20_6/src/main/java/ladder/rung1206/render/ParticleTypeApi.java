/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;

import net.minecraft.client.renderer.texture.TextureManager;

/**
 * 1.21's {@code ParticleRenderType.begin(tesselator, textures)} (a batch of the type's particles begun, its builder
 * given back; the engine draws it): a converted mod's render type gets this interface (an O row). 1.20.6's engine begins
 * a type with the tesselator's builder and ends it with the tesselator (mixin.ParticleEngineMixin calls this, then draws).
 */
public interface ParticleTypeApi {
    BufferBuilder begin(Tesselator tesselator, TextureManager textures);

    /** the game's own (a super call): never reached, 1.21's types are interfaces */
    static BufferBuilder begin(net.minecraft.client.particle.ParticleRenderType self, Tesselator tesselator, TextureManager textures) {
        throw new UnsupportedOperationException("ParticleRenderType.begin has no game implementation");
    }

    /**
     * a call of 1.21's begin on any render type (an X row): a converted mod's begins as 1.21 does; the game's own begins
     * its batch in the tesselator's builder (1.20.6's way), the builder given back
     */
    static BufferBuilder call(net.minecraft.client.particle.ParticleRenderType type, Tesselator tesselator, TextureManager textures) {
        if (type instanceof ParticleTypeApi own) {
            return own.begin(tesselator, textures);
        }
        BufferBuilder builder = tesselator.getBuilder();
        type.begin(builder, textures);
        return builder;
    }
}
