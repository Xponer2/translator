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

import javax.annotation.Nullable;

import io.github.xponer2.translator.item.ItemProperties;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 1.21 {@code ArrowItem} for the mods' arrows that extend it (the E line: a vanilla arrow and a bow's check stay
 * 1.12.2's ItemArrow): made with their properties (1.12.2's takes none: the mods' arrow items weren't made), with 1.21's
 * createArrow (a weapon more) and isInfinite (any shooter) behind 1.12.2's.
 */
public class ArrowItem extends ItemArrow implements io.github.xponer2.translator.item.TooltipContext.HoverText {
	public ArrowItem(ItemProperties properties) {
		properties.applyTo(this);
	}

	/** 1.21 {@code createArrow(level, ammo, shooter, weapon)}: the mod's arrow (1.12.2's by default). */
	public EntityArrow createArrow(World level, ItemStack ammo, EntityLivingBase shooter, @Nullable ItemStack weapon) {
		return super.createArrow(level, ammo, shooter);
	}

	@Override
	public EntityArrow createArrow(World level, ItemStack ammo, EntityLivingBase shooter) {
		return this.createArrow(level, ammo, shooter, ItemStack.EMPTY);
	}

	/** NeoForge {@code isInfinite(ammo, bow, shooter)}: Forge's for a player. */
	public boolean isInfinite(ItemStack ammo, ItemStack bow, EntityLivingBase shooter) {
		return shooter instanceof EntityPlayer && super.isInfinite(ammo, bow, (EntityPlayer) shooter);
	}

	@Override
	public boolean isInfinite(ItemStack ammo, ItemStack bow, EntityPlayer player) {
		return this.isInfinite(ammo, bow, (EntityLivingBase) player);
	}

	/** 1.21 {@code appendHoverText(stack, context, tooltip, flag)}: the mod's tooltip lines (none by default). */
	@Override
	public void appendHoverText(ItemStack stack, io.github.xponer2.translator.item.TooltipContext context, java.util.List<net.minecraft.util.text.ITextComponent> tooltip,
			net.minecraft.client.util.ITooltipFlag flag) {
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World world, java.util.List<String> tooltip, net.minecraft.client.util.ITooltipFlag flag) {
		try {
			io.github.xponer2.translator.item.TooltipContext.addInformation(this, stack, world, tooltip, flag);
		} catch (Throwable t) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the tooltip of " + this.getClass().getName(), t);
		}
	}

	/** 1.21 {@code getName(stack)}: the name by the 1.21 rules. */
	public net.minecraft.util.text.ITextComponent getName(ItemStack stack) {
		return new net.minecraft.util.text.TextComponentString(io.github.xponer2.translator.text.ItemNames.displayName(this, stack));
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		try {
			return this.getName(stack).getFormattedText();
		} catch (Throwable t) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the name of " + this.getClass().getName(), t);
			return io.github.xponer2.translator.text.ItemNames.displayName(this, stack);
		}
	}

	/** Every item of a 1.21.1 mod is in the search tab, as in 1.21. */
	@Override
	protected boolean isInCreativeTab(net.minecraft.creativetab.CreativeTabs tab) {
		return tab == net.minecraft.creativetab.CreativeTabs.SEARCH || super.isInCreativeTab(tab);
	}
}
