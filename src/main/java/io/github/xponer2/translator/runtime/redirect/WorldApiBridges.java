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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.Holder;
import io.github.xponer2.translator.registry.RegistryAccess;
import io.github.xponer2.translator.registry.ResourceKey;
import io.github.xponer2.translator.shape.Shapes;
import io.github.xponer2.translator.shape.VoxelShape;
import io.github.xponer2.translator.util.particle.ParticleOptions;
import io.github.xponer2.translator.util.particle.LegacyParticleOptions;
import io.github.xponer2.translator.world.DataDimensions;
import io.github.xponer2.translator.world.LevelLightEngine;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionHelper;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.DimensionType;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.WorldInfo;

/**
 * The 1.21.1 calls of the mods on the world classes that 1.12.2 lacks (World, WorldServer, IBlockAccess, Chunk,
 * ChunkProviderServer, DimensionType, Explosion, ChunkPos, WorldInfo), as 1.21 behaves: the dimension effects and
 * times, the brewing of a world, chunks asked with a status, suffocation and block or entity collisions, a free spot
 * for an entity, the floor height, block break particles, maps, scoreboards, the players and entities of a server
 * world, the getters of an explosion and the block positions of a chunk.
 */
public final class WorldApiBridges {
	private static final ResourceLocation OVERWORLD_EFFECTS = new ResourceLocation("minecraft", "overworld");
	private static final ResourceLocation NETHER_EFFECTS = new ResourceLocation("minecraft", "the_nether");
	private static final ResourceLocation END_EFFECTS = new ResourceLocation("minecraft", "the_end");
	private static final PotionHelper BREWING = new PotionHelper();
	private static final AtomicLong SUB_TICKS = new AtomicLong();

	private WorldApiBridges() {
	}

	// --- dimension types --------------------------------------------------------------------------------------------

	/** 1.21 {@code dimensionType.effectsLocation()}: vanilla's three, a mod dimension's type's {@code effects}, else the overworld's. */
	public static ResourceLocation effectsLocation(DimensionType type) {
		switch (type.getId()) {
			case -1:
				return NETHER_EFFECTS;
			case 1:
				return END_EFFECTS;
			case 0:
				return OVERWORLD_EFFECTS;
			default:
				ResourceLocation effects = DataDimensions.effects(type.getId());
				return effects != null ? effects : OVERWORLD_EFFECTS;
		}
	}

	/** 1.21 {@code dimensionType.timeOfDay(dayTime)}: the sun angle of that time (fixed in the Nether and the End). */
	public static float timeOfDay(DimensionType type, long dayTime) {
		long time = type == DimensionType.NETHER ? 18000L : type == DimensionType.THE_END ? 6000L : dayTime;
		double fraction = MathHelper.frac(time / 24000.0D - 0.25D);
		double eased = 0.5D - Math.cos(fraction * Math.PI) / 2.0D;
		return (float) (fraction * 2.0D + eased) / 3.0F;
	}

	/** 1.21 {@code dimensionType.monsterSpawnBlockLightLimit()}: the Nether's 15, else 0. */
	public static int monsterSpawnBlockLightLimit(DimensionType type) {
		return type == DimensionType.NETHER ? 15 : 0;
	}

	// --- world: brewing, chunks, light ------------------------------------------------------------------------------

	/** 1.21 {@code level.potionBrewing()}: 1.12.2's brewing (static, so one shared instance stands for every world). */
	public static PotionHelper potionBrewing(World world) {
		return BREWING;
	}

	/** 1.21 {@code level.getChunk(x, z, status)}: the chunk, loaded or generated (a status asks for it to exist). */
	public static Chunk getChunk(World world, int x, int z, @Nullable Object status) {
		return world.getChunk(x, z);
	}

	/** 1.21 {@code level.hasChunkAt(x, z)} with block coordinates. */
	public static boolean hasChunkAt(World world, int x, int z) {
		return world.isBlockLoaded(new BlockPos(x, 0, z));
	}

	/** 1.21 {@code serverLevel.shouldTickBlocksAt(chunkPos)}: the chunk is loaded. */
	public static boolean shouldTickBlocksAt(World world, long chunkPos) {
		return world.getChunkProvider().getLoadedChunk((int) chunkPos, (int) (chunkPos >>> 32)) != null;
	}

	/** 1.21 {@code serverLevel.areEntitiesLoaded(chunkPos)}: the chunk is loaded (1.12.2 loads its entities with it). */
	public static boolean areEntitiesLoaded(WorldServer world, long chunkPos) {
		return shouldTickBlocksAt(world, chunkPos);
	}

	/** 1.21 {@code level.gatherChunkSourceStats()}. */
	public static String gatherChunkSourceStats(World world) {
		return world.getChunkProvider().makeString();
	}

