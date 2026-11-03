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

import io.github.xponer2.translator.block.BlockProperties;
import io.github.xponer2.translator.block.BlockStates;
import io.github.xponer2.translator.block.EntityBlock;
import io.github.xponer2.translator.block.PropertiesBlock;
import io.github.xponer2.translator.shape.CollisionContext;
import io.github.xponer2.translator.shape.Shapes;
import io.github.xponer2.translator.shape.VoxelShape;
import io.github.xponer2.translator.util.BlockPlaceContext;
import io.github.xponer2.translator.util.Interactions;
import io.github.xponer2.translator.util.ItemInteractionResult;

import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Facade of the 1.21 {@code Block} for original 1.21.1 mods (runtime): {@link PropertiesBlock} with the 1.21 methods a
 * mod overrides (state definition, shapes, use, placement, removal, block entities), called from the 1.12.2 methods.
 * The bridges are the ones the source converter injects into ported blocks (tools/port/rules.py INJECT).
 */
public class Block extends PropertiesBlock implements io.github.xponer2.translator.util.ItemLike {
	public Block(BlockProperties properties) {
		super(properties);
	}

	// --- 1.21 methods ---------------------------------------------------------------------------------------------

	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
	}

	public VoxelShape getShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	public VoxelShape getCollisionShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		return this.properties.hasCollision() ? this.getShape(state, level, pos, context) : Shapes.empty();
	}

	public ItemInteractionResult useItemOn(ItemStack stack, IBlockState state, World level, BlockPos pos, EntityPlayer player, EnumHand hand,
			RayTraceResult hit) {
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	public EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player, RayTraceResult hit) {
		return EnumActionResult.PASS;
	}

	@Nullable
	public IBlockState getStateForPlacement(BlockPlaceContext context) {
		return this.getDefaultState();
	}

	public void onRemove(IBlockState state, World level, BlockPos pos, IBlockState newState, boolean movedByPiston) {
	}

	@Override
	public net.minecraft.item.Item asItem() {
		return net.minecraft.item.Item.getItemFromBlock(this);
	}

	public IBlockState defaultBlockState() {
		return this.getDefaultState();
	}

	public String getDescriptionId() {
		return this.getTranslationKey();
	}

	// --- the 1.21 codecs of blocks (datapack block definitions): the properties aren't serialized ----------------------

	@SuppressWarnings("unchecked")
	public static <B extends net.minecraft.block.Block> com.mojang.serialization.codecs.RecordCodecBuilder<B, BlockProperties> propertiesCodec() {
		return com.mojang.serialization.Codec.unit(BlockProperties::of).fieldOf("properties")
				.forGetter(block -> block instanceof io.github.xponer2.translator.block.PropertiesBlock properties ? properties.getProperties() : BlockProperties.of());
	}

	public static <B extends net.minecraft.block.Block> com.mojang.serialization.MapCodec<B> simpleCodec(java.util.function.Function<BlockProperties, B> factory) {
		return com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(Block.<B>propertiesCodec()).apply(instance, factory));
	}

	// --- the 1.21 behaviour a mod block falls back on (super calls) --------------------------------------------------

	/** The state after a neighbour changed: 1.12.2 blocks compute theirs from the world (getActualState). */
	public IBlockState updateShape(IBlockState state, EnumFacing direction, IBlockState neighborState, World level, BlockPos pos, BlockPos neighborPos) {
		return state;
	}

	public io.github.xponer2.translator.util.FluidState getFluidState(IBlockState state) {
		return io.github.xponer2.translator.util.FluidState.EMPTY;
	}

	public void updateEntityAfterFallOn(IBlockAccess level, Entity entity) {
		entity.motionY = 0.0D;
	}

	public void fallOn(World level, IBlockState state, BlockPos pos, Entity entity, float fallDistance) {
		entity.fall(fallDistance, 1.0F);
	}

	public void stepOn(World level, BlockPos pos, IBlockState state, Entity entity) {
	}

	public void entityInside(IBlockState state, World level, BlockPos pos, Entity entity) {
	}

	public void attack(IBlockState state, World level, BlockPos pos, EntityPlayer player) {
	}

	/** Before the block is broken by a player (1.12.2 shows the break particles itself). */
	public IBlockState playerWillDestroy(World level, BlockPos pos, IBlockState state, EntityPlayer player) {
		return state;
	}

	public void spawnDestroyParticles(World level, EntityPlayer player, BlockPos pos, IBlockState state) {
		level.playEvent(player, 2001, pos, net.minecraft.block.Block.getStateId(state));
	}

	public void spawnAfterBreak(IBlockState state, net.minecraft.world.WorldServer level, BlockPos pos, ItemStack tool, boolean dropExperience) {
	}

	public boolean onDestroyedByPlayer(IBlockState state, World level, BlockPos pos, EntityPlayer player, boolean willHarvest,
			io.github.xponer2.translator.util.FluidState fluid) {
		return super.removedByPlayer(state, level, pos, player, willHarvest);
	}

	public float getDestroyProgress(IBlockState state, EntityPlayer player, IBlockAccess level, BlockPos pos) {
		return level instanceof World world ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
	}

	@SuppressWarnings("deprecation")
	public float getExplosionResistance(IBlockState state, IBlockAccess level, BlockPos pos, net.minecraft.world.Explosion explosion) {
		return super.getExplosionResistance(null);
	}

	public VoxelShape getBlockSupportShape(IBlockState state, IBlockAccess level, BlockPos pos) {
		return this.getCollisionShape(state, level, pos, CollisionContext.empty());
	}

	public VoxelShape getOcclusionShape(IBlockState state, IBlockAccess level, BlockPos pos) {
		return this.getShape(state, level, pos, CollisionContext.empty());
	}

	public io.github.xponer2.translator.runtime.neoforge.TriState canSustainPlant(IBlockState state, IBlockAccess level, BlockPos pos, EnumFacing facing,
			IBlockState plant) {
		return io.github.xponer2.translator.runtime.neoforge.TriState.DEFAULT;
	}

	public boolean skipRendering(IBlockState state, IBlockState adjacent, EnumFacing side) {
		return false;
	}

	/** The block's drops for a loot context (the 1.12.2 drops at the context's position). */
	public List<ItemStack> getDrops(IBlockState state, io.github.xponer2.translator.block.LootParams.Builder params) {
		return params.drops(this, state);
	}

	// --- the 1.12.2 methods, calling the 1.21 ones ----------------------------------------------------------------

	@Override
	protected BlockStateContainer createBlockState() {
		var builder = new io.github.xponer2.translator.block.LazyStateContainer.Builder(this);
		this.createBlockStateDefinition(builder);
		return builder.build();
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return BlockStates.toMeta(state);
	}

	@Override
	@SuppressWarnings("deprecation")
	public IBlockState getStateFromMeta(int meta) {
		return BlockStates.fromMeta(this, meta);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
			float hitY, float hitZ) {
		return this.guarded("use", () -> {
			var hit = Interactions.blockHit(pos, facing, hitX, hitY, hitZ);
			var itemResult = this.useItemOn(player.getHeldItem(hand), state, world, pos, player, hand, hit);
			var blockResult = itemResult == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && hand == EnumHand.MAIN_HAND
					? this.useWithoutItem(state, world, pos, player, hit) : EnumActionResult.PASS;
			return Interactions.activated(itemResult, blockResult);
		}, false);
	}

	/**
	 * A 1.12.2 call into the mod's 1.21 code: what fails there (a stubbed method returned null, ...) is logged once and
	 * the 1.12.2 default is used, instead of a crash of the game.
	 */
	private <T> T guarded(String what, java.util.function.Supplier<T> call, T fallback) {
		try {
			return call.get();
		} catch (Throwable t) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + "." + what, t);
			return fallback;
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return this.guarded("getShape", () -> Shapes.selectionBox(this.getShape(state, world, pos, CollisionContext.empty())), FULL_BLOCK_AABB);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> list,
			@Nullable Entity entity, boolean isActualState) {
		this.guarded("getCollisionShape", () -> {
			Shapes.addCollisionBoxes(this.getCollisionShape(state, world, pos, CollisionContext.of(entity)), pos, entityBox, list);
			return null;
		}, null);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isOpaqueCube(IBlockState state) {
		// asked by the 1.12.2 Block constructor before the properties are set
		return super.isOpaqueCube(state) && Shapes.isFullBlock(() -> this.getShape(state, null, null, CollisionContext.empty()));
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isFullCube(IBlockState state) {
		return this.isOpaqueCube(state);
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
		return this.isOpaqueCube(state) ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
			EntityLivingBase placer, EnumHand hand) {
		var state = this.guarded("getStateForPlacement",
				() -> this.getStateForPlacement(BlockPlaceContext.of(world, pos, facing, hitX, hitY, hitZ, placer, hand)), null);
		return state == null ? this.getDefaultState() : state;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		this.guarded("onRemove", () -> {
			this.onRemove(state, world, pos, Blocks.AIR.getDefaultState(), false);
			return null;
		}, null);
		super.breakBlock(world, pos, state);
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return this instanceof EntityBlock;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return this instanceof EntityBlock entityBlock ? this.guarded("newBlockEntity", () -> entityBlock.newBlockEntity(BlockPos.ORIGIN, state), null) : null;
	}

	// the 1.12.2 hooks of what happens to the block, calling the 1.21 methods the mod overrides

	@Override
	public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance) {
		this.guarded("fallOn", () -> {
			this.fallOn(world, world.getBlockState(pos), pos, entity, fallDistance);
			return null;
		}, null);
	}

	@Override
	public void onLanded(World world, Entity entity) {
		this.guarded("updateEntityAfterFallOn", () -> {
			this.updateEntityAfterFallOn(world, entity);
			return null;
		}, null);
	}

	@Override
	public void onEntityWalk(World world, BlockPos pos, Entity entity) {
		this.guarded("stepOn", () -> {
			this.stepOn(world, pos, world.getBlockState(pos), entity);
			return null;
		}, null);
	}

	@Override
	public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
		this.guarded("entityInside", () -> {
			this.entityInside(state, world, pos, entity);
			return null;
		}, null);
	}

	@Override
	public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
		this.guarded("attack", () -> {
			this.attack(world.getBlockState(pos), world, pos, player);
			return null;
		}, null);
	}

	@Override
	public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		this.guarded("playerWillDestroy", () -> this.playerWillDestroy(world, pos, state, player), null);
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		return this.onDestroyedByPlayer(state, world, pos, player, willHarvest, io.github.xponer2.translator.util.FluidState.EMPTY);
	}

	@Override
	@SuppressWarnings("deprecation")
	public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World world, BlockPos pos) {
		return this.guarded("getDestroyProgress", () -> this.getDestroyProgress(state, player, world, pos),
				super.getPlayerRelativeBlockHardness(state, player, world, pos));
	}

	/** The layer the block is drawn in: the mod's choice or its model's render_type, cut out when it lets light through. */
	@Override
	@net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
	public net.minecraft.util.BlockRenderLayer getRenderLayer() {
		net.minecraft.util.BlockRenderLayer layer = io.github.xponer2.translator.client.RenderLayers.of(this);
		if (layer != null) {
			return layer;
		}
		return this.properties != null && !this.properties.canOcclude() ? net.minecraft.util.BlockRenderLayer.CUTOUT
				: net.minecraft.util.BlockRenderLayer.SOLID;
	}
}
