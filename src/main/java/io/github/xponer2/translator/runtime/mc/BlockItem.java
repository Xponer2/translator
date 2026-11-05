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

import java.util.List;

import javax.annotation.Nullable;

import io.github.xponer2.translator.item.ItemProperties;
import io.github.xponer2.translator.item.TooltipContext;
import io.github.xponer2.translator.text.ItemNames;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Facade of the 1.21 {@code BlockItem} for original 1.21.1 mods (runtime): an item block built from properties. */
public class BlockItem extends ItemBlock implements TooltipContext.HoverText, io.github.xponer2.translator.util.ItemLike {
	public BlockItem(Block block, ItemProperties properties) {
		super(block);
		properties.applyTo(this);
	}

	public Block getBlock() {
		return this.block;
	}

	// --- 1.21 placement (a mod's block item calls these through super) -----------------------------------------------

	public void registerBlocks(java.util.Map<Block, net.minecraft.item.Item> map, net.minecraft.item.Item item) {
		map.put(this.getBlock(), item);
	}

	public void removeFromBlockToItemMap(java.util.Map<Block, net.minecraft.item.Item> map, net.minecraft.item.Item item) {
		map.remove(this.getBlock());
	}

	/** Places the block for a placement context: the state for it, the block entity data, the sound, one item less. */
	public net.minecraft.util.EnumActionResult place(io.github.xponer2.translator.util.BlockPlaceContext context) {
		World world = context.getLevel();
		net.minecraft.util.math.BlockPos pos = context.getClickedPos();
		net.minecraft.entity.player.EntityPlayer player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		net.minecraft.util.EnumFacing face = context.getClickedFace();
		// into a block that takes it (a flower bed): 1.12.2's mayPlace would ask the block's material only
		boolean into = !world.getBlockState(pos).getBlock().isReplaceable(world, pos);
		if (stack.isEmpty() || !context.canPlace() || player != null && !player.canPlayerEdit(pos, face, stack)
				|| !into && !world.mayPlace(this.getBlock(), pos, false, face, player)) {
			return net.minecraft.util.EnumActionResult.FAIL;
		}
		net.minecraft.block.state.IBlockState state = this.getPlacementState(context);
		if (state == null || !this.placeBlockAt(stack, player, world, pos, face, context.hitX(), context.hitY(), context.hitZ(), state)) {
			return net.minecraft.util.EnumActionResult.FAIL;
		}
		net.minecraft.block.state.IBlockState placed = world.getBlockState(pos);
		net.minecraft.block.SoundType sound = placed.getBlock().getSoundType(placed, world, pos, player);
		world.playSound(player, pos, sound.getPlaceSound(), net.minecraft.util.SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
				sound.getPitch() * 0.8F);
		if (player == null || !player.capabilities.isCreativeMode) {
			stack.shrink(1);
		}
		return net.minecraft.util.EnumActionResult.SUCCESS;
	}

	@Nullable
	@SuppressWarnings("deprecation")
	public net.minecraft.block.state.IBlockState getPlacementState(io.github.xponer2.translator.util.BlockPlaceContext context) {
		Block block = this.getBlock();
		net.minecraft.block.state.IBlockState state = block instanceof io.github.xponer2.translator.runtime.mc.Block modBlock
				? modBlock.getStateForPlacement(context)
				: block.getStateForPlacement(context.getLevel(), context.getClickedPos(), context.getClickedFace(), context.hitX(), context.hitY(),
						context.hitZ(), this.getMetadata(context.getItemInHand().getMetadata()), context.getPlayer(), context.getHand());
		return state != null && this.canPlace(context, state) ? state : null;
	}

	public boolean canPlace(io.github.xponer2.translator.util.BlockPlaceContext context, net.minecraft.block.state.IBlockState state) {
		return true;
	}

	/**
	 * Placing on a block of a 1.21 mod that takes it (1.21 canBeReplaced: a flower bed takes up to four flowers of its
	 * item, wildflowers and pink petals): placed into that block, not next to it. 1.12.2 asks only isReplaceable.
	 */
	@Override
	public net.minecraft.util.EnumActionResult onItemUse(net.minecraft.entity.player.EntityPlayer player, World world, net.minecraft.util.math.BlockPos pos,
			net.minecraft.util.EnumHand hand, net.minecraft.util.EnumFacing facing, float hitX, float hitY, float hitZ) {
		net.minecraft.block.state.IBlockState clicked = world.getBlockState(pos);
		if (clicked.getBlock() instanceof io.github.xponer2.translator.runtime.mc.Block modBlock && !clicked.getBlock().isReplaceable(world, pos)) {
			io.github.xponer2.translator.util.BlockPlaceContext context;
			boolean replace;
			try {
				context = io.github.xponer2.translator.util.BlockPlaceContext.of(world, pos, facing, hitX, hitY, hitZ, player, hand);
				replace = modBlock.canBeReplaced(clicked, context);
			} catch (RuntimeException e) {
				context = null;
				replace = false;
			}
			if (replace) {
				return this.place(context);
			}
		}
		return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
	}

