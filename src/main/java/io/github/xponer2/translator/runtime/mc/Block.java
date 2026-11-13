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

	/** 1.21 {@code onPlace(state, level, pos, oldState, movedByPiston)}: the block was set (1.12.2's onBlockAdded calls it). */
	public void onPlace(IBlockState state, World level, BlockPos pos, IBlockState oldState, boolean movedByPiston) {
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
	/**
	 * 1.21 {@code Block.isExceptionForConnection}: blocks fences, walls, bars and panes don't connect to although they
	 * are full (leaves, barrier, pumpkins, melon, shulker boxes).
	 */
	/**
	 * 1.21 {@code canSupportRigidBlock(level, pos)}: the block there has a full top face (Farmer's Delight's pies, feasts
	 * and cutting boards stand only on one; the call was missing, so none could be placed).
	 */
	public static boolean canSupportRigidBlock(IBlockAccess world, BlockPos pos) {
		IBlockState below = world.getBlockState(pos);
		return below.getBlockFaceShape(world, pos, net.minecraft.util.EnumFacing.UP) == net.minecraft.block.state.BlockFaceShape.SOLID;
	}

	/**
	 * 1.21 {@code pushEntitiesUp(from, to, level, pos)}: the entities standing in the block when it grows (path or farmland
	 * back to a full block: Farmer's Delight's rich soil) are lifted onto it. Gives the new state.
	 */
	public static IBlockState pushEntitiesUp(IBlockState from, IBlockState to, net.minecraft.world.World world, BlockPos pos) {
		net.minecraft.util.math.AxisAlignedBB box = new net.minecraft.util.math.AxisAlignedBB(pos).expand(0, 1, 0);
		for (net.minecraft.entity.Entity entity : world.getEntitiesWithinAABBExcludingEntity(null, box)) {
			double top = pos.getY() + 1.0;
			if (entity.posY < top && entity.posY >= pos.getY()) {
				entity.setPositionAndUpdate(entity.posX, top, entity.posZ);
			}
		}
		return to;
	}

	public static boolean isExceptionForConnection(IBlockState state) {
		net.minecraft.block.Block block = state.getBlock();
		return block instanceof net.minecraft.block.BlockLeaves || block instanceof net.minecraft.block.BlockShulkerBox
				|| block == Blocks.BARRIER || block == Blocks.PUMPKIN || block == Blocks.LIT_PUMPKIN || block == Blocks.MELON_BLOCK;
	}

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

	/**
	 * 1.21 {@code canBeReplaced(state, context)}: whether placing replaces this block (grass, or a flower bed taking one
	 * more flower of its own item: the mod overrides it).
	 */
	public boolean canBeReplaced(IBlockState state, BlockPlaceContext context) {
		ItemStack stack = context.getItemInHand();
		return state.getMaterial().isReplaceable() && (stack.isEmpty() || stack.getItem() != net.minecraft.item.Item.getItemFromBlock(this));
	}

	/** 1.21 {@code canSurvive(state, level, pos)}: whether the block can stay there (plants on their ground). */
	public boolean canSurvive(IBlockState state, World level, BlockPos pos) {
		return true;
	}

	/** 1.21 {@code neighborChanged(state, level, pos, block, fromPos, movedByPiston)}. */
	public void neighborChanged(IBlockState state, World level, BlockPos pos, net.minecraft.block.Block neighborBlock, BlockPos neighborPos,
			boolean movedByPiston) {
	}

	/** 1.21 scheduled tick ({@code level.scheduleTick}): 1.12.2's updateTick calls it. */
	public void tick(IBlockState state, net.minecraft.world.WorldServer level, BlockPos pos, java.util.Random random) {
	}

	/** 1.21 random tick: 1.12.2's random tick calls it when {@link #isRandomlyTicking} says so. */
	public void randomTick(IBlockState state, net.minecraft.world.WorldServer level, BlockPos pos, java.util.Random random) {
	}

	/** 1.21 {@code propagatesSkylightDown}: a shape that doesn't fill the block lets the sky light through unchanged. */
	public boolean propagatesSkylightDown(IBlockState state, IBlockAccess level, BlockPos pos) {
		return !io.github.xponer2.translator.runtime.redirect.MiscBridges.isShapeFullBlock(this.getShape(state, level, pos, CollisionContext.empty()));
	}

	/** 1.21 {@code getLightBlock}: 15 through a solid block, 0 when the sky light goes through, else 1 (leaves). */
	public int getLightBlock(IBlockState state, IBlockAccess level, BlockPos pos) {
		if (this.isOpaqueCube(state)) {
			return 15;
		}
		return this.propagatesSkylightDown(state, level, pos) ? 0 : 1;
	}

	/** The light a state takes, per state (a mod overriding the 1.21 methods is asked each time). */
	private final java.util.Map<IBlockState, Integer> lightBlock = new java.util.concurrent.ConcurrentHashMap<>();
	private Boolean ownLightBlock;

	/**
	 * 1.12.2's light opacity by the 1.21 rules: a full block that isn't solid (leaves, a mod's glass-like full blocks)
	 * dims the light by one, as in 1.21; 1.12.2 gave every block that isn't opaque none, so under a mod's trees it was
	 * as light as in the open and their leaves were lit the same inside and out.
	 */
	@Override
	@SuppressWarnings("deprecation")
	public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
		if (this.ownLightBlock == null) {
			boolean own;
			try {
				Class<?> type = this.getClass();
				own = type.getMethod("getLightBlock", IBlockState.class, IBlockAccess.class, BlockPos.class).getDeclaringClass() != Block.class
						|| type.getMethod("propagatesSkylightDown", IBlockState.class, IBlockAccess.class, BlockPos.class).getDeclaringClass() != Block.class;
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				own = false;
			}
			this.ownLightBlock = own;
		}
		int light;
		if (this.ownLightBlock && (world == null || pos == null)) {
			// asked without a world (1.12.2's getLightOpacity(state)): a block's own answer reads its block entity there
			// (Storage Drawers' drawers failed each time), the property's value instead
			light = this.lightOpacity;
		} else if (this.ownLightBlock) {
			light = this.guarded("getLightBlock", () -> this.getLightBlock(state, world, pos), this.lightOpacity);
		} else {
			// per state as 1.21 caches it: in an empty world at the origin (not null: a shape from the position failed)
			light = this.lightBlock.computeIfAbsent(state, key -> this.guarded("getLightBlock",
					() -> this.getLightBlock(key, io.github.xponer2.translator.world.EmptyBlockGetter.INSTANCE, BlockPos.ORIGIN), this.lightOpacity));
		}
		return light >= 15 ? 255 : light;
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getLightOpacity(IBlockState state) {
		return this.getLightOpacity(state, null, null);
	}

	/** 1.21 {@code isRandomlyTicking(state)}: the block's randomTicks() property. */
	public boolean isRandomlyTicking(IBlockState state) {
		return this.needsRandomTick;
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
		return super.isOpaqueCube(state) && Shapes.isFullBlock(() -> this.getShape(state, io.github.xponer2.translator.world.EmptyBlockGetter.INSTANCE, BlockPos.ORIGIN,
				CollisionContext.empty()));
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

	/**
	 * The 1.21 block's onPlace (Twilight Forest's reactor debris picks its textures there; without it the block entity
	 * saved null textures and the game crashed). 1.12.2 doesn't give the old state: air.
	 */
	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		super.onBlockAdded(world, pos, state);
		this.guarded("onPlace", () -> {
			this.onPlace(state, world, pos, Blocks.AIR.getDefaultState(), false);
			return null;
		}, null);
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

	// ticks: 1.12.2 has one updateTick for both, 1.21 a scheduled tick and a random tick

	private Boolean ownRandomTicking;

	/**
	 * Random ticks for the blocks that want them: the randomTicks() property, or a mod's isRandomlyTicking (a state
	 * that grows). 1.12.2 counts a chunk's ticking blocks by this when they're set: without it they never ticked.
	 */
	@Override
	public boolean getTickRandomly() {
		if (this.needsRandomTick) {
			return true;
		}
		if (this.ownRandomTicking == null) {
			boolean own;
			try {
				own = this.getClass().getMethod("isRandomlyTicking", IBlockState.class).getDeclaringClass() != Block.class;
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				own = false;
			}
			this.ownRandomTicking = own;
		}
		return this.ownRandomTicking;
	}

	@Override
	public void randomTick(World world, BlockPos pos, IBlockState state, java.util.Random random) {
		if (world instanceof net.minecraft.world.WorldServer server
				&& Boolean.TRUE.equals(this.guarded("isRandomlyTicking", () -> this.isRandomlyTicking(state), false))) {
			this.guarded("randomTick", () -> {
				this.randomTick(state, server, pos, random);
				return null;
			}, null);
		}
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, java.util.Random random) {
		if (world instanceof net.minecraft.world.WorldServer server) {
			this.guarded("tick", () -> {
				this.tick(state, server, pos, random);
				return null;
			}, null);
		}
	}

	/**
	 * A neighbour changed: 1.21 asks the block its new state for that side (updateShape: a plant whose ground went
	 * becomes air and breaks, a waterlogged block schedules its water), then calls its neighborChanged.
	 */
	@Override
	@SuppressWarnings("deprecation")
	public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block block, BlockPos from) {
		super.neighborChanged(state, world, pos, block, from);
		if (world.isRemote) {
			return;
		}
		EnumFacing direction = null;
		for (EnumFacing facing : EnumFacing.values()) {
			if (pos.offset(facing).equals(from)) {
				direction = facing;
			}
		}
		if (direction != null) {
			EnumFacing side = direction;
			IBlockState current = world.getBlockState(pos);
			if (current.getBlock() != this) {
				return;
			}
			IBlockState updated = this.guarded("updateShape", () -> this.updateShape(current, side, world.getBlockState(from), world, pos, from), current);
			if (updated != null && updated != current) {
				if (updated.getBlock() == Blocks.AIR) {
					world.destroyBlock(pos, true);
					return;
				}
				world.setBlockState(pos, updated, 2);
			}
		}
		if (world.getBlockState(pos).getBlock() == this) {
			this.guarded("neighborChanged", () -> {
				this.neighborChanged(world.getBlockState(pos), world, pos, block, from, false);
				return null;
			}, null);
		}
	}

	/** 1.21 placement: the block must survive there (a plant needs its ground). */
	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos) {
		return super.canPlaceBlockAt(world, pos)
				&& Boolean.TRUE.equals(this.guarded("canSurvive", () -> this.canSurvive(this.getDefaultState(), world, pos), true));
	}

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
