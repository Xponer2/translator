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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.Holder;
import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.registry.ResourceKey;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

/**
 * Static stand-ins for 1.21 methods of the level and the server that the 1.12.2 classes lack (runtime,
 * neoforge-1.21.1-redirects.tsv). A parameter typed Object stands for a 1.21 type the Translator has no class for
 * (Heightmap.Types, GameEvent.Context, TickPriority): the redirect takes it as it is.
 */
public final class WorldBridges {
	private WorldBridges() {
	}

	// --- World: biomes and heights -----------------------------------------------------------------------------------

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Holder<Biome> biome(Biome biome) {
		ResourceLocation name = biome.getRegistryName();
		if (name == null) {
			return Holder.direct(biome);
		}
		return new Registry.Reference<>(ResourceKey.create((ResourceKey) ResourceKey.createRegistryKey(new ResourceLocation("worldgen/biome")), name), biome);
	}

	public static Holder<Biome> getBiome(World world, BlockPos pos) {
		return biome(world.getBiome(pos));
	}

	/** The biome at quarter coordinates (1.21 noise biomes are 4x4x4). */
	public static Holder<Biome> getUncachedNoiseBiome(World world, int x, int y, int z) {
		return biome(world.getBiome(new BlockPos(x << 2, y << 2, z << 2)));
	}

	public static Holder<Biome> getNoiseBiome(World world, int x, int y, int z) {
		return getUncachedNoiseBiome(world, x, y, z);
	}