	/**
	 * 1.21 {@code placeBlock(context, state)}: the block set (a door's item clears the block above first). 1.12.2's
	 * placement goes through here, so a mod's override of it runs: doors were placed without their upper half.
	 */
	public boolean placeBlock(io.github.xponer2.translator.util.BlockPlaceContext context, net.minecraft.block.state.IBlockState state) {
		return context.getLevel().setBlockState(context.getClickedPos(), state, 11);
	}

	/** 1.12.2's placement: the block by the 1.21 placeBlock, then its block entity data and setPlacedBy as 1.12.2 does. */
	@Override
	public boolean placeBlockAt(ItemStack stack, net.minecraft.entity.player.EntityPlayer player, World world, net.minecraft.util.math.BlockPos pos,
			net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ, net.minecraft.block.state.IBlockState newState) {
		io.github.xponer2.translator.util.BlockPlaceContext context;
		try {
			net.minecraft.util.EnumHand hand = player != null && player.getHeldItem(net.minecraft.util.EnumHand.OFF_HAND) == stack
					? net.minecraft.util.EnumHand.OFF_HAND : net.minecraft.util.EnumHand.MAIN_HAND;
			context = player == null ? null : io.github.xponer2.translator.util.BlockPlaceContext.of(world, pos, side, hitX, hitY, hitZ, player, hand);
		} catch (RuntimeException e) {
			context = null;
		}
		if (context == null) {
			return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
		}
		boolean placed;
		try {
			placed = this.placeBlock(context, newState);
		} catch (RuntimeException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".placeBlock", e);
			placed = world.setBlockState(pos, newState, 11);
		}
		if (!placed) {
			return false;
		}
		net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
		if (state.getBlock() == this.block) {
			setTileEntityNBT(world, player, pos, stack);
			this.block.onBlockPlacedBy(world, pos, state, player, stack);
			if (player instanceof net.minecraft.entity.player.EntityPlayerMP serverPlayer) {
				net.minecraft.advancements.CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
			}
		}
		return true;
	}

	public boolean updateCustomBlockEntityTag(net.minecraft.util.math.BlockPos pos, World world, @Nullable net.minecraft.entity.player.EntityPlayer player,
			ItemStack stack, net.minecraft.block.state.IBlockState state) {
		return ItemBlock.setTileEntityNBT(world, player, pos, stack);
	}

	public net.minecraft.util.EnumActionResult onItemUseFirst(ItemStack stack, io.github.xponer2.translator.util.UseOnContext context) {
		return net.minecraft.util.EnumActionResult.PASS;
	}

	public boolean isPrimaryItemFor(ItemStack stack, io.github.xponer2.translator.registry.Holder<?> enchantment) {
		return false;
	}

	public boolean supportsEnchantment(ItemStack stack, io.github.xponer2.translator.registry.Holder<net.minecraft.enchantment.Enchantment> enchantment) {
		return enchantment != null && enchantment.value().canApply(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<ITextComponent> tooltip, ITooltipFlag flag) {
	}

	public ITextComponent getName(ItemStack stack) {
		return new TextComponentString(ItemNames.displayName(this, stack));
	}

	public String getDescriptionId() {
		return this.getTranslationKey();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		try {
			TooltipContext.addInformation(this, stack, world, tooltip, flag);
		} catch (Throwable t) {
			// the search tree asks for every tooltip at load and when the server stops: a throw there stops the game
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the tooltip of " + this.getClass().getName(), t);
		}
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		try {
			return this.getName(stack).getFormattedText();
		} catch (Throwable t) {
			// the game asks every item's name (statistics, search trees) when the mod may not be ready for it
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the name of " + this.getClass().getName(), t);
			return ItemNames.displayName(this, stack);
		}
	}

	/** Every item of a 1.21.1 mod is in the search tab, as in 1.21 (Forge lists an ingredient of any damage through it). */
	@Override
	protected boolean isInCreativeTab(net.minecraft.creativetab.CreativeTabs tab) {
		return tab == net.minecraft.creativetab.CreativeTabs.SEARCH || super.isInCreativeTab(tab);
	}
}
