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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.function.IntUnaryOperator;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * 1.21 {@code NativeImage} on the Java heap: 1.21 keeps the pixels in native memory and reads PNGs with LWJGL 3's
 * STB, neither of which 1.12.2 (LWJGL 2) has. Pixels are 1.21's RGBA ints (ABGR as a number: {@code getPixelRGBA}
 * gives 0xAABBGGRR); files go through ImageIO; upload / download use the texture bound in OpenGL, like 1.21.
 */
public class NativeImage implements AutoCloseable {
    private final Format format;
    private final int width;
    private final int height;
    private int[] data;
    /**
     * 1.21's native address of the pixels: mods check {@code image.pixels != 0} for an image that is still open
     * (JourneyMap's texture cache). Not 0 until {@link #close()}.
     */
    public long pixels = 1L;

    public NativeImage(int width, int height, boolean useCalloc) {
        this(Format.RGBA, width, height, useCalloc);
    }

    public NativeImage(Format format, int width, int height, boolean useCalloc) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid texture size: " + width + "x" + height);
        }
        this.format = format;
        this.width = width;
        this.height = height;
        this.data = new int[width * height];
    }

    // --- reading and writing files -------------------------------------------------------------------------------------

    public static NativeImage read(InputStream in) throws IOException {
        return read(Format.RGBA, in);
    }

    public static NativeImage read(Format format, InputStream in) throws IOException {
        BufferedImage image = ImageIO.read(in);
        if (image == null) {
            throw new IOException("Could not load image: not a supported image file");
        }
        var result = new NativeImage(format == null ? Format.RGBA : format, image.getWidth(), image.getHeight(), false);
        int[] argb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        for (int i = 0; i < argb.length; i++) {
            result.data[i] = swapRedBlue(argb[i]);
        }
        return result;
    }

    public static NativeImage read(byte[] bytes) throws IOException {
        return read(new ByteArrayInputStream(bytes));
    }

    public static NativeImage read(ByteBuffer buffer) throws IOException {
        var bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return read(bytes);
    }

    public static NativeImage read(Format format, ByteBuffer buffer) throws IOException {
        var bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return read(format, new ByteArrayInputStream(bytes));
    }

    /** The image as PNG bytes. */
    public byte[] asByteArray() throws IOException {
        var out = new ByteArrayOutputStream();
        ImageIO.write(this.toBufferedImage(), "png", out);
        return out.toByteArray();
    }

    public void writeToFile(File file) throws IOException {
        ImageIO.write(this.toBufferedImage(), "png", file);
    }

    public void writeToFile(Path path) throws IOException {
        this.writeToFile(path.toFile());
    }

    public BufferedImage toBufferedImage() {
        var image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, this.width, this.height, this.makePixelArray(), 0, this.width);
        return image;
    }

    // --- pixels --------------------------------------------------------------------------------------------------------

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public Format format() {
        return this.format;
    }

    public int getPixelRGBA(int x, int y) {
        this.check(x, y);
        return this.data[x + y * this.width];
    }

    public void setPixelRGBA(int x, int y, int abgr) {
        this.check(x, y);
        this.data[x + y * this.width] = abgr;
    }

    /** 1.21 blendPixel: the color drawn over the pixel by its alpha. */
    public void blendPixel(int x, int y, int abgr) {
        int old = this.getPixelRGBA(x, y);
        float a = (abgr >>> 24) / 255.0F;
        if (a <= 0.0F) {
            return;
        }
        int r = blend(abgr & 0xFF, old & 0xFF, a);
        int g = blend(abgr >> 8 & 0xFF, old >> 8 & 0xFF, a);
        int b = blend(abgr >> 16 & 0xFF, old >> 16 & 0xFF, a);
        int alpha = Math.min(255, (abgr >>> 24) + (int) ((old >>> 24) * (1.0F - a)));
        this.setPixelRGBA(x, y, alpha << 24 | b << 16 | g << 8 | r);
    }

    private static int blend(int source, int target, float alpha) {
        return Math.min(255, Math.round(source * alpha + target * (1.0F - alpha)));
    }

    public byte getLuminanceOrAlpha(int x, int y) {
        int pixel = this.getPixelRGBA(x, y);
        return (byte) (this.format.hasAlpha() ? pixel >>> 24 : pixel & 0xFF);
    }

    /** The pixels in 1.21's RGBA order (ABGR numbers). */
    public int[] getPixelsRGBA() {
        return this.data.clone();
    }

    /** The pixels as ARGB (what Java images and 1.12.2 textures use). */
    public int[] makePixelArray() {
        var argb = new int[this.data.length];
        for (int i = 0; i < argb.length; i++) {
            argb[i] = swapRedBlue(this.data[i]);
        }
        return argb;
    }

    public void fillRect(int x, int y, int width, int height, int abgr) {
        for (int j = y; j < y + height; j++) {
            for (int i = x; i < x + width; i++) {
                this.setPixelRGBA(i, j, abgr);
            }
        }
    }

    public void copyFrom(NativeImage other) {
        if (other.width != this.width || other.height != this.height) {
            throw new IllegalArgumentException("Image sizes don't match");
        }
        System.arraycopy(other.data, 0, this.data, 0, this.data.length);
    }

    public void copyRect(int x, int y, int offsetX, int offsetY, int width, int height, boolean flipX, boolean flipY) {
        this.copyRect(this, x, y, x + offsetX, y + offsetY, width, height, flipX, flipY);
    }

    public void copyRect(NativeImage target, int x, int y, int targetX, int targetY, int width, int height, boolean flipX, boolean flipY) {
        var copy = new int[width * height];
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                copy[i + j * width] = this.getPixelRGBA(x + i, y + j);
            }
        }
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int sx = flipX ? width - 1 - i : i;
                int sy = flipY ? height - 1 - j : j;
                target.setPixelRGBA(targetX + i, targetY + j, copy[sx + sy * width]);
            }
        }
    }

    public void flipY() {
        var row = new int[this.width];
        for (int y = 0; y < this.height / 2; y++) {
            int top = y * this.width;
            int bottom = (this.height - 1 - y) * this.width;
            System.arraycopy(this.data, top, row, 0, this.width);
            System.arraycopy(this.data, bottom, this.data, top, this.width);
            System.arraycopy(row, 0, this.data, bottom, this.width);
        }
    }

    public NativeImage mappedCopy(IntUnaryOperator function) {
        var copy = new NativeImage(this.format, this.width, this.height, false);
        for (int i = 0; i < this.data.length; i++) {
            copy.data[i] = function.applyAsInt(this.data[i]);
        }
        return copy;
    }

    public void applyToAllPixels(IntUnaryOperator function) {
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] = function.applyAsInt(this.data[i]);
        }
    }

    // --- OpenGL (the texture bound now) --------------------------------------------------------------------------------

    public void upload(int level, int x, int y, boolean mipmap) {
        this.upload(level, x, y, 0, 0, this.width, this.height, false, false, mipmap, false);
    }

    public void upload(int level, int x, int y, int skipX, int skipY, int width, int height, boolean mipmap, boolean autoClose) {
        this.upload(level, x, y, skipX, skipY, width, height, false, false, mipmap, autoClose);
    }

    public void upload(int level, int x, int y, int skipX, int skipY, int width, int height, boolean blur, boolean clamp, boolean mipmap,
            boolean autoClose) {
        IntBuffer buffer = BufferUtils.createIntBuffer(width * height);
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                buffer.put(swapRedBlue(this.data[skipX + i + (skipY + j) * this.width]));
            }
        }
        buffer.flip();
        int filter = blur ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mipmap ? GL11.GL_NEAREST_MIPMAP_LINEAR : filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        int wrap = clamp ? GL12.GL_CLAMP_TO_EDGE : GL11.GL_REPEAT;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, x, y, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
        if (autoClose) {
            this.close();
        }
    }

    /** The bound texture's pixels into this image (its size). */
    public void downloadTexture(int level, boolean opaque) {
        IntBuffer buffer = BufferUtils.createIntBuffer(this.width * this.height);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, level, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
        for (int i = 0; i < this.data.length; i++) {
            int abgr = swapRedBlue(buffer.get(i));
            this.data[i] = opaque ? abgr | 0xFF000000 : abgr;
        }
    }

    @Override
    public void close() {
        this.pixels = 0L;
    }

    @Override
    public String toString() {
        return "NativeImage[" + this.format + " " + this.width + "x" + this.height + "]";
    }

    private void check(int x, int y) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            throw new IllegalArgumentException(String.format("(%s, %s) outside of image bounds (%s, %s)", x, y, this.width, this.height));
        }
    }

    /** ABGR <-> ARGB. */
    static int swapRedBlue(int color) {
        return color & 0xFF00FF00 | (color >> 16 & 0xFF) | (color & 0xFF) << 16;
    }

    /** 1.21 NativeImage.Format. */
    public enum Format {
        RGBA(4, true),
        RGB(3, false),
        LUMINANCE_ALPHA(2, true),
        LUMINANCE(1, false);

        private final int components;
        private final boolean hasAlpha;

        Format(int components, boolean hasAlpha) {
            this.components = components;
            this.hasAlpha = hasAlpha;
        }

        public int components() {
            return this.components;
        }

        public boolean hasAlpha() {
            return this.hasAlpha;
        }
    }
}