	/** 1.21 {@code worldGenLevel.setCurrentlyGenerating(supplier)}: only for crash reports. */
	public static void setCurrentlyGenerating(World world, @Nullable Supplier<String> what) {
	}

	/** 1.21 {@code blockGetter.getLightEngine()}: of the world behind it (a chunk cache's), else null. */
	@Nullable
	public static LevelLightEngine getLightEngine(IBlockAccess access) {
		World world = worldOf(access);
		return world != null ? new LevelLightEngine(world) : null;
	}

	/** 1.21 {@code blockGetter.getMaxLightLevel()}. */
	public static int getMaxLightLevel(IBlockAccess access) {
		return 15;
	}

	/** 1.21 {@code heightAccessor.isOutsideBuildHeight(y)}. */
	public static boolean isOutsideBuildHeight(IBlockAccess access, int y) {
		return y < 0 || y >= 256;
	}

	public static int getSectionIndexFromSectionY(World world, int sectionY) {
		return sectionY;
	}

	public static int getSectionYFromSectionIndex(World world, int index) {
		return index;
	}

	/** 1.21 {@code level.nextSubTickCount()}: the order of the ticks scheduled in the same game tick. */
	public static long nextSubTickCount(World world) {
		return SUB_TICKS.getAndIncrement();
	}

	@Nullable
	private static World worldOf(IBlockAccess access) {
		if (access instanceof World world) {
			return world;
		}
		if (access instanceof ChunkCache) {
			return (World) read(CHUNK_CACHE_WORLD, access);
		}
		return null;
	}

	private static final Field CHUNK_CACHE_WORLD = field(ChunkCache.class, "field_72815_e", "world");

	// --- world: collisions, free positions ----------------------------------------------------------------------------

	/** 1.21 {@code level.collidesWithSuffocatingBlock(entity, box)}: a suffocating block's collision box touches it. */
	public static boolean collidesWithSuffocatingBlock(World world, @Nullable Entity entity, AxisAlignedBB box) {
		return !blockBoxes(world, entity, box, true, true).isEmpty();
	}

	/** 1.21 {@code level.getBlockCollisions(entity, box)}: the collision shapes of the blocks in the box (loaded chunks). */
	public static Iterable<VoxelShape> getBlockCollisions(World world, @Nullable Entity entity, AxisAlignedBB box) {
		List<VoxelShape> shapes = new ArrayList<>();
		for (AxisAlignedBB hit : blockBoxes(world, entity, box, false, false)) {
			shapes.add(Shapes.create(hit));
		}
		return shapes;
	}

	/**
	 * 1.21 {@code level.getEntityCollisions(entity, box)}: the boxes of the entities in it that it collides with (boats,
	 * shulkers, minecarts for a boat).
	 */
	public static List<VoxelShape> getEntityCollisions(World world, @Nullable Entity entity, AxisAlignedBB box) {
		List<VoxelShape> shapes = new ArrayList<>();
		if (box.getAverageEdgeLength() < 1.0E-7D) {
			return shapes;
		}
		for (Entity other : world.getEntitiesWithinAABBExcludingEntity(entity, box.grow(1.0E-7D))) {
			if (entity != null && entity.isRidingSameEntity(other)) {
				continue;
			}
			AxisAlignedBB solid = other.getCollisionBoundingBox();
			if (solid == null && entity != null) {
				solid = entity.getCollisionBox(other);
			}
			if (solid != null) {
				shapes.add(Shapes.create(solid));
			}
		}
		return shapes;
	}

