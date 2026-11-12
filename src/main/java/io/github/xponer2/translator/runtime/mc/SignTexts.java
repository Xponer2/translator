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

	public static SignText getFrontText(TileEntitySign sign) {
		SignText text = sign instanceof SignBlockEntity own ? own.frontText : null;
		if (text == null) {
			text = new SignText();
		}
		for (int i = 0; i < sign.signText.length && i < 4; i++) {
			ITextComponent line = sign.signText[i];
			if (line != null && line != text.getMessage(i, false)) {
				text = text.setMessage(i, line);
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
				ITextComponent line = text.getMessage(i, false);
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
