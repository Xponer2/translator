/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.client;

import net.minecraft.client.renderer.GlStateManager;

/**
 * 1.21 {@code DynamicTexture}: a texture of a {@link NativeImage}, on the 1.12.2 dynamic texture (so the texture
 * manager and any 1.12.2 code take it). {@link #upload} copies the image into the 1.12.2 texture data and uploads it.
 */
public class DynamicTexture extends net.minecraft.client.renderer.texture.DynamicTexture implements AutoCloseable {
    private NativeImage pixels;

    public DynamicTexture(NativeImage pixels) {
        super(pixels.getWidth(), pixels.getHeight());
        this.pixels = pixels;
        this.upload();
    }

    public DynamicTexture(int width, int height, boolean useCalloc) {
        super(width, height);
        this.pixels = new NativeImage(width, height, useCalloc);
    }

    /** The image into the 1.12.2 texture (same size) and up to OpenGL. */
    public void upload() {
        if (this.pixels == null) {
            return;
        }
        int[] data = this.getTextureData();
        int[] argb = this.pixels.makePixelArray();
        System.arraycopy(argb, 0, data, 0, Math.min(data.length, argb.length));
        this.updateDynamicTexture();
    }

    public NativeImage getPixels() {
        return this.pixels;
    }

    public void setPixels(NativeImage pixels) {
        this.pixels = pixels;
    }

    public int getId() {
        return this.getGlTextureId();
    }

    public void bind() {
        GlStateManager.bindTexture(this.getGlTextureId());
    }

    /**
     * 1.21 {@code setFilter(blur, mipmap)}: of this texture, bound first as 1.21 does (1.12.2's setBlurMipmapDirect sets
     * the texture bound now: the title screen's logo got the blur of a mod's texture).
     */
    public void setFilter(boolean blur, boolean mipmap) {
        GlStateManager.bindTexture(this.getGlTextureId());
        this.setBlurMipmapDirect(blur, mipmap);
    }

    public void releaseId() {
        this.deleteGlTexture();
    }

    @Override
    public void close() {
        this.deleteGlTexture();
        if (this.pixels != null) {
            this.pixels.close();
            this.pixels = null;
        }
    }
}