	/** The collision boxes of the blocks around the box that touch it (of the suffocating blocks only, if asked). */
	private static List<AxisAlignedBB> blockBoxes(World world, @Nullable Entity entity, AxisAlignedBB box, boolean suffocating, boolean firstOnly) {
		List<AxisAlignedBB> hits = new ArrayList<>();
		int minX = MathHelper.floor(box.minX - 1.0E-7D) - 1;
		int maxX = MathHelper.floor(box.maxX + 1.0E-7D) + 1;
		int minY = Math.max(0, MathHelper.floor(box.minY - 1.0E-7D) - 1);
		int maxY = Math.min(255, MathHelper.floor(box.maxY + 1.0E-7D) + 1);
		int minZ = MathHelper.floor(box.minZ - 1.0E-7D) - 1;
		int maxZ = MathHelper.floor(box.maxZ + 1.0E-7D) + 1;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				pos.setPos(x, 0, z);
				if (!world.isBlockLoaded(pos)) {
					continue;
				}
				for (int y = minY; y <= maxY; y++) {
					pos.setPos(x, y, z);
					IBlockState state = world.getBlockState(pos);
					if (suffocating && !state.causesSuffocation()) {
						continue;
					}
					state.addCollisionBoxToList(world, pos.toImmutable(), box, hits, entity, false);
					if (firstOnly && !hits.isEmpty()) {
						return hits;
					}
				}
			}
		}
		return hits;
	}

	/**
	 * 1.21 {@code level.findFreePosition(entity, shape, pos, x, y, z)}: the point of the shape nearest to pos where a box
	 * of that size fits between the blocks, or empty.
	 */
	public static Optional<Vec3d> findFreePosition(World world, @Nullable Entity entity, VoxelShape shape, Vec3d pos, double x, double y, double z) {
		if (shape.isEmpty()) {
			return Optional.empty();
		}
		List<AxisAlignedBB> parts = shape.toAabbs();
		List<AxisAlignedBB> blocked = new ArrayList<>();
		for (AxisAlignedBB hit : blockBoxes(world, entity, shape.bounds().grow(x, y, z), false, false)) {
			blocked.add(hit.grow(x / 2.0D, y / 2.0D, z / 2.0D));
		}
		double[] xs = cuts(parts, blocked, EnumFacing.Axis.X);
		double[] ys = cuts(parts, blocked, EnumFacing.Axis.Y);
		double[] zs = cuts(parts, blocked, EnumFacing.Axis.Z);
		if ((long) xs.length * ys.length * zs.length > 200_000L) {
			// too fine to split: the position itself if it's free
			return inside(parts, pos) && !inside(blocked, pos) ? Optional.of(pos) : Optional.empty();
		}
		Vec3d best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i + 1 < xs.length; i++) {
			for (int j = 0; j + 1 < ys.length; j++) {
				for (int k = 0; k + 1 < zs.length; k++) {
					Vec3d middle = new Vec3d((xs[i] + xs[i + 1]) / 2.0D, (ys[j] + ys[j + 1]) / 2.0D, (zs[k] + zs[k + 1]) / 2.0D);
					if (!inside(parts, middle) || inside(blocked, middle)) {
						continue;
					}
					Vec3d nearest = new Vec3d(MathHelper.clamp(pos.x, xs[i], xs[i + 1]), MathHelper.clamp(pos.y, ys[j], ys[j + 1]),
							MathHelper.clamp(pos.z, zs[k], zs[k + 1]));
					double distance = nearest.squareDistanceTo(pos);
					if (distance < bestDistance) {
						bestDistance = distance;
						best = nearest;
					}
				}
			}
		}
		return Optional.ofNullable(best);
	}

	private static double[] cuts(List<AxisAlignedBB> parts, List<AxisAlignedBB> blocked, EnumFacing.Axis axis) {
		java.util.TreeSet<Double> values = new java.util.TreeSet<>();
		double low = Double.MAX_VALUE;
		double high = -Double.MAX_VALUE;
		for (AxisAlignedBB part : parts) {
			low = Math.min(low, min(part, axis));
			high = Math.max(high, max(part, axis));
			values.add(min(part, axis));
			values.add(max(part, axis));
		}
		for (AxisAlignedBB box : blocked) {
			values.add(MathHelper.clamp(min(box, axis), low, high));
			values.add(MathHelper.clamp(max(box, axis), low, high));
		}
		return values.stream().mapToDouble(Double::doubleValue).toArray();
	}

	private static double min(AxisAlignedBB box, EnumFacing.Axis axis) {
		return axis == EnumFacing.Axis.X ? box.minX : axis == EnumFacing.Axis.Y ? box.minY : box.minZ;
	}

	private static double max(AxisAlignedBB box, EnumFacing.Axis axis) {
		return axis == EnumFacing.Axis.X ? box.maxX : axis == EnumFacing.Axis.Y ? box.maxY : box.maxZ;
	}

	private static boolean inside(List<AxisAlignedBB> boxes, Vec3d point) {
		for (AxisAlignedBB box : boxes) {
			if (point.x > box.minX && point.x < box.maxX && point.y > box.minY && point.y < box.maxY && point.z > box.minZ && point.z < box.maxZ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 1.21 {@code level.getBlockFloorHeight(pos)}: the top of the block's collision box there, else of the one below if
	 * it reaches into this block, else negative infinity.
	 */
	public static double getBlockFloorHeight(World world, BlockPos pos) {
		AxisAlignedBB box = world.getBlockState(pos).getCollisionBoundingBox(world, pos);
		if (box != null && box != Block.NULL_AABB) {
			return box.maxY;
		}
		BlockPos below = pos.down();
		AxisAlignedBB under = world.getBlockState(below).getCollisionBoundingBox(world, below);
		double top = under == null ? Double.NEGATIVE_INFINITY : under.maxY;
		return top >= 1.0D ? top - 1.0D : Double.NEGATIVE_INFINITY;
	}

	/** 1.21 {@code level.getBlockStatesIfLoaded(box)}: the states in the box if its chunks are loaded, else none. */
	public static Stream<IBlockState> getBlockStatesIfLoaded(World world, AxisAlignedBB box) {
		int minX = MathHelper.floor(box.minX);
		int maxX = MathHelper.floor(box.maxX);
		int minY = MathHelper.floor(box.minY);
		int maxY = MathHelper.floor(box.maxY);
		int minZ = MathHelper.floor(box.minZ);
		int maxZ = MathHelper.floor(box.maxZ);
		if (!world.isAreaLoaded(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ))) {
			return Stream.empty();
		}
		List<IBlockState> states = new ArrayList<>();
		for (BlockPos pos : BlockPos.getAllInBoxMutable(minX, minY, minZ, maxX, maxY, maxZ)) {
			states.add(world.getBlockState(pos));
		}
		return states.stream();
	}

	// --- world: effects, sounds, render -----------------------------------------------------------------------------

	/** 1.21 {@code level.addDestroyBlockEffect(pos, state)}: the break particles, on the client (nothing on a server). */
	public static void addDestroyBlockEffect(World world, BlockPos pos, IBlockState state) {
		if (world.isRemote && !state.getBlock().isAir(state, world, pos)) {
			Client.destroyEffect(pos, state);
		}
	}

	/** 1.21 {@code level.setBlocksDirty(pos, old, now)}: the block is drawn again. */
	public static void setBlocksDirty(World world, BlockPos pos, @Nullable IBlockState old, @Nullable IBlockState now) {
		world.markBlockRangeForRenderUpdate(pos, pos);
	}

	/** 1.21 {@code level.playSeededSound(player, entity, sound, source, volume, pitch, seed)}: at the entity, for all but the player. */
	public static void playSeededSound(World world, @Nullable EntityPlayer player, Entity entity, Holder<SoundEvent> sound, SoundCategory category,
			float volume, float pitch, long seed) {
		world.playSound(player, entity.posX, entity.posY, entity.posZ, sound.value(), category, volume, pitch);
	}

	// --- world: registries, maps, scoreboard ------------------------------------------------------------------------

	/** 1.21 {@code level.holder(key)}: of the world's registries. */
	public static <T> Optional<Holder<T>> holder(World world, ResourceKey<T> key) {
		return RegistryAccess.INSTANCE.holder(key);
	}

	/** 1.21 {@code level.setMapData(id, data)}: saved as 1.12.2's {@code map_<id>}. */
	public static void setMapData(World world, Object id, MapData data) {
		world.setData("map_" + mapId(id), data);
	}

	/** 1.21 {@code level.getMapData(id)}. */
	@Nullable
	public static MapData getMapData(World world, Object id) {
		return (MapData) world.loadData(MapData.class, "map_" + mapId(id));
	}

	private static int mapId(Object id) {
		if (id instanceof Integer number) {
			return number;
		}
		try {
			return (Integer) id.getClass().getMethod("id").invoke(id);
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException("not a map id: " + id, e);
		}
	}

	/** 1.21 {@code serverLevel.getScoreboard()}: the server's scoreboard. */
	public static ServerScoreboard getScoreboard(WorldServer world) {
		if (world.getScoreboard() instanceof ServerScoreboard scoreboard) {
			return scoreboard;
		}
		return (ServerScoreboard) world.getMinecraftServer().getWorld(0).getScoreboard();
	}

	// --- server world: players and entities ---------------------------------------------------------------------------

	/** 1.21 {@code serverLevel.addWithUUID(entity)}: added unless an entity of that UUID is there. */
	public static boolean addWithUUID(WorldServer world, Entity entity) {
		return world.getEntityFromUuid(entity.getUniqueID()) == null && world.spawnEntity(entity);
	}

	/** 1.21 {@code serverLevel.getRandomPlayer()}: one of the living players, or null. */
	@Nullable
	public static EntityPlayerMP getRandomPlayer(WorldServer world) {
		List<EntityPlayerMP> alive = new ArrayList<>();
		for (EntityPlayer player : world.playerEntities) {
			if (player instanceof EntityPlayerMP mp && player.isEntityAlive()) {
				alive.add(mp);
			}
		}
		return alive.isEmpty() ? null : alive.get(world.rand.nextInt(alive.size()));
	}

	/** 1.21 {@code serverLevel.getAllEntities()}. */
	public static Iterable<Entity> getAllEntities(WorldServer world) {
		return new ArrayList<>(world.loadedEntityList);
	}

	/** 1.21 {@code serverLevel.onReputationEvent(...)}: 1.12.2's villagers keep no gossip. */
	public static void onReputationEvent(WorldServer world, @Nullable Object type, @Nullable Entity target, @Nullable Object holder) {
	}

	// --- chunks -------------------------------------------------------------------------------------------------------

	/** 1.21 {@code chunk.getHeightAccessorForGeneration()}: the chunk's world. */
	public static World getHeightAccessorForGeneration(Chunk chunk) {
		return chunk.getWorld();
	}

	/** 1.21 {@code chunk.getBlockEntitiesPos()}. */
	public static Set<BlockPos> getBlockEntitiesPos(Chunk chunk) {
		return new HashSet<>(chunk.getTileEntityMap().keySet());
	}

	/** 1.21 {@code chunk.getBlockEntity(pos)}: the one there, none made. */
	@Nullable
	public static TileEntity getBlockEntity(Chunk chunk, BlockPos pos) {
		return chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
	}

	/** 1.21 {@code chunk.isUnsaved()}. */
	public static boolean isUnsaved(Chunk chunk) {
		return chunk.needsSaving(false);
	}

	public static int getSectionsCount(Chunk chunk) {
		return 16;
	}

	public static int getMinSection(Chunk chunk) {
		return 0;
	}

	public static int getMaxSection(Chunk chunk) {
		return 16;
	}

	public static int getSectionIndex(Chunk chunk, int y) {
		return y >> 4;
	}

	public static int getSectionIndexFromSectionY(Chunk chunk, int sectionY) {
		return sectionY;
	}

	public static int getSectionYFromSectionIndex(Chunk chunk, int index) {
		return index;
	}

	// --- chunk positions -------------------------------------------------------------------------------------------

	/** 1.21 {@code chunkPos.getMiddleBlockPosition(y)}. */
	public static BlockPos getMiddleBlockPosition(ChunkPos pos, int y) {
		return new BlockPos((pos.x << 4) + 7, y, (pos.z << 4) + 7);
	}

	public static int getMiddleBlockX(ChunkPos pos) {
		return (pos.x << 4) + 7;
	}

	public static int getMiddleBlockZ(ChunkPos pos) {
		return (pos.z << 4) + 7;
	}

	public static int getBlockX(ChunkPos pos, int offset) {
		return (pos.x << 4) + offset;
	}

	public static int getBlockZ(ChunkPos pos, int offset) {
		return (pos.z << 4) + offset;
	}

	/** 1.21 {@code chunkPos.getBlockAt(x, y, z)}: x and z inside the chunk. */
	public static BlockPos getBlockAt(ChunkPos pos, int x, int y, int z) {
		return new BlockPos((pos.x << 4) + x, y, (pos.z << 4) + z);
	}

	public static int getChessboardDistance(ChunkPos pos, ChunkPos other) {
		return Math.max(Math.abs(pos.x - other.x), Math.abs(pos.z - other.z));
	}

	public static int distanceSquared(ChunkPos pos, ChunkPos other) {
		int dx = pos.x - other.x;
		int dz = pos.z - other.z;
		return dx * dx + dz * dz;
	}

	/** 1.21 {@code chunkPos.distanceSquared(packed)}: the same packing as 1.12.2's {@code ChunkPos.asLong}. */
	public static int distanceSquared(ChunkPos pos, long packed) {
		int dx = pos.x - (int) packed;
		int dz = pos.z - (int) (packed >>> 32);
		return dx * dx + dz * dz;
	}

	// --- explosions -------------------------------------------------------------------------------------------------

	private static final Field EXPLODER = field(Explosion.class, "field_77283_e", "exploder");
	private static final Field SIZE = field(Explosion.class, "field_77280_f", "size");
	private static final Field DAMAGES_TERRAIN = field(Explosion.class, "field_82755_b", "damagesTerrain");

	/** 1.21 {@code explosion.getDirectSourceEntity()}: the exploding entity (the TNT, the creeper). */
	@Nullable
	public static Entity getDirectSourceEntity(Explosion explosion) {
		return (Entity) read(EXPLODER, explosion);
	}

	/** 1.21 {@code explosion.center()}. */
	public static Vec3d center(Explosion explosion) {
		return explosion.getPosition();
	}

	/** 1.21 {@code explosion.radius()}. */
	public static float radius(Explosion explosion) {
		Object size = read(SIZE, explosion);
		return size instanceof Float value ? value : 0F;
	}

	/** 1.21 {@code explosion.interactsWithBlocks()}: it breaks blocks. */
	public static boolean interactsWithBlocks(Explosion explosion) {
		return Boolean.TRUE.equals(read(DAMAGES_TERRAIN, explosion));
	}

	/** 1.21 {@code explosion.canTriggerBlocks()}: only wind charges trigger blocks; 1.12.2's explosions never do. */
	public static boolean canTriggerBlocks(Explosion explosion) {
		return false;
	}

	/** 1.21 {@code explosion.getBlockInteraction()}: DESTROY if it breaks blocks, else KEEP (the carried enum). */
	@Nullable
	public static Object getBlockInteraction(Explosion explosion) {
		try {
			@SuppressWarnings({"unchecked", "rawtypes"})
			Class<? extends Enum> type = (Class<? extends Enum>) Class.forName("net.minecraft.world.level.Explosion$BlockInteraction", true,
					net.minecraft.launchwrapper.Launch.classLoader);
			@SuppressWarnings("unchecked")
			Object value = Enum.valueOf(type, interactsWithBlocks(explosion) ? "DESTROY" : "KEEP");
			return value;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	/** 1.21 {@code explosion.getSmallExplosionParticles()}: 1.21's "explosion" puff, 1.12.2's large explosion particle. */
	public static ParticleOptions getSmallExplosionParticles(Explosion explosion) {
		return SMALL_PARTICLES;
	}

	/** 1.21 {@code explosion.getLargeExplosionParticles()}: the emitter, 1.12.2's huge explosion particle. */
	public static ParticleOptions getLargeExplosionParticles(Explosion explosion) {
		return LARGE_PARTICLES;
	}

	private static final ParticleOptions SMALL_PARTICLES = new LegacyParticleOptions(EnumParticleTypes.EXPLOSION_LARGE);
	private static final ParticleOptions LARGE_PARTICLES = new LegacyParticleOptions(EnumParticleTypes.EXPLOSION_HUGE);

	public static Holder<SoundEvent> getExplosionSound(Explosion explosion) {
		return Holder.direct(SoundEvents.ENTITY_GENERIC_EXPLODE);
	}

	@Nullable
	private static Object read(@Nullable Field field, Object owner) {
		try {
			return field != null ? field.get(owner) : null;
		} catch (IllegalAccessException e) {
			return null;
		}
	}

	/** A private 1.12.2 field by its SRG name (the game) or its MCP name (a deobfuscated game), null if neither is there. */
	@Nullable
	private static Field field(Class<?> owner, String srg, String mcp) {
		for (String name : new String[] {srg, mcp}) {
			try {
				Field field = owner.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (ReflectiveOperationException | RuntimeException e) {
				// the other name
			}
		}
		return null;
	}

	// --- world data -------------------------------------------------------------------------------------------------

	/** 1.21 {@code levelData.getGameTime()}: the total world time. */
	public static long getGameTime(WorldInfo info) {
		return info.getWorldTotalTime();
	}

	// --- neoforge: auxiliary light, day time ------------------------------------------------------------------------

	private static final java.util.Map<Chunk, Object> AUX_LIGHT = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

	/**
	 * NeoForge {@code level.getAuxLightManager(pos)}: the chunk's light of the blocks that set their own (Create's
	 * lamps), the carried NeoForge manager kept per chunk; null outside the world or if the API isn't there.
	 */
	@Nullable
	public static Object getAuxLightManager(IBlockAccess access, BlockPos pos) {
		World world = worldOf(access);
		return world == null || !world.isBlockLoaded(pos) ? null : auxLight(world.getChunk(pos));
	}

	@Nullable
	public static Object getAuxLightManager(World world, ChunkPos pos) {
		Chunk chunk = world.getChunkProvider().getLoadedChunk(pos.x, pos.z);
		return chunk == null ? null : auxLight(chunk);
	}

	@Nullable
	private static Object auxLight(Chunk chunk) {
		return AUX_LIGHT.computeIfAbsent(chunk, owner -> {
			try {
				return Class.forName("net.neoforged.neoforge.common.world.LevelChunkAuxiliaryLightManager", true, net.minecraft.launchwrapper.Launch.classLoader)
						.getConstructor(Chunk.class).newInstance(owner);
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				return null;
			}
		});
	}

	private static final java.util.Map<World, float[]> DAY_TIME = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

	/** NeoForge {@code level.getDayTimePerTick()}: -1, the vanilla day length, unless a mod set its own. */
	public static float getDayTimePerTick(World world) {
		float[] time = DAY_TIME.get(world);
		return time == null ? -1.0F : time[1];
	}

	public static void setDayTimePerTick(World world, float perTick) {
		DAY_TIME.computeIfAbsent(world, w -> new float[] {0.0F, -1.0F})[1] = perTick;
	}

	/** NeoForge {@code level.getDayTimeFraction()}: the part of a tick the day time has moved on beyond the whole ticks. */
	public static float getDayTimeFraction(World world) {
		float[] time = DAY_TIME.get(world);
		return time == null ? 0.0F : time[0];
	}

	public static void setDayTimeFraction(World world, float fraction) {
		DAY_TIME.computeIfAbsent(world, w -> new float[] {0.0F, -1.0F})[0] = fraction;
	}

	// --- light-dependent values, shade, signals ---------------------------------------------------------------------

	/** 1.21 {@code level.getLightLevelDependentMagicValue(pos)}: 1.12.2's light brightness (the same curve and ambient light). */
	public static float getLightLevelDependentMagicValue(World world, BlockPos pos) {
		return world.getLightBrightness(pos);
	}

	/** 1.21 {@code level.getPathfindingCostFromLightLevels(pos)}: monsters walk to the dark. */
	public static float getPathfindingCostFromLightLevels(World world, BlockPos pos) {
		return world.getLightBrightness(pos) - 0.5F;
	}

	/** 1.21 {@code level.getShade(direction, shade)}: the face darkening of block models (constant in the Nether). */
	public static float getShade(IBlockAccess access, EnumFacing direction, boolean shade) {
		World world = worldOf(access);
		boolean constant = world != null && world.provider.getDimension() == -1;
		if (!shade) {
			return constant ? 0.9F : 1.0F;
		}
		switch (direction) {
			case DOWN:
				return constant ? 0.9F : 0.5F;
			case UP:
				return constant ? 0.9F : 1.0F;
			case NORTH:
			case SOUTH:
				return 0.8F;
			case WEST:
			case EAST:
				return 0.6F;
			default:
				return 1.0F;
		}
	}

	/** 1.21 {@code level.getControlInputSignal(pos, direction, diodesOnly)}: the side input of a comparator or repeater. */
	public static int getControlInputSignal(World world, BlockPos pos, EnumFacing direction, boolean diodesOnly) {
		IBlockState state = world.getBlockState(pos);
		if (diodesOnly) {
			return net.minecraft.block.BlockRedstoneDiode.isDiode(state) ? world.getStrongPower(pos, direction) : 0;
		}
		if (state.getBlock() == net.minecraft.init.Blocks.REDSTONE_BLOCK) {
			return 15;
		}
		if (state.getBlock() == net.minecraft.init.Blocks.REDSTONE_WIRE) {
			return state.getValue(net.minecraft.block.BlockRedstoneWire.POWER);
		}
		return state.canProvidePower() ? world.getStrongPower(pos, direction) : 0;
	}

	// --- clips, collisions, entities --------------------------------------------------------------------------------

	/** 1.21 {@code blockGetter.clip(context)}: of the world behind it, else nothing hit. */
	@Nullable
	public static net.minecraft.util.math.RayTraceResult clip(IBlockAccess access, io.github.xponer2.translator.util.ClipContext context) {
		World world = worldOf(access);
		return world == null ? null : VanillaRedirects.clip(world, context);
	}

	/**
	 * 1.21 {@code level.clipWithInteractionOverride(from, to, pos, shape, state)}: the hit on the shape, facing the side of
	 * the interaction shape if that is hit first (a cauldron's inside).
	 */
	@Nullable
	public static net.minecraft.util.math.RayTraceResult clipWithInteractionOverride(IBlockAccess access, Vec3d from, Vec3d to, BlockPos pos,
			VoxelShape shape, IBlockState state) {
		net.minecraft.util.math.RayTraceResult hit = shape.clip(from, to, pos);
		if (hit != null) {
			net.minecraft.util.math.RayTraceResult inner = WorldGapBridges.getInteractionShape(state, access, pos).clip(from, to, pos);
			if (inner != null && inner.hitVec.subtract(from).lengthSquared() < hit.hitVec.subtract(from).lengthSquared()) {
				return new net.minecraft.util.math.RayTraceResult(hit.typeOfHit, hit.hitVec, inner.sideHit, hit.getBlockPos());
			}
		}
		return hit;
	}

	/** 1.21 {@code level.getCollisions(entity, box)}: the entities' boxes, then the blocks'. */
	public static Iterable<VoxelShape> getCollisions(World world, @Nullable Entity entity, AxisAlignedBB box) {
		List<VoxelShape> shapes = new ArrayList<>(getEntityCollisions(world, entity, box));
		for (VoxelShape shape : getBlockCollisions(world, entity, box)) {
			shapes.add(shape);
		}
		return shapes;
	}

	/** 1.21 {@code level.getEntities(test, box, filter, out, limit)}: up to limit entities of the type test in the box. */
	public static void getEntities(World world, @Nullable Object test, AxisAlignedBB box, java.util.function.Predicate<? super Entity> filter,
			List<? super Entity> out, int limit) {
		for (Entity entity : world.getEntitiesWithinAABB(Entity.class, box)) {
			if (typeTest(test, entity) && filter.test(entity)) {
				out.add(entity);
				if (out.size() >= limit) {
					return;
				}
			}
		}
	}

	/** 1.21 {@code serverLevel.getEntities(test, filter, out)}: all loaded entities of the type test. */
	public static void getEntities(WorldServer world, @Nullable Object test, java.util.function.Predicate<? super Entity> filter, List<? super Entity> out) {
		getEntities(world, test, filter, out, Integer.MAX_VALUE);
	}

	public static void getEntities(WorldServer world, @Nullable Object test, java.util.function.Predicate<? super Entity> filter, List<? super Entity> out,
			int limit) {
		for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
			if (typeTest(test, entity) && filter.test(entity)) {
				out.add(entity);
				if (out.size() >= limit) {
					return;
				}
			}
		}
	}

	/** 1.21 {@code serverLevel.getEntities(test, filter)}. */
	public static List<Entity> getEntities(WorldServer world, @Nullable Object test, java.util.function.Predicate<? super Entity> filter) {
		List<Entity> found = new ArrayList<>();
		getEntities(world, test, filter, found, Integer.MAX_VALUE);
		return found;
	}

	/**
	 * An EntityTypeTest of the mods: a 1.21 entity type (that type), a carried test ({@code tryCast} not null:
	 * {@code EntityTypeTest.forClass(Player.class)} of the carried ContainerOpenersCounter, which every chest-like block
	 * entity of the mods asks for the players near it), or none (all). WorldBridges' {@code getEntities} too.
	 */
	static boolean typeTest(@Nullable Object test, Entity entity) {
		if (test == null) {
			return true;
		}
		if (test instanceof io.github.xponer2.translator.registry.EntityType<?> type) {
			return ApiBridges.getType(entity) == type;
		}
		java.lang.reflect.Method tryCast = TRY_CAST.get(test.getClass());
		if (tryCast == null) {
			return true;
		}
		try {
			return tryCast.invoke(test, entity) != null;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return false;
		}
	}

	/** The {@code tryCast(Object)} of a carried EntityTypeTest class (null when it has none). */
	private static final ClassValue<java.lang.reflect.Method> TRY_CAST = new ClassValue<java.lang.reflect.Method>() {
		@Override
		protected java.lang.reflect.Method computeValue(Class<?> type) {
			try {
				java.lang.reflect.Method method = type.getMethod("tryCast", Object.class);
				method.setAccessible(true);
				return method;
			} catch (ReflectiveOperationException | RuntimeException e) {
				return null;
			}
		}
	};

	// --- chunk sections, chunk source ------------------------------------------------------------------------------

	/** 1.21 {@code section.setBlockState(x, y, z, state)}: the state it had. */
	public static IBlockState setBlockState(net.minecraft.world.chunk.storage.ExtendedBlockStorage section, int x, int y, int z, IBlockState state) {
		IBlockState old = section.get(x, y, z);
		section.set(x, y, z, state);
		return old;
	}

	public static IBlockState setBlockState(net.minecraft.world.chunk.storage.ExtendedBlockStorage section, int x, int y, int z, IBlockState state,
			boolean locked) {
		return setBlockState(section, x, y, z, state);
	}

	/** 1.21 {@code section.maybeHas(test)}: a section with blocks may have it. */
	public static boolean maybeHas(net.minecraft.world.chunk.storage.ExtendedBlockStorage section, java.util.function.Predicate<IBlockState> test) {
		return !section.isEmpty() || test.test(net.minecraft.init.Blocks.AIR.getDefaultState());
	}

	/** 1.21 {@code new LevelChunkSection(biomes)}: an empty section with sky light. */
	public static net.minecraft.world.chunk.storage.ExtendedBlockStorage newExtendedBlockStorage(@Nullable Object biomes) {
		return new net.minecraft.world.chunk.storage.ExtendedBlockStorage(0, true);
	}

	/** 1.21 {@code chunkSource.save(flush)}: the loaded chunks are saved. */
	public static void save(net.minecraft.world.gen.ChunkProviderServer chunks, boolean flush) {
		chunks.saveChunks(true);
	}

	// --- world data: wandering trader ----------------------------------------------------------------------------

	/** 1.21 {@code levelData.getWanderingTraderId()}: 1.12.2 has no wandering trader. */
	@Nullable
	public static java.util.UUID getWanderingTraderId(WorldInfo info) {
		return null;
	}

	public static void setWanderingTraderId(WorldInfo info, @Nullable java.util.UUID id) {
	}

	public static int getWanderingTraderSpawnChance(WorldInfo info) {
		return 25;
	}

	public static int getWanderingTraderSpawnDelay(WorldInfo info) {
		return 24000;
	}

	/** Client calls, loaded only on the client. */
	private static final class Client {
		static void destroyEffect(BlockPos pos, IBlockState state) {
			net.minecraft.client.Minecraft.getMinecraft().effectRenderer.addBlockDestroyEffects(pos, state);
		}
	}
}
