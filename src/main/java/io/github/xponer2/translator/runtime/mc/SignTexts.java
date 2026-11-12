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
package io.github.xponer2.translator.runtime.mc;

import java.lang.reflect.Method;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.level.block.entity.SignText;

/**
 * 1.21 {@code SignBlockEntity} text methods on any sign, 1.12.2's own too (a mod's sign renderer reads the text of the
 * signs it draws: Farmer's Delight's canvas signs drew none). The front text is 1.12.2's four lines (what the edit screen,
 * the saved data and 1.12.2's renderer use) with the color and glow a mod's sign keeps; 1.12.2 signs have no back.
 */
public final class SignTexts {
	private SignTexts() {
	}

	// the carried SignText's methods (its compile stub can't name Minecraft types): getMessage(int, boolean),
	// setMessage(int, ITextComponent), getColor(), setColor(EnumDyeColor), hasGlowingText(), setHasGlowingText(boolean)
	private static Method getMessage;
	private static Method setMessage;
	private static Method getColor;
	private static Method setColor;
	private static Method hasGlowingText;
	private static Method setHasGlowingText;

	private static synchronized void methods() throws ReflectiveOperationException {
		if (getMessage == null) {
			Class<?> type = SignText.class;
			setMessage = type.getMethod("setMessage", int.class, ITextComponent.class);
			getColor = type.getMethod("getColor");
			setColor = type.getMethod("setColor", EnumDyeColor.class);
			hasGlowingText = type.getMethod("hasGlowingText");
			setHasGlowingText = type.getMethod("setHasGlowingText", boolean.class);
			getMessage = type.getMethod("getMessage", int.class, boolean.class);
		}
	}

	static ITextComponent message(SignText text, int line) {
		try {
			methods();
			return (ITextComponent) getMessage.invoke(text, line, false);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	static SignText withMessage(SignText text, int line, ITextComponent message) {
		try {
			methods();
			return (SignText) setMessage.invoke(text, line, message);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return text;
		}
	}

	static EnumDyeColor color(SignText text) {
		try {
			methods();
			return (EnumDyeColor) getColor.invoke(text);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return EnumDyeColor.BLACK;
		}
	}

	static SignText withColor(SignText text, EnumDyeColor color) {
		try {
			methods();
			return (SignText) setColor.invoke(text, color);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return text;
		}
	}

	static boolean glowing(SignText text) {
		try {
			methods();
			return (Boolean) hasGlowingText.invoke(text);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return false;
		}
	}

	static SignText withGlowing(SignText text, boolean glowing) {
		try {
			methods();
			return (SignText) setHasGlowingText.invoke(text, glowing);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return text;
		}
	}

	public static SignText getFrontText(TileEntitySign sign) {
		SignText text = sign instanceof SignBlockEntity own ? own.frontText : null;
		if (text == null) {
			text = new SignText();
		}
		for (int i = 0; i < sign.signText.length && i < 4; i++) {
			ITextComponent line = sign.signText[i];
			if (line != null && line != message(text, i)) {
				text = withMessage(text, i, line);
			}
		}
		if (sign instanceof SignBlockEntity own) {
			own.frontText = text;
		}
		return text;
	}

	public static SignText getBackText(TileEntitySign sign) {
		if (sign instanceof SignBlockEntity own) {
			if (own.backText == null) {
				own.backText = own.createDefaultSignText();
			}
			return own.backText;
		}
		return new SignText();
	}

	public static SignText getText(TileEntitySign sign, boolean front) {
		return front ? getFrontText(sign) : getBackText(sign);
	}

	/** 1.21 {@code setText(text, front)}: the front's lines become 1.12.2's, saved and sent. */
	public static boolean setText(TileEntitySign sign, SignText text, boolean front) {
		if (front) {
			for (int i = 0; i < sign.signText.length && i < 4; i++) {
				ITextComponent line = message(text, i);
				if (line != null) {
					sign.signText[i] = line;
				}
			}
		}
		if (sign instanceof SignBlockEntity own) {
			if (front) {
				own.frontText = text;
			} else {
				own.backText = text;
			}
		}
		sign.markDirty();
		if (sign.getWorld() != null) {
			var state = sign.getWorld().getBlockState(sign.getPos());
			sign.getWorld().notifyBlockUpdate(sign.getPos(), state, state, 3);
		}
		return true;
	}

	public static int getMaxTextLineWidth(TileEntitySign sign) {
		return 90;
	}

	public static int getTextLineHeight(TileEntitySign sign) {
		return 10;
	}
}
