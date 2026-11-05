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
		super(orDefault(properties).material(), orDefault(properties).mapColor());
		this.properties = orDefault(properties);
		this.properties.applyTo(this, this::setSoundType);
		// the Block constructor asked isOpaqueCube before the properties were set; blocks with a 1.21 shape answer from
		// the shape (a pedestal lets the light in, so the item on it isn't drawn black)
		this.fullBlock = this.isOpaqueCube(this.getDefaultState());
		this.lightOpacity = this.fullBlock ? 255 : 0;
	}

	/** The drops of the block's 1.21 loot table (when the mod has one), else 1.12.2's (the block itself). */
	@Override
	public void getDrops(net.minecraft.util.NonNullList<net.minecraft.item.ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		java.util.Random random = world instanceof net.minecraft.world.World level ? level.rand : RANDOM;
		if (!io.github.xponer2.translator.loot.BlockLoot.drop(io.github.xponer2.translator.loot.BlockLoot.tableOf(this, this.properties), drops, world, pos,
				state, fortune, harvesters.get(), random)) {
			super.getDrops(drops, world, pos, state, fortune);
		}
	}

	/** The model data of the block entity here, for its model (a 1.21 model reads it in getQuads). */
	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
		IBlockState extended = super.getExtendedState(state, world, pos);
		if (this.hasTileEntity(state) && net.minecraftforge.fml.common.FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			return io.github.xponer2.translator.client.model.ModelDataState.extend(extended, world, pos);
		}
		return extended;
	}

	/** Null when a builder call couldn't run (a part of 1.21 the Translator lacks): the defaults, the block still made. */
	private static BlockProperties orDefault(@javax.annotation.Nullable BlockProperties properties) {
		return properties != null ? properties : BlockProperties.of();
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
	@SuppressWarnings("deprecation")
	public net.minecraft.block.material.EnumPushReaction getPushReaction(IBlockState state) {
		var reaction = this.properties == null ? null : this.properties.pushReaction();
		return reaction != null ? reaction : super.getPushReaction(state);
	}

	@Override
	public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, EntityLiving.SpawnPlacementType type) {
		// 1.12.2 asks by the placement type, the mod's 1.21 predicate takes the entity type, which isn't known here: none
		// (a predicate that needs it and fails lets the mob spawn; the spawner gave it a SpawnPlacementType and the world
		// crashed on its cast to EntityType)
		boolean valid;
		try {
			valid = this.properties.isValidSpawn(state, world, pos, null);
		} catch (RuntimeException e) {
			valid = true;
		}
		return valid && super.canCreatureSpawn(state, world, pos, type);
	}
}
