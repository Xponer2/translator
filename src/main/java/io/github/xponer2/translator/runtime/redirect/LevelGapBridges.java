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
package io.github.xponer2.translator.runtime.redirect;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import io.github.xponer2.translator.block.LootContextParams;
import io.github.xponer2.translator.block.LootParams;
import io.github.xponer2.translator.shape.CollisionContext;
import io.github.xponer2.translator.shape.VoxelShape;
import io.github.xponer2.translator.util.TagKey;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

/**
 * Calls of the 1.21.1 mods on the world, block states and positions that 1.12.2's classes lack, found most often left
 * empty across the mods (the conversion of all of them outside the game): shapes asked with a collision context, a
 * state's drops and tags, collisions of a box, block entities set by themselves, block updates with a recursion
 * limit, positions moved, the look of a mob, the known player profiles, an entity moved to another dimension.
 */
public final class LevelGapBridges {
	private LevelGapBridges() {
	}

	// --- block states --------------------------------------------------------------------------------------------------

	/** 1.21 {@code state.getShape(level, pos, context)}: 1.12.2's outline doesn't depend on who asks. */
	public static VoxelShape getShape(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable CollisionContext context) {
		return ApiBridges.getShape(state, world, pos);
	}

	/** 1.21 {@code state.getCollisionShape(level, pos, context)}. */
	public static VoxelShape getCollisionShape(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable CollisionContext context) {
		return ApiBridges.getCollisionShape(state, world, pos);
	}

	/** 1.21 {@code state.getTags()}: the block tags of the mods' data the state's block is in. */
	public static java.util.stream.Stream<TagKey<Block>> getTags(IBlockState state) {
		return io.github.xponer2.translator.tags.TagManager.getTags(state);
	}

	/**
	 * 1.21 {@code state.getDrops(lootParams)}: what the block drops for the position, tool and breaker the parameters
	 * name (a mod breaking blocks itself: Create's contraptions, Twilight Forest's giant pickaxe).
	 */
	public static List<ItemStack> getDrops(IBlockState state, LootParams.Builder params) {
		World world = params.getLevel();
		Vec3d origin = params.getOptionalParameter(LootContextParams.ORIGIN);
		if (!(world instanceof WorldServer) || origin == null) {
			return Collections.emptyList();
		}
		Entity breaker = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
		ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
		return BlockDrops.getDrops(state, (WorldServer) world, new BlockPos(origin), params.getOptionalParameter(LootContextParams.BLOCK_ENTITY), breaker,
				tool == null ? ItemStack.EMPTY : tool);
	}

	/** 1.21 {@code LootParams.Builder.getLevel()} as the server level it is. */
	public static WorldServer getLevel(LootParams.Builder params) {
		return (WorldServer) params.getLevel();
	}

	/** 1.21 {@code withDynamicDrop(name, drop)}: 1.12.2's loot has no dynamic drops (a shulker box's contents). */
	public static LootParams.Builder withDynamicDrop(LootParams.Builder params, net.minecraft.util.ResourceLocation name, Object drop) {
		return params;
	}

	// --- the world -----------------------------------------------------------------------------------------------------

	/** 1.21 {@code level.noCollision(box)}: nothing solid in the box. */
	public static boolean noCollision(World world, AxisAlignedBB box) {
		return world.getCollisionBoxes(null, box).isEmpty();
	}

	/** 1.21 {@code level.setBlockEntity(blockEntity)}: at the block entity's own position. */
	public static void setBlockEntity(World world, TileEntity tile) {
		if (tile != null) {
			world.setTileEntity(tile.getPos(), tile);
		}
	}

	/** NeoForge {@code markAndNotifyBlock(pos, chunk, old, new, flags, recursionLeft)}: Forge's, which has no limit to pass. */
	public static void markAndNotifyBlock(World world, BlockPos pos, @Nullable Chunk chunk, IBlockState old, IBlockState now, int flags, int recursionLeft) {
		world.markAndNotifyBlock(pos, chunk, old, now, flags);
	}

	/** 1.21 {@code addAlwaysVisibleParticle(particle, ...)}: a particle of the level (1.12.2 has no "always visible"). */
	public static void addAlwaysVisibleParticle(World world, io.github.xponer2.translator.util.particle.ParticleOptions particle, double x, double y, double z,
			double xSpeed, double ySpeed, double zSpeed) {
		io.github.xponer2.translator.util.particle.Particles.addParticle(world, particle, x, y, z, xSpeed, ySpeed, zSpeed);
	}

