package io.github.xponer2.translator.block;

import java.util.Locale;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * 1.21 {@code SlabBlock}: one block with the property {@code type} = bottom, top or double (1.12.2 slabs are two
 * blocks). Using the slab's item on the open face of a half slab makes it double.
 */
public class SlabBlock extends PropertiesBlock {
	public static final PropertyEnum<SlabType> TYPE = PropertyEnum.create("type", SlabType.class);
	private static final AxisAlignedBB BOTTOM_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
	private static final AxisAlignedBB TOP_AABB = new AxisAlignedBB(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D);

	public SlabBlock(BlockProperties properties) {
		super(properties.noOcclusion());
		this.setDefaultState(this.blockState.getBaseState().withProperty(TYPE, SlabType.BOTTOM));
		this.useNeighborBrightness = true;
		this.lightOpacity = 255;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, TYPE);
	}

	@Override
	@SuppressWarnings("deprecation")
	public IBlockState getStateFromMeta(int meta) {
		return this.getDefaultState().withProperty(TYPE, SlabType.values()[Math.min(meta, 2)]);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(TYPE).ordinal();
	}

	@Override
	@SuppressWarnings("deprecation")
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return switch (state.getValue(TYPE)) {
			case BOTTOM -> BOTTOM_AABB;
			case TOP -> TOP_AABB;
			case DOUBLE -> FULL_BLOCK_AABB;
		};
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isOpaqueCube(IBlockState state) {
		return state.getValue(TYPE) == SlabType.DOUBLE;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isFullCube(IBlockState state) {
		return state.getValue(TYPE) == SlabType.DOUBLE;
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
		return this.doesSideBlockRendering(state, world, pos, face) ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
	}

	@Override
	public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
		var type = state.getValue(TYPE);
		return type == SlabType.DOUBLE || (type == SlabType.BOTTOM && face == EnumFacing.DOWN) || (type == SlabType.TOP && face == EnumFacing.UP);
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
		var top = facing == EnumFacing.DOWN || (facing != EnumFacing.UP && hitY > 0.5F);
		return this.getDefaultState().withProperty(TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		var held = player.getHeldItem(hand);
		var type = state.getValue(TYPE);
		var openFace = (type == SlabType.BOTTOM && facing == EnumFacing.UP) || (type == SlabType.TOP && facing == EnumFacing.DOWN);
		if (!openFace || held.isEmpty() || Block.getBlockFromItem(held.getItem()) != this || !player.canPlayerEdit(pos, facing, held)) {
			return false;
		}
		var doubled = state.withProperty(TYPE, SlabType.DOUBLE);
		var box = doubled.getCollisionBoundingBox(world, pos);
		if (box != null && !world.checkNoEntityCollision(box.offset(pos))) {
			return false;
		}
		if (!world.isRemote) {
			world.setBlockState(pos, doubled, 11);
			var sound = this.getSoundType(doubled, world, pos, player);
			world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
			if (!player.capabilities.isCreativeMode) {
				held.shrink(1);
			}
		}
		return true;
	}

	@Override
	public int quantityDropped(IBlockState state, int fortune, Random random) {
		return state.getValue(TYPE) == SlabType.DOUBLE ? 2 : 1;
	}

	@Override
	public int damageDropped(IBlockState state) {
		return 0;
	}

	public enum SlabType implements IStringSerializable {
		BOTTOM, TOP, DOUBLE;

		@Override
		public String getName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}
}
