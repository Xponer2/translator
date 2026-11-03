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

import static io.github.xponer2.translator.lwjgl.Native.UNSAFE;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.function.IntUnaryOperator;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import io.github.xponer2.translator.lwjgl.Native;
import io.github.xponer2.translator.lwjgl.STBImageResize;

/**
 * 1.21 {@code NativeImage}: the pixels in native memory as in 1.21 ({@link #pixels} is their address, which mods hand to
 * LWJGL 3's MemoryUtil and STB: the Translator's versions of those work on it), {@code format.components()} bytes per
 * pixel, RGBA as bytes (so {@code getPixelRGBA} gives 0xAABBGGRR). Files go through ImageIO (1.21 uses STB); upload and
 * download use the texture bound in OpenGL, like 1.21.
 */
public class NativeImage implements AutoCloseable {
    private final Format format;
    private final int width;
    private final int height;
    private final long size;
    /** The address of the pixels, 0 once closed (mods check {@code image.pixels != 0} for an image still open). */
    public long pixels;

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
        this.size = (long) width * height * format.components();
        // always cleared: an image read before it is drawn shows black rather than old memory
        this.pixels = Native.allocate(this.size, true);
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
            result.putAbgr(i, swapRedBlue(argb[i]));
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
        return this.getAbgr(x + y * this.width);
    }

    public void setPixelRGBA(int x, int y, int abgr) {
        this.check(x, y);
        this.putAbgr(x + y * this.width, abgr);
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
        this.check(x, y);
        long pixel = this.pixel(x + y * this.width);
        return UNSAFE.getByte(pixel + (this.format.hasAlpha() ? this.format.components() - 1 : 0));
    }

    /** The pixels in 1.21's RGBA order (ABGR numbers). */
    public int[] getPixelsRGBA() {
        var abgr = new int[this.width * this.height];
        for (int i = 0; i < abgr.length; i++) {
            abgr[i] = this.getAbgr(i);
        }
        return abgr;
    }

    /** The pixels as ARGB (what Java images and 1.12.2 textures use). */
    public int[] makePixelArray() {
        var argb = new int[this.width * this.height];
        for (int i = 0; i < argb.length; i++) {
            argb[i] = swapRedBlue(this.getAbgr(i));
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
        if (other.format != this.format || other.width != this.width || other.height != this.height) {
            throw new IllegalArgumentException("Image formats don't match");
        }
        this.checkOpen();
        other.checkOpen();
        UNSAFE.copyMemory(other.pixels, this.pixels, this.size);
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
        this.checkOpen();
        int stride = this.width * this.format.components();
        var row = new byte[stride];
        for (int y = 0; y < this.height / 2; y++) {
            long top = this.pixels + (long) y * stride;
            long bottom = this.pixels + (long) (this.height - 1 - y) * stride;
            UNSAFE.copyMemory(null, top, row, Native.BYTE_ARRAY_OFFSET, stride);
            UNSAFE.copyMemory(bottom, top, stride);
            UNSAFE.copyMemory(row, Native.BYTE_ARRAY_OFFSET, null, bottom, stride);
        }
    }

    /** 1.21 resizeSubRectTo: the part of this image scaled into the whole target (STB resize in 1.21). */
    public void resizeSubRectTo(int x, int y, int width, int height, NativeImage target) {
        this.checkOpen();
        target.checkOpen();
        if (target.format != this.format) {
            throw new UnsupportedOperationException("resizeSubRectTo only works for images of the same format");
        }
        int components = this.format.components();
        long start = this.pixels + ((long) x + (long) y * this.width) * components;
        STBImageResize.resize(start, width, height, this.width * components, target.pixels, target.width, target.height, 0, components);
    }

    public NativeImage mappedCopy(IntUnaryOperator function) {
        var copy = new NativeImage(this.format, this.width, this.height, false);
        int count = this.width * this.height;
        for (int i = 0; i < count; i++) {
            copy.putAbgr(i, function.applyAsInt(this.getAbgr(i)));
        }
        return copy;
    }

    public void applyToAllPixels(IntUnaryOperator function) {
        int count = this.width * this.height;
        for (int i = 0; i < count; i++) {
            this.putAbgr(i, function.applyAsInt(this.getAbgr(i)));
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
        this.checkOpen();
        int filter = blur ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mipmap ? GL11.GL_NEAREST_MIPMAP_LINEAR : filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        int wrap = clamp ? GL12.GL_CLAMP_TO_EDGE : GL11.GL_REPEAT;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap);
        // the rows of the part straight from the image's memory, as 1.21 does
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, this.width);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipX);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipY);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, this.format.components());
        try {
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, x, y, width, height, this.format.glFormat(), GL11.GL_UNSIGNED_BYTE,
                    Native.wrap(this.pixels, (int) this.size));
        } finally {
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        }
        if (autoClose) {
            this.close();
        }
    }

    /** The bound texture's pixels into this image (its size). */
    public void downloadTexture(int level, boolean opaque) {
        this.checkOpen();
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, this.format.components());
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, level, this.format.glFormat(), GL11.GL_UNSIGNED_BYTE, Native.wrap(this.pixels, (int) this.size));
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4);
        if (opaque && this.format.hasAlpha()) {
            int count = this.width * this.height;
            int components = this.format.components();
            for (int i = 0; i < count; i++) {
                UNSAFE.putByte(this.pixels + (long) i * components + components - 1, (byte) 0xFF);
            }
        }
    }

    @Override
    public void close() {
        if (this.pixels != 0L) {
            UNSAFE.freeMemory(this.pixels);
            this.pixels = 0L;
        }
    }

    @Override
    public String toString() {
        return "NativeImage[" + this.format + " " + this.width + "x" + this.height + "@" + this.pixels + "]";
    }

    // --- memory ----------------------------------------------------------------------------------------------------------

    private long pixel(int index) {
        this.checkOpen();
        return this.pixels + (long) index * this.format.components();
    }

    /** A pixel as an ABGR number; formats without all four channels are widened (gray to RGB, no alpha to opaque). */
    private int getAbgr(int index) {
        long pixel = this.pixel(index);
        return switch (this.format) {
            case RGBA -> UNSAFE.getInt(pixel);
            case RGB -> 0xFF000000 | (UNSAFE.getByte(pixel + 2) & 0xFF) << 16 | (UNSAFE.getByte(pixel + 1) & 0xFF) << 8
                    | UNSAFE.getByte(pixel) & 0xFF;
            case LUMINANCE_ALPHA -> {
                int l = UNSAFE.getByte(pixel) & 0xFF;
                yield (UNSAFE.getByte(pixel + 1) & 0xFF) << 24 | l << 16 | l << 8 | l;
            }
            case LUMINANCE -> {
                int l = UNSAFE.getByte(pixel) & 0xFF;
                yield 0xFF000000 | l << 16 | l << 8 | l;
            }
        };
    }

    private void putAbgr(int index, int abgr) {
        long pixel = this.pixel(index);
        switch (this.format) {
            case RGBA -> UNSAFE.putInt(pixel, abgr);
            case RGB -> {
                UNSAFE.putByte(pixel, (byte) abgr);
                UNSAFE.putByte(pixel + 1, (byte) (abgr >> 8));
                UNSAFE.putByte(pixel + 2, (byte) (abgr >> 16));
            }
            case LUMINANCE_ALPHA -> {
                UNSAFE.putByte(pixel, (byte) luminance(abgr));
                UNSAFE.putByte(pixel + 1, (byte) (abgr >>> 24));
            }
            case LUMINANCE -> UNSAFE.putByte(pixel, (byte) luminance(abgr));
        }
    }

    private static int luminance(int abgr) {
        return ((abgr & 0xFF) * 299 + (abgr >> 8 & 0xFF) * 587 + (abgr >> 16 & 0xFF) * 114) / 1000;
    }

    private void checkOpen() {
        if (this.pixels == 0L) {
            throw new IllegalStateException("Image is not allocated.");
        }
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
        RGBA(4, true, GL11.GL_RGBA),
        RGB(3, false, GL11.GL_RGB),
        LUMINANCE_ALPHA(2, true, GL11.GL_LUMINANCE_ALPHA),
        LUMINANCE(1, false, GL11.GL_LUMINANCE);

        private final int components;
        private final boolean hasAlpha;
        private final int glFormat;

        Format(int components, boolean hasAlpha, int glFormat) {
            this.components = components;
            this.hasAlpha = hasAlpha;
            this.glFormat = glFormat;
        }

        public int components() {
            return this.components;
        }

        public boolean hasAlpha() {
            return this.hasAlpha;
        }

        public int glFormat() {
            return this.glFormat;
        }
    }
}