	public static void addAlwaysVisibleParticle(World world, io.github.xponer2.translator.util.particle.ParticleOptions particle, boolean ignoreRange, double x,
			double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		io.github.xponer2.translator.util.particle.Particles.addParticle(world, particle, x, y, z, xSpeed, ySpeed, zSpeed);
	}

	/** 1.21 {@code blockHitResult.isInside()}: whether the ray began inside the block; 1.12.2's ray trace doesn't tell. */
	public static boolean isInside(RayTraceResult hit) {
		return false;
	}

	// --- positions -----------------------------------------------------------------------------------------------------

	/** 1.21 {@code Vec3i.offset(dx, dy, dz)}: a block position stays one. */
	public static Vec3i offset(Vec3i vec, int dx, int dy, int dz) {
		if (dx == 0 && dy == 0 && dz == 0) {
			return vec;
		}
		return vec instanceof BlockPos ? ((BlockPos) vec).add(dx, dy, dz) : new Vec3i(vec.getX() + dx, vec.getY() + dy, vec.getZ() + dz);
	}

	public static Vec3i offset(Vec3i vec, Vec3i by) {
		return offset(vec, by.getX(), by.getY(), by.getZ());
	}

	/** 1.21 {@code pos.relative(axis, amount)}: moved along the axis. */
	public static BlockPos relative(BlockPos pos, net.minecraft.util.EnumFacing.Axis axis, int amount) {
		if (amount == 0) {
			return pos;
		}
		return pos.add(axis == net.minecraft.util.EnumFacing.Axis.X ? amount : 0, axis == net.minecraft.util.EnumFacing.Axis.Y ? amount : 0,
				axis == net.minecraft.util.EnumFacing.Axis.Z ? amount : 0);
	}

	// --- mobs ----------------------------------------------------------------------------------------------------------

	/** 1.21 {@code lookControl.setLookAt(entity)}: at the speeds 1.21 uses when none are given. */
	public static void setLookAt(EntityLookHelper look, Entity entity) {
		look.setLookPositionWithEntity(entity, 10.0F, 40.0F);
	}

	public static void setLookAt(EntityLookHelper look, double x, double y, double z) {
		look.setLookPosition(x, y, z, 10.0F, 40.0F);
	}

	public static void setLookAt(EntityLookHelper look, Vec3d pos) {
		look.setLookPosition(pos.x, pos.y, pos.z, 10.0F, 40.0F);
	}

	/** 1.21 {@code creeper.isPowered()}: a charged creeper. */
	public static boolean isPowered(net.minecraft.entity.monster.EntityCreeper creeper) {
		return creeper.getPowered();
	}

	/** 1.21 {@code wither.isPowered()}: the wither's armor at half health. */
	public static boolean isPowered(net.minecraft.entity.boss.EntityWither wither) {
		return wither.isArmored();
	}

	/** 1.21 {@code navigation.createPath(target, accuracy)}: 1.12.2's path to it (it has no accuracy to give). */
	@Nullable
	public static net.minecraft.pathfinding.Path createPath(net.minecraft.pathfinding.PathNavigate navigation, Entity target, int accuracy) {
		return target == null ? null : navigation.getPathToEntityLiving(target);
	}

	@Nullable
	public static net.minecraft.pathfinding.Path createPath(net.minecraft.pathfinding.PathNavigate navigation, BlockPos target, int accuracy) {
		return target == null ? null : navigation.getPathToPos(target);
	}

	@Nullable
	public static net.minecraft.pathfinding.Path createPath(net.minecraft.pathfinding.PathNavigate navigation, BlockPos target, int regionOffset, int accuracy) {
		return target == null ? null : navigation.getPathToPos(target);
	}

	@Nullable
	public static net.minecraft.pathfinding.Path createPath(net.minecraft.pathfinding.PathNavigate navigation, double x, double y, double z, int accuracy) {
		return navigation.getPathToXYZ(x, y, z);
	}

