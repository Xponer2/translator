package io.github.xponer2.translator.block;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The 1.21 {@code Block(Properties)}: a 1.12.2 block built from {@link BlockProperties}. The properties set material,
 * map color, sound, strength and drops; occlusion, collision, suffocation, redstone conduction and mob spawning follow
 * them like on 1.21.
 */
public class PropertiesBlock extends Block {
	protected final BlockProperties properties;

	public PropertiesBlock(BlockProperties properties) {
		super(properties.material(), properties.mapColor());
		this.properties = properties;
		properties.applyTo(this, this::setSoundType);
		// the Block constructor asked isOpaqueCube before the properties were set; blocks with a 1.21 shape answer from
		// the shape (a pedestal lets the light in, so the item on it isn't drawn black)
		this.fullBlock = this.isOpaqueCube(this.getDefaultState());
		this.lightOpacity = this.fullBlock ? 255 : 0;
	}

	public BlockProperties getProperties() {
		return this.properties;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isOpaqueCube(IBlockState state) {
		return this.properties == null || this.properties.canOcclude();
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isFullCube(IBlockState state) {
		return this.properties == null || this.properties.canOcclude();
	}

	@Nullable
	@Override
	@SuppressWarnings("deprecation")
	public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return this.properties.hasCollision() ? super.getCollisionBoundingBox(state, world, pos) : NULL_AABB;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean causesSuffocation(IBlockState state) {
		var suffocating = this.properties.isSuffocating(state, null, null);
		return suffocating != null ? suffocating : super.causesSuffocation(state);
	}

	@Override
	public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
		var conductor = this.properties.isRedstoneConductor(state, world, pos);
		return conductor != null ? conductor : super.isNormalCube(state, world, pos);
	}

	@Override
	public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, EntityLiving.SpawnPlacementType type) {
		return this.properties.isValidSpawn(state, world, pos, type) && super.canCreatureSpawn(state, world, pos, type);
	}
}
