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
package io.github.xponer2.translator.lwjgl;

import static io.github.xponer2.translator.lwjgl.Native.UNSAFE;

import java.nio.ByteBuffer;

/**
 * LWJGL 3's {@code org.lwjgl.stb.STBImageResize} (the native stb_image_resize) in Java: 8-bit images in native memory,
 * averaged when made smaller, bilinear when made larger. Both the old API (a channel count) and the new one (a pixel
 * layout) are here: mods pick one by reflection (JourneyMap's map tiles).
 */
public final class STBImageResize {
	// the pixel layouts of stb_image_resize 2 (LWJGL 3.3.4+): the channel count for the formats Minecraft uses
	public static final int STBIR_1CHANNEL = 1;
	public static final int STBIR_2CHANNEL = 2;
	public static final int STBIR_RGB = 3;
	public static final int STBIR_BGR = 0;
	public static final int STBIR_4CHANNEL = 5;
	public static final int STBIR_RGBA = 4;
	public static final int STBIR_BGRA = 6;
	public static final int STBIR_ARGB = 7;
	public static final int STBIR_ABGR = 8;
	public static final int STBIR_RA = 9;
	public static final int STBIR_AR = 10;

	private STBImageResize() {
	}

	/** Old API: 1 for success. A stride of 0 is a packed row. */
	public static int nstbir_resize_uint8(long input, int inputW, int inputH, int inputStride, long output, int outputW, int outputH,
			int outputStride, int channels) {
		resize(input, inputW, inputH, inputStride, output, outputW, outputH, outputStride, channels);
		return 1;
	}

	public static boolean stbir_resize_uint8(ByteBuffer input, int inputW, int inputH, int inputStride, ByteBuffer output, int outputW,
			int outputH, int outputStride, int channels) {
		return nstbir_resize_uint8(MemoryUtil.memAddress(input), inputW, inputH, inputStride, MemoryUtil.memAddress(output), outputW, outputH,
				outputStride, channels) != 0;
	}

	/** New API: the output address (0 for a failure). */
	public static long nstbir_resize_uint8_linear(long input, int inputW, int inputH, int inputStride, long output, int outputW, int outputH,
			int outputStride, int layout) {
		resize(input, inputW, inputH, inputStride, output, outputW, outputH, outputStride, channels(layout));
		return output;
	}

	public static long nstbir_resize_uint8_srgb(long input, int inputW, int inputH, int inputStride, long output, int outputW, int outputH,
			int outputStride, int layout) {
		return nstbir_resize_uint8_linear(input, inputW, inputH, inputStride, output, outputW, outputH, outputStride, layout);
	}

	private static int channels(int layout) {
		return switch (layout) {
			case STBIR_1CHANNEL -> 1;
			case STBIR_2CHANNEL, STBIR_RA, STBIR_AR -> 2;
			case STBIR_RGB, STBIR_BGR -> 3;
			default -> 4;
		};
	}

	/** Each output pixel from the input pixels it covers (made smaller) or the four nearest (made larger). */
	public static void resize(long input, int inputW, int inputH, int inputStride, long output, int outputW, int outputH, int outputStride,
			int channels) {
		int inStride = inputStride == 0 ? inputW * channels : inputStride;
		int outStride = outputStride == 0 ? outputW * channels : outputStride;
		double scaleX = (double) inputW / outputW;
		double scaleY = (double) inputH / outputH;
		int[] sum = new int[channels];
		for (int y = 0; y < outputH; y++) {
			for (int x = 0; x < outputW; x++) {
				long target = output + (long) y * outStride + (long) x * channels;
				if (scaleX >= 1.0 && scaleY >= 1.0) {
					int x0 = (int) (x * scaleX);
					int y0 = (int) (y * scaleY);
					int x1 = Math.max(x0 + 1, Math.min(inputW, (int) Math.ceil((x + 1) * scaleX)));
					int y1 = Math.max(y0 + 1, Math.min(inputH, (int) Math.ceil((y + 1) * scaleY)));
					java.util.Arrays.fill(sum, 0);
					for (int sy = y0; sy < y1; sy++) {
						long row = input + (long) sy * inStride;
						for (int sx = x0; sx < x1; sx++) {
							for (int c = 0; c < channels; c++) {
								sum[c] += UNSAFE.getByte(row + (long) sx * channels + c) & 0xFF;
							}
						}
					}
					int count = (x1 - x0) * (y1 - y0);
					for (int c = 0; c < channels; c++) {
						UNSAFE.putByte(target + c, (byte) ((sum[c] + count / 2) / count));
					}
				} else {
					double fx = Math.max(0.0, (x + 0.5) * scaleX - 0.5);
					double fy = Math.max(0.0, (y + 0.5) * scaleY - 0.5);
					int x0 = Math.min((int) fx, inputW - 1);
					int y0 = Math.min((int) fy, inputH - 1);
					int x1 = Math.min(x0 + 1, inputW - 1);
					int y1 = Math.min(y0 + 1, inputH - 1);
					double dx = fx - x0;
					double dy = fy - y0;
					for (int c = 0; c < channels; c++) {
						double top = sample(input, inStride, channels, x0, y0, c) * (1 - dx) + sample(input, inStride, channels, x1, y0, c) * dx;
						double bottom = sample(input, inStride, channels, x0, y1, c) * (1 - dx) + sample(input, inStride, channels, x1, y1, c) * dx;
						UNSAFE.putByte(target + c, (byte) Math.round(top * (1 - dy) + bottom * dy));
					}
				}
			}
		}
	}

	private static int sample(long input, int stride, int channels, int x, int y, int channel) {
		return UNSAFE.getByte(input + (long) y * stride + (long) x * channels + channel) & 0xFF;
	}
}
