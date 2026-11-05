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

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

/**
 * Facade of the 1.21 {@code BucketItem} of a mod's fluid: a bucket that places the fluid's block (found when used: the
 * fluid's block may be registered after the bucket) and gives the empty bucket back.
 */
public class BucketItem extends ItemBucket {
	private final Fluid content;

	public BucketItem(Fluid content, ItemProperties properties) {
		super(Blocks.AIR);
		this.content = content;
		properties.applyTo(this);
		this.setContainerItem(Items.BUCKET);
	}

	public Fluid getFluid() {
		return this.content;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		Block block = this.content == null ? null : this.content.getBlock();
		RayTraceResult hit = this.rayTrace(world, player, false);
		if (block == null || hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
			return new ActionResult<>(EnumActionResult.PASS, stack);
		}
		BlockPos pos = hit.getBlockPos();
		if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
			pos = pos.offset(hit.sideHit);
		}
		if (!world.isBlockModifiable(player, pos) || !player.canPlayerEdit(pos, hit.sideHit, stack)
				|| !world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
			return new ActionResult<>(EnumActionResult.FAIL, stack);
		}
		if (!world.isRemote) {
			world.setBlockState(pos, block.getDefaultState(), 11);
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, player.capabilities.isCreativeMode ? stack : new ItemStack(Items.BUCKET));
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
}