	/**
	 * 1.21 {@code path.canReach()}: whether the path ends at its target (1.12.2's path ends at the nearest point it
	 * found; its target is kept in a field without a getter on the server).
	 */
	public static boolean canReach(net.minecraft.pathfinding.Path path) {
		net.minecraft.pathfinding.PathPoint end = path.getFinalPathPoint();
		if (end == null || PATH_TARGET == null) {
			return end != null;
		}
		try {
			Object target = PATH_TARGET.get(path);
			if (!(target instanceof net.minecraft.pathfinding.PathPoint)) {
				return true;
			}
			net.minecraft.pathfinding.PathPoint point = (net.minecraft.pathfinding.PathPoint) target;
			return Math.abs(point.x - end.x) <= 1 && Math.abs(point.y - end.y) <= 1 && Math.abs(point.z - end.z) <= 1;
		} catch (IllegalAccessException e) {
			return true;
		}
	}

	private static final java.lang.reflect.Field PATH_TARGET = pathTarget();

	private static java.lang.reflect.Field pathTarget() {
		for (java.lang.reflect.Field field : net.minecraft.pathfinding.Path.class.getDeclaredFields()) {
			if (field.getType() == net.minecraft.pathfinding.PathPoint.class) {
				field.setAccessible(true);
				return field;
			}
		}
		return null;
	}

	// --- dyes ----------------------------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code dyeItem.getDyeColor()} for the stack the item came from (the converter passes it): 1.12.2 has one dye
	 * item, its 16 colours are the stack's damage. White when the stack is no dye of 1.12.2's (never null, as in 1.21).
	 */
	public static net.minecraft.item.EnumDyeColor dyeColor(ItemStack stack) {
		net.minecraft.item.EnumDyeColor color = stack == null || stack.isEmpty() ? null : MiscBridges.getColor(stack);
		return color != null ? color : net.minecraft.item.EnumDyeColor.WHITE;
	}

	// --- players -------------------------------------------------------------------------------------------------------

	/** 1.21 {@code profileCache.get(uuid)}: the profile the server knows under that id. */
	public static Optional<GameProfile> get(PlayerProfileCache cache, UUID id) {
		return Optional.ofNullable(id == null ? null : cache.getProfileByUUID(id));
	}

	/** 1.21 {@code profileCache.get(name)}. */
	public static Optional<GameProfile> get(PlayerProfileCache cache, String name) {
		return Optional.ofNullable(name == null ? null : cache.getGameProfileForUsername(name));
	}

	// --- dimensions ----------------------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code entity.changeDimension(transition)}: the entity moved to the transition's level, position and rotation
	 * (a carried 1.21 record, read by its accessors), with Forge's teleporter hook placing it. The entity in the new
	 * level, null when it couldn't move (a mod's teleporter, portal or spatial storage did nothing before).
	 */
	@Nullable
	public static Entity changeDimension(Entity entity, Object transition) {
		if (transition == null || entity.world.isRemote) {
			return null;
		}
		WorldServer level;
		Vec3d pos;
		float yaw;
		float pitch;
		try {
			level = (WorldServer) transition.getClass().getMethod("newLevel").invoke(transition);
			pos = (Vec3d) transition.getClass().getMethod("pos").invoke(transition);
			yaw = (Float) transition.getClass().getMethod("yRot").invoke(transition);
			pitch = (Float) transition.getClass().getMethod("xRot").invoke(transition);
		} catch (ReflectiveOperationException | RuntimeException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("Entity.changeDimension: the transition " + transition.getClass().getName(), e);
			return null;
		}
		if (level == null || pos == null) {
			return null;
		}
		if (level == entity.world) {
			place(entity, pos, yaw, pitch);
			return entity;
		}
		Entity moved = entity.changeDimension(level.provider.getDimension(), (world, mover, entityYaw) -> place(mover, pos, yaw, pitch));
		if (moved != null) {
			place(moved, pos, yaw, pitch);
		}
		return moved;
	}

	private static void place(Entity entity, Vec3d pos, float yaw, float pitch) {
		if (entity instanceof EntityPlayerMP && ((EntityPlayerMP) entity).connection != null) {
			((EntityPlayerMP) entity).connection.setPlayerLocation(pos.x, pos.y, pos.z, yaw, pitch);
		} else {
			entity.setLocationAndAngles(pos.x, pos.y, pos.z, yaw, pitch);
		}
	}
}
