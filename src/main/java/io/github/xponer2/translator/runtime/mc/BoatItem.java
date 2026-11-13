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

import io.github.xponer2.translator.item.ItemProperties;

import net.minecraft.entity.item.EntityBoat;
import net.minecraft.item.ItemBoat;

/** Facade of the 1.21 {@code BoatItem}: the 1.12.2 boat item of a boat type (chest boats are plain boats here). */
public class BoatItem extends ItemBoat {
	public BoatItem(boolean hasChest, EntityBoat.Type type, ItemProperties properties) {
		super(type == null ? EntityBoat.Type.OAK : type);
		// 1.12.2's boat names itself boat.<type>: the mod's registry name gives the 1.21 key (item.<mod>.<id>), as for
		// any item (Twilight Forest's boats showed "item.boat.twilightforest:canopy")
		this.setTranslationKey("null");
		properties.applyTo(this);
	}

	/** 1.21 {@code getName(stack)}: the name by the 1.21 rules (1.12.2's vanilla items show "Blood Bucket.name"). */
	public net.minecraft.util.text.ITextComponent getName(net.minecraft.item.ItemStack stack) {
		return new net.minecraft.util.text.TextComponentString(io.github.xponer2.translator.text.ItemNames.displayName(this, stack));
	}

	@Override
	public String getItemStackDisplayName(net.minecraft.item.ItemStack stack) {
		try {
			return this.getName(stack).getFormattedText();
		} catch (Throwable t) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the name of " + this.getClass().getName(), t);
			return io.github.xponer2.translator.text.ItemNames.displayName(this, stack);
		}
	}

	/** Every item of a 1.21.1 mod is in the search tab, as in 1.21 (Forge lists an ingredient of any damage through it). */
	@Override
	protected boolean isInCreativeTab(net.minecraft.creativetab.CreativeTabs tab) {
		return tab == net.minecraft.creativetab.CreativeTabs.SEARCH || super.isInCreativeTab(tab);
	}
}
