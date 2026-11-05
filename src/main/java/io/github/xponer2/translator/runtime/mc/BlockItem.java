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
		if (stack.isEmpty() || !context.canPlace() || player != null && !player.canPlayerEdit(pos, face, stack)
				|| !world.mayPlace(this.getBlock(), pos, false, face, player)) {
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
}
