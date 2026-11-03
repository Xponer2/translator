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

import javax.annotation.Nullable;

import io.github.xponer2.translator.block.BlockProperties;
import io.github.xponer2.translator.block.BlockStateProperties;
import io.github.xponer2.translator.shape.CollisionContext;
import io.github.xponer2.translator.shape.Shapes;
import io.github.xponer2.translator.shape.VoxelShape;
import io.github.xponer2.translator.util.BlockPlaceContext;

import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code CropBlock} (neoforge-1.21.1-runtime.tsv): a crop on farmland that grows with light and
 * bone meal. The table renames getStateForAge to withAge and growCrops to grow.
 */
public class CropBlock extends BushBlock implements net.minecraft.block.IGrowable {
	public static final net.minecraft.block.properties.PropertyInteger AGE = BlockStateProperties.AGE_7;

	public CropBlock(BlockProperties properties) {
		super(properties.randomTicks());
	}

	@Override
	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
		builder.add(AGE);
	}

	@Override
	protected boolean mayPlaceOn(IBlockState state, IBlockAccess level, BlockPos pos) {
		return state.getBlock() == net.minecraft.init.Blocks.FARMLAND;
	}

	@Override
	public VoxelShape getShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		int age = state.getPropertyKeys().contains(this.getAgeProperty()) ? this.getAge(state) : this.getMaxAge();
		return Shapes.box16(0, 0, 0, 16, 2 + 14.0 * age / Math.max(1, this.getMaxAge()), 16);
	}

	public net.minecraft.block.properties.PropertyInteger getAgeProperty() {
		return AGE;
	}

	public int getMaxAge() {
		return 7;
	}

	public int getAge(IBlockState state) {
		return state.getValue(this.getAgeProperty());
	}

	public IBlockState withAge(int age) {
		return this.getDefaultState().withProperty(this.getAgeProperty(), Math.max(0, Math.min(age, this.getMaxAge())));
	}

	public boolean isMaxAge(IBlockState state) {
		return this.getAge(state) >= this.getMaxAge();
	}

	/** The seeds item of the crop (1.21 getBaseSeedId): the crop's own item unless the mod says. */
	protected io.github.xponer2.translator.util.ItemLike getBaseSeedId() {
		return this;
	}

	protected int getBonemealAgeIncrease(World level) {
		return 2 + level.rand.nextInt(4);
	}

	public void grow(World level, BlockPos pos, IBlockState state) {
		level.setBlockState(pos, this.withAge(this.getAge(state) + this.getBonemealAgeIncrease(level)), 2);
	}

	protected static float getGrowthChance(net.minecraft.block.Block block, IBlockAccess level, BlockPos pos) {
		return 1.0F;
	}

	/** A random tick: one age more now and then, with light. */
	public void randomTick(IBlockState state, net.minecraft.world.WorldServer level, BlockPos pos, Random random) {
		if (!this.isMaxAge(state) && level.getLightFromNeighbors(pos.up()) >= 9 && random.nextInt(5) == 0) {
			level.setBlockState(pos, this.withAge(this.getAge(state) + 1), 2);
		}
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
		super.updateTick(world, pos, state, random);
		if (world instanceof net.minecraft.world.WorldServer server && state.getBlock() == this) {
			this.randomTick(state, server, pos, random);
		}
	}

	@Override
	public net.minecraft.item.ItemStack getPickBlock(IBlockState state, net.minecraft.util.math.RayTraceResult target, World world, BlockPos pos,
			net.minecraft.entity.player.EntityPlayer player) {
		var seed = this.getBaseSeedId();
		return seed == null ? net.minecraft.item.ItemStack.EMPTY : new net.minecraft.item.ItemStack(seed.asItem());
	}

	// --- bone meal (1.12.2 IGrowable) --------------------------------------------------------------------------------

	@Override
	public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
		return !this.isMaxAge(state);
	}

	@Override
	public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
		return true;
	}

	@Override
	public void grow(World world, Random random, BlockPos pos, IBlockState state) {
		this.grow(world, pos, state);
	}
}
