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
package io.github.xponer2.translator.text;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;

/**
 * Item names with the 1.21 rules. 1.12.2 shows translate(translate(key) + ".name"): with the 1.21 name keys in the
 * language files (item.mod.wand=Wand, used by the 1.21 code) the inner translation already gives the name and the
 * outer one fails ("Wand.name"). The converter overrides getItemStackDisplayName of the item classes with this.
 */
@SuppressWarnings("deprecation")
public final class ItemNames {
	private ItemNames() { }

	public static String displayName(Item item, ItemStack stack) {
		var key = item.getTranslationKey(stack);
		if (I18n.canTranslate(key + ".name")) {
			return I18n.translateToLocal(key + ".name").trim();
		}
		// a 1.21 description id, or a name the item built itself
		return I18n.canTranslate(key) ? I18n.translateToLocal(key).trim() : key;
	}
}
