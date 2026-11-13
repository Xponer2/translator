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

import java.util.Random;

import io.github.xponer2.translator.block.BlockProperties;

import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Facade of the 1.21 {@code DoorBlock}: the 1.12.2 door, with properties; it drops its own item (not the oak door). */
public class DoorBlock extends BlockDoor {
	public DoorBlock(Object blockSetType, BlockProperties properties) {
		super(properties.material());
		properties.applyTo(this, this::setSoundType);
	}

	/** 1.21 {@code DoorBlock.createBlockStateDefinition}: the door's properties; a mod's door adds its own to them. */
	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
		builder.add(HALF, FACING, OPEN, HINGE, POWERED);
	}

	/** The state of a 1.21 door: what its createBlockStateDefinition adds (Create's sliding doors: visible). */
	@Override
	protected BlockStateContainer createBlockState() {
		var builder = new io.github.xponer2.translator.block.LazyStateContainer.Builder(this);
		this.createBlockStateDefinition(builder);
		return builder.build();
	}

	/**
	 * 1.21 {@code DoorBlock.getStateForPlacement}: the lower half facing as the player looks, its hinge by the doors and
	 * blocks beside it (else by where the player clicked), powered and open with a signal; none without room above.
	 * 1.12.2's door item did this; a 1.21 mod's door item is a block item.
	 */
	@Override
	@SuppressWarnings("deprecation")
	public IBlockState getStateForPlacement(World world, BlockPos pos, net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ, int meta,
			net.minecraft.entity.EntityLivingBase placer, net.minecraft.util.EnumHand hand) {
		net.minecraft.util.EnumFacing facing = placer == null ? net.minecraft.util.EnumFacing.NORTH : placer.getHorizontalFacing();
		BlockPos left = pos.offset(facing.rotateYCCW());
		BlockPos right = pos.offset(facing.rotateY());
		int solidLeft = (world.getBlockState(left).isNormalCube() ? 1 : 0) + (world.getBlockState(left.up()).isNormalCube() ? 1 : 0);
		int solidRight = (world.getBlockState(right).isNormalCube() ? 1 : 0) + (world.getBlockState(right.up()).isNormalCube() ? 1 : 0);
		boolean doorLeft = world.getBlockState(left).getBlock() instanceof BlockDoor;
		boolean doorRight = world.getBlockState(right).getBlock() instanceof BlockDoor;
		EnumHingePosition hinge;
		if (doorLeft && !doorRight || solidRight > solidLeft) {
			hinge = EnumHingePosition.RIGHT;
		} else if (doorRight && !doorLeft || solidLeft > solidRight) {
			hinge = EnumHingePosition.LEFT;
		} else {
			// as 1.21: by the half of the block the player clicked
			int stepX = facing.getXOffset();
			int stepZ = facing.getZOffset();
			boolean leftHinge = (stepX >= 0 || hitZ >= 0.5F) && (stepX <= 0 || hitZ <= 0.5F) && (stepZ >= 0 || hitX <= 0.5F) && (stepZ <= 0 || hitX >= 0.5F);
			hinge = leftHinge ? EnumHingePosition.LEFT : EnumHingePosition.RIGHT;
		}
		boolean powered = world.isBlockPowered(pos) || world.isBlockPowered(pos.up());
		IBlockState state = this.getDefaultState().withProperty(FACING, facing).withProperty(HINGE, hinge).withProperty(HALF, EnumDoorHalf.LOWER);
		if (state.getPropertyKeys().contains(POWERED)) {
			state = state.withProperty(POWERED, powered);
		}
		return state.getPropertyKeys().contains(OPEN) ? state.withProperty(OPEN, powered) : state;
	}

	/** 1.21 {@code setPlacedBy}: the upper half above the lower one. */
	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, net.minecraft.entity.EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		if (state.getValue(HALF) == EnumDoorHalf.LOWER && world.getBlockState(pos.up()).getBlock() != this) {
			world.setBlockState(pos.up(), state.withProperty(HALF, EnumDoorHalf.UPPER), 3);
		}
	}

	@Override
	public Item getItemDropped(IBlockState state, Random random, int fortune) {
		return state.getValue(HALF) == EnumDoorHalf.UPPER ? net.minecraft.init.Items.AIR : Item.getItemFromBlock(this);
	}

	@Override
	public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
		return new ItemStack(Item.getItemFromBlock(this));
	}

	/** The drops of the block's 1.21 loot table (when the mod has one), else 1.12.2's. */
	@Override
	public void getDrops(net.minecraft.util.NonNullList<net.minecraft.item.ItemStack> drops, net.minecraft.world.IBlockAccess world,
			net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState state, int fortune) {
		java.util.Random random = world instanceof net.minecraft.world.World level ? level.rand : RANDOM;
		if (!io.github.xponer2.translator.loot.BlockLoot.drop(io.github.xponer2.translator.loot.BlockLoot.tableOf(this, null), drops, world, pos, state,
				fortune, harvesters.get(), random)) {
			super.getDrops(drops, world, pos, state, fortune);
		}
	}

	/**
	 * The layer the block is drawn in: the mod's choice or its model's render_type (a hedge of leaves, a door with
	 * glass: drawn solid by the 1.12.2 class this one extends, their holes showed as a background), else 1.12.2's.
	 */
	@Override
	@net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
	public net.minecraft.util.BlockRenderLayer getRenderLayer() {
		net.minecraft.util.BlockRenderLayer layer = io.github.xponer2.translator.client.RenderLayers.of(this);
		return layer != null ? layer : super.getRenderLayer();
	}
}
