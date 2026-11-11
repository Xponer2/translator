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
package io.github.xponer2.translator.runtime.redirect;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The 1.21 {@code VertexFormatElement} constants and record members (R row): its elements are 1.12.2's default ones,
 * the same objects the game's vertex formats hold, so a mod comparing a format's elements with them (Fusion's and
 * Supplementaries' quad builders, Xaero's buffers) finds them. 1.12.2 has no overlay element: UV1 is one of its own.
 */
@SideOnly(Side.CLIENT)
public final class VertexElementStatics {
	public static final VertexFormatElement POSITION = DefaultVertexFormats.POSITION_3F;
	public static final VertexFormatElement COLOR = DefaultVertexFormats.COLOR_4UB;
	public static final VertexFormatElement UV0 = DefaultVertexFormats.TEX_2F;
	public static final VertexFormatElement UV = UV0;
	public static final VertexFormatElement UV1 = new VertexFormatElement(1, VertexFormatElement.EnumType.SHORT, VertexFormatElement.EnumUsage.UV, 2);
	public static final VertexFormatElement UV2 = DefaultVertexFormats.TEX_2S;
	public static final VertexFormatElement NORMAL = DefaultVertexFormats.NORMAL_3B;
	public static final VertexFormatElement PADDING = DefaultVertexFormats.PADDING_1B;
	/** By the 1.21 ids (0 to 5 as 1.21 registers them; 32 places as 1.21's). */
	public static final VertexFormatElement[] BY_ID = new VertexFormatElement[32];
	public static final List<VertexFormatElement> ELEMENTS;

	static {
		BY_ID[0] = POSITION;
		BY_ID[1] = COLOR;
		BY_ID[2] = UV0;
		BY_ID[3] = UV1;
		BY_ID[4] = UV2;
		BY_ID[5] = NORMAL;
		ELEMENTS = ImmutableList.of(POSITION, COLOR, UV0, UV1, UV2, NORMAL);
	}

	private VertexElementStatics() {
	}

	/** 1.21 {@code id()}: the element's place in BY_ID (equal elements share it), else by its usage. */
	public static int id(VertexFormatElement element) {
		for (int i = 0; i < 6; i++) {
			if (BY_ID[i] == element) {
				return i;
			}
		}
		for (int i = 0; i < 6; i++) {
			if (BY_ID[i].equals(element)) {
				return i;
			}
		}
		switch (element.getUsage()) {
			case POSITION:
				return 0;
			case COLOR:
				return 1;
			case UV:
				return element.getIndex() == 0 ? 2 : element.getIndex() == 1 ? 3 : 4;
			case NORMAL:
				return 5;
			default:
				return 6;
		}
	}

	public static int mask(VertexFormatElement element) {
		return 1 << id(element);
	}

	public static int count(VertexFormatElement element) {
		return element.getElementCount();
	}

	public static int index(VertexFormatElement element) {
		return element.getIndex();
	}

	public static VertexFormatElement.EnumType type(VertexFormatElement element) {
		return element.getType();
	}

	public static VertexFormatElement.EnumUsage usage(VertexFormatElement element) {
		return element.getUsage();
	}

	public static VertexFormatElement byId(int id) {
		return id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
	}
}