	/** The height of a column by a 1.21 heightmap: the ocean floor ones ignore fluids, the others are the height map. */
	public static int getHeight(World world, Object heightmap, int x, int z) {
		if (String.valueOf(heightmap).contains("OCEAN_FLOOR")) {
			return world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z)).getY();
		}
		return world.getHeight(x, z);
	}

	public static BlockPos getHeightmapPos(World world, Object heightmap, BlockPos pos) {
		return new BlockPos(pos.getX(), getHeight(world, heightmap, pos.getX(), pos.getZ()), pos.getZ());
	}

	public static int getMaxLightLevel(World world) {
		return 15;
	}

	public static int getLightEmission(World world, BlockPos pos) {
		return world.getBlockState(pos).getLightValue(world, pos);
	}

	/** The shade of a face (1.21 ClientLevel.getShade): darker sideways and below. */
	public static float getShade(World world, EnumFacing face, boolean shade) {
		if (!shade) {
			return 1.0F;
		}
		switch (face) {
		case DOWN:
			return 0.5F;
		case UP:
			return 1.0F;
		case NORTH:
		case SOUTH:
			return 0.8F;
		default:
			return 0.6F;
		}
	}

	// --- World: blocks -----------------------------------------------------------------------------------------------

	public static void updateNeighborsAt(World world, BlockPos pos, Block block) {
		world.notifyNeighborsOfStateChange(pos, block, false);
	}

	public static void updateNeighborsAtExceptFromFacing(World world, BlockPos pos, Block block, EnumFacing except) {
		world.notifyNeighborsOfStateExcept(pos, block, except);
	}

	public static int getBestNeighborSignal(World world, BlockPos pos) {
		return world.getRedstonePowerFromNeighbors(pos);
	}

	public static int getSignal(World world, BlockPos pos, EnumFacing face) {
		return world.getRedstonePower(pos, face);
	}

	public static int getDirectSignalTo(World world, BlockPos pos) {
		return world.getStrongPower(pos);
	}

	public static boolean isStateAtPosition(World world, BlockPos pos, Predicate<IBlockState> test) {
		return test.test(world.getBlockState(pos));
	}

	public static boolean isWaterAt(World world, BlockPos pos) {
		return world.getBlockState(pos).getMaterial() == Material.WATER;
	}

	public static boolean destroyBlock(World world, BlockPos pos, boolean drop, @Nullable Entity entity) {
		return world.destroyBlock(pos, drop);
	}

	public static boolean destroyBlock(World world, BlockPos pos, boolean drop, @Nullable Entity entity, int recursion) {
		return world.destroyBlock(pos, drop);
	}

	public static void blockEntityChanged(World world, BlockPos pos) {
		world.markChunkDirty(pos, null);
	}

	public static void scheduleTick(World world, BlockPos pos, Block block, int delay, Object priority) {
		world.scheduleUpdate(pos, block, delay);
	}

	/** 1.21 {@code level.getBlockTicks()} and {@code getFluidTicks()}: the scheduled ticks of the world. */
	public static io.github.xponer2.translator.util.LevelTicks getBlockTicks(World world) {
		return io.github.xponer2.translator.util.LevelTicks.of(world);
	}

	public static io.github.xponer2.translator.util.LevelTicks getFluidTicks(World world) {
		return io.github.xponer2.translator.util.LevelTicks.of(world);
	}

	/** Whether the state's collision box at the position has no entity in it. */
	public static boolean isUnobstructed(World world, IBlockState state, BlockPos pos, Object context) {
		AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
		return box == null || box == Block.NULL_AABB || world.checkNoEntityCollision(box.offset(pos));
	}

	public static boolean isUnobstructed(World world, Entity entity, Object shape) {
		return true;
	}

	// --- World: chunks -----------------------------------------------------------------------------------------------

	public static boolean hasChunk(World world, int x, int z) {
		return world.getChunkProvider().getLoadedChunk(x, z) != null;
	}

	public static boolean hasChunkAt(World world, BlockPos pos) {
		return world.isBlockLoaded(pos);
	}

	@Nullable
	public static Chunk getChunk(World world, int x, int z, Object status, boolean load) {
		return load ? world.getChunk(x, z) : world.getChunkProvider().getLoadedChunk(x, z);
	}

	public static Chunk getChunkAt(World world, BlockPos pos) {
		return world.getChunk(pos);
	}

	public static World getChunkForCollisions(World world, int x, int z) {
		return world;
	}

	// --- World: entities and players ---------------------------------------------------------------------------------

	/** Entities of a 1.21 type test (an EntityType: entities of that type) in a box, matching the filter. */
	public static List<Entity> getEntities(World world, Object test, AxisAlignedBB box, Predicate<? super Entity> filter) {
		var result = new ArrayList<Entity>();
		for (Entity entity : world.getEntitiesWithinAABB(Entity.class, box)) {
			if (WorldApiBridges.typeTest(test, entity) && filter.test(entity)) {
				result.add(entity);
			}
		}
		return result;
	}

	@Nullable
	public static EntityPlayer getNearestPlayer(World world, Entity entity, double distance) {
		return world.getClosestPlayerToEntity(entity, distance);
	}

	@Nullable
	public static EntityPlayer getNearestPlayer(World world, double x, double y, double z, double distance, @Nullable Predicate<Entity> filter) {
		return world.getClosestPlayer(x, y, z, distance, filter == null ? entity -> true : filter::test);
	}

	@Nullable
	public static EntityPlayer getNearestPlayer(World world, double x, double y, double z, double distance, boolean creative) {
		return world.getClosestPlayer(x, y, z, distance, creative);
	}

	// --- World: sounds, particles, events ----------------------------------------------------------------------------

	public static void playLocalSound(World world, BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch, boolean delay) {
		world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, category, volume, pitch, delay);
	}

	public static void playLocalSound(World world, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch,
			boolean delay) {
		world.playSound(x, y, z, sound, category, volume, pitch, delay);
	}

	public static void playLocalSound(World world, Entity entity, SoundEvent sound, SoundCategory category, float volume, float pitch) {
		world.playSound(entity.posX, entity.posY, entity.posZ, sound, category, volume, pitch, false);
	}

	public static void addParticle(World world, io.github.xponer2.translator.util.particle.ParticleOptions particle, boolean force, double x, double y,
			double z, double xSpeed, double ySpeed, double zSpeed) {
		io.github.xponer2.translator.util.particle.Particles.addParticle(world, particle, x, y, z, xSpeed, ySpeed, zSpeed);
	}

	/** Game events (sculk sensors, wardens) don't exist on 1.12.2: nothing listens. */
	public static void gameEvent(World world, Holder<?> event, BlockPos pos, Object context) {
	}

	public static void gameEvent(World world, Holder<?> event, net.minecraft.util.math.Vec3d pos, Object context) {
	}

	public static void gameEvent(World world, @Nullable Entity entity, Holder<?> event, BlockPos pos) {
	}

	/** The saved data of the level (1.21 {@code level.getDataStorage()}): its 1.12.2 per-world map storage. */
	public static io.github.xponer2.translator.world.DimensionDataStorage getDataStorage(World world) {
		return io.github.xponer2.translator.world.DimensionDataStorage.of(world);
	}

	@Nullable
	public static Entity getEntity(WorldServer world, java.util.UUID id) {
		return world.getEntityFromUuid(id);
	}

	public static boolean tryAddFreshEntityWithPassengers(World world, Entity entity) {
		return world.spawnEntity(entity);
	}

	/** Chunk loading tickets of 1.21 (forceload): 1.12.2 needs a Forge ticket of the mod, none is taken. */
	public static boolean setChunkForced(WorldServer world, int x, int z, boolean forced) {
		return false;
	}

	public static boolean isPositionEntityTicking(World world, BlockPos pos) {
		return world.isBlockLoaded(pos);
	}

	public static int getSectionsCount(World world) {
		return 16;
	}

	@SuppressWarnings("unchecked")
	public static <T extends net.minecraft.tileentity.TileEntity> java.util.Optional<T> getBlockEntity(World world, BlockPos pos,
			io.github.xponer2.translator.registry.BlockEntityType<T> type) {
		var tile = world.getTileEntity(pos);
		return tile != null && type.getTileClass().isInstance(tile) ? java.util.Optional.of((T) tile) : java.util.Optional.empty();
	}

	public static io.github.xponer2.translator.util.FeatureFlagSet enabledFeatures(World world) {
		return io.github.xponer2.translator.util.FeatureFlagSet.of();
	}

	public static WorldServer getLevel(World world) {
		return (WorldServer) world;
	}

	public static long getGameTime(World world) {
		return world.getTotalWorldTime();
	}

	public static long getDayTime(World world) {
		return world.getWorldTime();
	}

	public static net.minecraft.world.EnumDifficulty getDifficulty(World world) {
		return world.getDifficulty();
	}

	// --- BlockPos ----------------------------------------------------------------------------------------------------

	public static BlockPos.MutableBlockPos mutable(BlockPos pos) {
		return new BlockPos.MutableBlockPos(pos);
	}

	public static net.minecraft.util.math.Vec3d getCenter(BlockPos pos) {
		return new net.minecraft.util.math.Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
	}

	public static net.minecraft.util.math.Vec3d getBottomCenter(BlockPos pos) {
		return new net.minecraft.util.math.Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
	}

	public static BlockPos atY(BlockPos pos, int y) {
		return new BlockPos(pos.getX(), y, pos.getZ());
	}

	public static BlockPos multiply(BlockPos pos, int factor) {
		return new BlockPos(pos.getX() * factor, pos.getY() * factor, pos.getZ() * factor);
	}

	public static String toShortString(net.minecraft.util.math.Vec3i pos) {
		return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
	}

	public static int distManhattan(net.minecraft.util.math.Vec3i pos, net.minecraft.util.math.Vec3i other) {
		return Math.abs(pos.getX() - other.getX()) + Math.abs(pos.getY() - other.getY()) + Math.abs(pos.getZ() - other.getZ());
	}

	public static boolean closerThan(net.minecraft.util.math.Vec3i pos, net.minecraft.util.math.Vec3i other, double distance) {
		return pos.distanceSq(other) < distance * distance;
	}

	public static double distToLowCornerSqr(net.minecraft.util.math.Vec3i pos, double x, double y, double z) {
		double dx = pos.getX() - x;
		double dy = pos.getY() - y;
		double dz = pos.getZ() - z;
		return dx * dx + dy * dy + dz * dz;
	}

	/** The squared distance from the block's center to a position (a 1.21 Position: a Vec3d here). */
	public static double distToCenterSqr(net.minecraft.util.math.Vec3i pos, Object position) {
		if (!(position instanceof net.minecraft.util.math.Vec3d vec)) {
			return Double.MAX_VALUE;
		}
		double dx = pos.getX() + 0.5 - vec.x;
		double dy = pos.getY() + 0.5 - vec.y;
		double dz = pos.getZ() + 0.5 - vec.z;
		return dx * dx + dy * dy + dz * dz;
	}

	public static boolean closerToCenterThan(net.minecraft.util.math.Vec3i pos, Object position, double distance) {
		return distToCenterSqr(pos, position) < distance * distance;
	}

	public static net.minecraft.util.math.Vec3d clampLocationWithin(BlockPos pos, net.minecraft.util.math.Vec3d location) {
		return new net.minecraft.util.math.Vec3d(Math.max(pos.getX(), Math.min(pos.getX() + 1, location.x)),
				Math.max(pos.getY(), Math.min(pos.getY() + 1, location.y)), Math.max(pos.getZ(), Math.min(pos.getZ() + 1, location.z)));
	}

	// --- MinecraftServer ---------------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code server.overworld()}: null while the worlds aren't loaded yet. The mods' data reload listeners run before
	 * that (Supplementaries' faucet behaviours asked for it, and 1.12.2's getWorld tried to load the overworld and threw
	 * "Cannot Hotload Dim").
	 */
	@javax.annotation.Nullable
	public static WorldServer overworld(MinecraftServer server) {
		WorldServer loaded = net.minecraftforge.common.DimensionManager.getWorld(0);
		return loaded != null ? loaded : server.worlds != null && server.worlds.length > 0 ? server.worlds[0] : null;
	}

	public static Iterable<WorldServer> getAllLevels(MinecraftServer server) {
		return server.worlds == null ? java.util.Collections.emptyList() : Arrays.asList(server.worlds);
	}

	/** A folder of the save (1.21 LevelResource: its name, "." for the save's root). */
	public static Path getWorldPath(MinecraftServer server, Object resource) {
		WorldServer overworld = net.minecraftforge.common.DimensionManager.getWorld(0);
		// before the worlds load (ServerAboutToStart: Framework links its server configs there, and getWorld(0) threw
		// "Overworld is not Loaded", so Refurbished Furniture's block entities never got their config) the save's folder
		java.io.File root = overworld != null ? overworld.getSaveHandler().getWorldDirectory()
				: server.getActiveAnvilConverter().getFile(server.getFolderName(), "level.dat").getParentFile();
		String name = resourceName(resource);
		return name == null || name.isEmpty() || ".".equals(name) ? root.toPath() : root.toPath().resolve(name);
	}

	/** The id of a 1.21 LevelResource (a record-like class with the folder name). */
	@Nullable
	private static String resourceName(Object resource) {
		try {
			var method = resource.getClass().getMethod("getId");
			return String.valueOf(method.invoke(resource));
		} catch (ReflectiveOperationException | RuntimeException e) {
			String text = String.valueOf(resource);
			return text.startsWith("/") ? text.substring(1) : text;
		}
	}

	public static Path getServerDirectory(MinecraftServer server) {
		return server.getDataDirectory().toPath();
	}

	public static boolean isSingleplayer(MinecraftServer server) {
		return server.isSinglePlayer();
	}

	public static int getTickCount(MinecraftServer server) {
		return server.getTickCounter();
	}

	public static net.minecraft.server.management.PlayerProfileCache getProfileCache(MinecraftServer server) {
		return server.getPlayerProfileCache();
	}

	public static int getOperatorUserPermissionLevel(MinecraftServer server) {
		return server.getOpPermissionLevel();
	}

	public static int getProfilePermissions(MinecraftServer server, com.mojang.authlib.GameProfile profile) {
		var entry = server.getPlayerList().getOppedPlayers().getEntry(profile);
		return entry != null ? entry.getPermissionLevel() : server.isSinglePlayer() && profile.getName().equals(server.getServerOwner()) ? 4 : 0;
	}

	@Nullable
	public static com.mojang.authlib.GameProfile getSingleplayerProfile(MinecraftServer server) {
		String owner = server.getServerOwner();
		return owner == null ? null : server.getPlayerProfileCache().getGameProfileForUsername(owner);
	}

	public static net.minecraft.world.GameRules getGameRules(MinecraftServer server) {
		return server.getWorld(0).getGameRules();
	}

	public static net.minecraft.world.gen.structure.template.TemplateManager getStructureManager(MinecraftServer server) {
		return server.getWorld(0).getStructureTemplateManager();
	}

	public static boolean isLevelEnabled(MinecraftServer server, World world) {
		return true;
	}

	public static boolean isRunning(MinecraftServer server) {
		return server.isServerRunning();
	}

	public static boolean isStopped(MinecraftServer server) {
		return server.isServerStopped();
	}

	public static boolean isSameThread(MinecraftServer server) {
		return server.isCallingFromMinecraftThread();
	}

	public static void execute(MinecraftServer server, Runnable task) {
		server.addScheduledTask(task);
	}

	public static void tell(MinecraftServer server, Runnable task) {
		server.addScheduledTask(task);
	}

	public static CompletableFuture<Void> submit(MinecraftServer server, Runnable task) {
		var future = new CompletableFuture<Void>();
		server.addScheduledTask(() -> {
			try {
				task.run();
				future.complete(null);
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		});
		return future;
	}

	/** 1.21 {@code level.getSectionIndex(y)}: the section of a height (1.12.2's sections start at 0). */
	public static int getSectionIndex(World world, int y) {
		return y >> 4;
	}
}
