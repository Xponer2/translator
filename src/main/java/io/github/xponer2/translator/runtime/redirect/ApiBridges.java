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

import java.util.Random;

import io.github.xponer2.translator.registry.ResourceKey;
import io.github.xponer2.translator.text.LiteralComponent;
import io.github.xponer2.translator.text.MutableComponent;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * The 1.21.1 methods mods use most that 1.12.2 has under another shape (tools/runtime/gap-report.md, by the number of
 * mods using them): {@code receiver.name(args)} becomes {@code ApiBridges.name(receiver, args)} (runtime,
 * neoforge-1.21.1-redirects.tsv). Each one is the 1.12.2 way of doing the same thing.
 */
public final class ApiBridges {
	private ApiBridges() {
	}

	// --- worlds and dimensions ---------------------------------------------------------------------------------------

	private static final ResourceKey<io.github.xponer2.translator.registry.Registry<World>> DIMENSION = ResourceKey.createRegistryKey(
			new ResourceLocation("dimension"));

	/** 1.21 {@code Level.OVERWORLD / NETHER / END} (Powah's ender network lives in the overworld's data). */
	public static final ResourceKey<World> OVERWORLD = dimensionKey(0);
	public static final ResourceKey<World> NETHER = dimensionKey(-1);
	public static final ResourceKey<World> END = dimensionKey(1);

	/** The 1.21 dimension key of a 1.12.2 dimension id (overworld, the_nether, the_end; others by their type name). */
	public static ResourceKey<World> dimensionKey(int id) {
		String name = switch (id) {
			case 0 -> "overworld";
			case -1 -> "the_nether";
			case 1 -> "the_end";
			default -> {
				try {
					yield DimensionType.getById(id).getName();
				} catch (RuntimeException e) {
					yield "dim" + id;
				}
			}
		};
		return ResourceKey.create(DIMENSION, name.contains(":") ? new ResourceLocation(name) : new ResourceLocation("minecraft", name));
	}

	/** The 1.12.2 dimension id of a 1.21 dimension key. */
	public static int dimensionId(ResourceKey<?> key) {
		String path = key.location().getPath();
		switch (path) {
			case "overworld":
				return 0;
			case "the_nether":
				return -1;
			case "the_end":
				return 1;
			default:
				String full = key.location().toString();
				for (DimensionType type : DimensionType.values()) {
					// a mod's dimension is named by its full id (twilightforest:twilight_forest), see DataDimensions
					if (type.getName().equals(full) || type.getName().equals(path) || ("dim" + type.getId()).equals(path)) {
						return type.getId();
					}
				}
				return 0;
		}
	}

	public static ResourceKey<World> dimension(World world) {
		return dimensionKey(world.provider.getDimension());
	}

	/** 1.21 {@code mapData.dimension} (a level key; 1.12.2's field is the dimension id): maps of Supplementaries, Moonlight, TF. */
	public static ResourceKey<World> dimension(net.minecraft.world.storage.MapData data) {
		return dimensionKey(data.dimension);
	}

	/** A write to it. */
	public static void dimension(net.minecraft.world.storage.MapData data, ResourceKey<World> key) {
		data.dimension = dimensionId(key);
	}

	public static DimensionType dimensionType(World world) {
		return world.provider.getDimensionType();
	}

	public static int getMinBuildHeight(World world) {
		return 0;
	}

	public static int getMaxBuildHeight(World world) {
		return world.getHeight();
	}

	public static Random getRandom(World world) {
		return world.rand;
	}

	public static boolean removeBlock(World world, BlockPos pos, boolean moving) {
		return world.setBlockToAir(pos);
	}

	public static io.github.xponer2.translator.recipe.RecipeManager getRecipeManager(World world) {
		return io.github.xponer2.translator.recipe.RecipeManager.get();
	}

	/** NeoForge {@code entity.getCapability(capability)}: the providers registered for the entity's type. */
	@javax.annotation.Nullable
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Object getCapability(net.minecraft.entity.Entity entity, io.github.xponer2.translator.capabilities.EntityCapability capability) {
		return capability == null ? null : capability.getCapability(entity);
	}

	@javax.annotation.Nullable
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Object getCapability(net.minecraft.entity.Entity entity, io.github.xponer2.translator.capabilities.EntityCapability capability,
			Object context) {
		return capability == null ? null : capability.getCapability(entity, context);
	}

	/** 1.21 {@code level.holderLookup(registry key)}: the game's registry of that key. */
	public static io.github.xponer2.translator.registry.HolderLookup holderLookup(World world, ResourceKey<?> key) {
		return io.github.xponer2.translator.registry.RegistryAccess.INSTANCE.lookupOrThrow((ResourceKey) key);
	}

	/**
	 * 1.21 {@code level.explode(source, x, y, z, radius, fire, interaction)}: 1.12.2's explosion; any interaction but
	 * NONE breaks blocks (the 1.21 enum is the carried class's, read by its name).
	 */
	public static net.minecraft.world.Explosion explode(World world, @javax.annotation.Nullable Entity source, double x, double y, double z, float radius,
			boolean fire, Object interaction) {
		boolean breaks = !(interaction instanceof Enum<?> kind) || !"NONE".equals(kind.name());
		return world.newExplosion(source, x, y, z, radius, fire, breaks);
	}

	public static net.minecraft.world.Explosion explode(World world, @javax.annotation.Nullable Entity source, double x, double y, double z, float radius,
			Object interaction) {
		return explode(world, source, x, y, z, radius, false, interaction);
	}

	public static net.minecraft.world.Explosion explode(World world, @javax.annotation.Nullable Entity source, Object damageSource, Object damage,
			double x, double y, double z, float radius, boolean fire, Object interaction) {
		return explode(world, source, x, y, z, radius, fire, interaction);
	}

	// --- the chunk map (1.21 ChunkMap, 1.12.2's PlayerChunkMap) ----------------------------------------------------

	/** 1.21 {@code serverChunkCache.chunkMap}: the world's player chunk map. */
	public static net.minecraft.server.management.PlayerChunkMap chunkMap(net.minecraft.world.gen.ChunkProviderServer chunks) {
		return chunks.world.getPlayerChunkMap();
	}

	/**
	 * 1.21 {@code chunkMap.generator} (read): the 1.21 generator the mods are given (WorldGapBridges.getGenerator;
	 * Twilight Forest asks its sea level so).
	 */
	@javax.annotation.Nullable
	public static Object generator(net.minecraft.server.management.PlayerChunkMap map) {
		return io.github.xponer2.translator.world.DataFeatures.generator();
	}

	/** 1.21 {@code chunkMap.getPlayers(chunk, onlyOnEdge)}: the players the chunk is sent to (whole view, not only its edge). */
	public static java.util.List<net.minecraft.entity.player.EntityPlayerMP> getPlayers(net.minecraft.server.management.PlayerChunkMap map,
			net.minecraft.util.math.ChunkPos chunk, boolean onlyOnEdge) {
		var players = new java.util.ArrayList<net.minecraft.entity.player.EntityPlayerMP>();
		for (net.minecraft.entity.player.EntityPlayer player : map.getWorldServer().playerEntities) {
			if (player instanceof net.minecraft.entity.player.EntityPlayerMP serverPlayer && map.isPlayerWatchingChunk(serverPlayer, chunk.x, chunk.z)) {
				players.add(serverPlayer);
			}
		}
		return players;
	}

	/** 1.21 {@code chunkMap.broadcast(entity, packet)}: to the players tracking the entity. */
	public static void broadcast(net.minecraft.server.management.PlayerChunkMap map, Entity entity, net.minecraft.network.Packet<?> packet) {
		map.getWorldServer().getEntityTracker().sendToTracking(entity, packet);
	}

	public static boolean isChunkTracked(net.minecraft.server.management.PlayerChunkMap map, net.minecraft.entity.player.EntityPlayerMP player, int x, int z) {
		return map.isPlayerWatchingChunk(player, x, z);
	}

	/** 1.21 {@code level.getProfiler()}: the world's profiler. */
	public static net.minecraft.profiler.Profiler getProfiler(World world) {
		return world.profiler;
	}

	/**
	 * 1.21 {@code state.getMenuProvider(level, pos)}: the block's own (a mod block that opens a menu without a block entity:
	 * Iron's Spells' arcane anvil, Twilight Forest's uncrafting table; they opened nothing, the menu was null), else the
	 * block entity there when it opens a menu (1.21's default). A block's own that asks its super is given the default.
	 */
	@javax.annotation.Nullable
	public static io.github.xponer2.translator.menu.MenuProvider getMenuProvider(net.minecraft.block.state.IBlockState state, World world,
			net.minecraft.util.math.BlockPos pos) {
		java.lang.reflect.Method own = OWN_MENU_PROVIDER.get(state.getBlock().getClass());
		if (own != null && !IN_MENU_PROVIDER.get()) {
			IN_MENU_PROVIDER.set(true);
			try {
				Object provider = own.invoke(state.getBlock(), state, world, pos);
				if (provider instanceof io.github.xponer2.translator.menu.MenuProvider menu) {
					return menu;
				}
			} catch (ReflectiveOperationException | RuntimeException e) {
				// its own failed: the block entity's, as 1.21's default
			} finally {
				IN_MENU_PROVIDER.set(false);
			}
		}
		var tile = world.getTileEntity(pos);
		return tile instanceof io.github.xponer2.translator.menu.MenuProvider provider ? provider : null;
	}

	private static final ThreadLocal<Boolean> IN_MENU_PROVIDER = ThreadLocal.withInitial(() -> false);

	/** A block class's own getMenuProvider(state, level, pos) (a 1.21.1 mod's override), null when it has none. */
	private static final ClassValue<java.lang.reflect.Method> OWN_MENU_PROVIDER = new ClassValue<java.lang.reflect.Method>() {
		@Override
		protected java.lang.reflect.Method computeValue(Class<?> type) {
			for (Class<?> current = type; current != null && current != net.minecraft.block.Block.class; current = current.getSuperclass()) {
				try {
					java.lang.reflect.Method method = current.getDeclaredMethod("getMenuProvider", net.minecraft.block.state.IBlockState.class, World.class,
							net.minecraft.util.math.BlockPos.class);
					method.setAccessible(true);
					return method;
				} catch (NoSuchMethodException e) {
					// its superclass
				} catch (RuntimeException | LinkageError e) {
					return null;
				}
			}
			return null;
		}
	};

	/** 1.21 {@code level.tickRateManager()}: 1.12.2 always ticks at 20 a second. */
	public static io.github.xponer2.translator.world.TickRateManager tickRateManager(World world) {
		return io.github.xponer2.translator.world.TickRateManager.INSTANCE;
	}

	/** The same for the server ({@code server.tickRateManager()}). */
	public static io.github.xponer2.translator.world.TickRateManager tickRateManager(MinecraftServer server) {
		return io.github.xponer2.translator.world.TickRateManager.INSTANCE;
	}

	public static net.minecraft.world.gen.ChunkProviderServer getChunkSource(World world) {
		return world instanceof WorldServer server ? server.getChunkProvider() : null;
	}

	/**
	 * 1.21 {@code ServerChunkCache.blockChanged(pos)}: the players watching the block get it again (Create's block
	 * entities send their data so).
	 */
	public static void blockChanged(net.minecraft.world.gen.ChunkProviderServer chunks, net.minecraft.util.math.BlockPos pos) {
		chunks.world.getPlayerChunkMap().markBlockForUpdate(pos);
	}

	public static WorldServer getLevel(MinecraftServer server, ResourceKey<?> key) {
		return server.getWorld(dimensionId(key));
	}

	// --- entities and players ----------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code blockPosition()}: the block the entity's feet are in (its position floored). 1.12.2's getPosition adds
	 * half a block up: a seat entity 0.7 above its chair was in the air above it, so it went away at once and nobody
	 * could sit (Macaw's chairs, sofas); anything of a mod that looks at the block of an entity was one block off.
	 */
	public static BlockPos blockPosition(Entity entity) {
		return new BlockPos(entity.posX, entity.posY, entity.posZ);
	}

	public static float getYRot(Entity entity) {
		return entity.rotationYaw;
	}

	public static float getXRot(Entity entity) {
		return entity.rotationPitch;
	}

	public static void setYRot(Entity entity, float yaw) {
		entity.rotationYaw = yaw;
	}

	public static void setXRot(Entity entity, float pitch) {
		entity.rotationPitch = pitch;
	}

	public static AxisAlignedBB getBoundingBox(Entity entity) {
		return entity.getEntityBoundingBox();
	}

	public static io.github.xponer2.translator.registry.EntityType<?> getType(Entity entity) {
		return io.github.xponer2.translator.registry.EntityType.of(entity);
	}

	public static PlayerCapabilities getAbilities(EntityPlayer player) {
		return player.capabilities;
	}

	public static WorldServer serverLevel(EntityPlayerMP player) {
		return player.getServerWorld();
	}

	// --- blocks and positions ----------------------------------------------------------------------------------------

	public static Item asItem(Block block) {
		return Item.getItemFromBlock(block);
	}

	public static long asLong(BlockPos pos) {
		return pos.toLong();
	}

	// --- text --------------------------------------------------------------------------------------------------------

	public static MutableComponent copy(ITextComponent text) {
		if (text instanceof MutableComponent mutable) {
			return mutable.copy();
		}
		return new LiteralComponent("").append(text.createCopy());
	}

	/**
	 * 1.21 {@code ComponentUtils.updateForEntity(source, text, entity, depth)}: the text as it is (selectors and scores
	 * in it stay unresolved). The carried one asks each part for 1.21's {@code resolve}, which 1.12.2's text hasn't: it
	 * gave null, and Supplementaries' sign posts, whose lines were null, crashed the server when it saved them.
	 */
	public static MutableComponent updateForEntity(@javax.annotation.Nullable Object source, @javax.annotation.Nullable ITextComponent text,
			@javax.annotation.Nullable net.minecraft.entity.Entity entity, int depth) {
		return text == null ? new LiteralComponent("") : copy(text);
	}

	/** 1.12.2 has no RGB text colors: the nearest of the 16 formatting colors. */
	public static Style withColor(Style style, int rgb) {
		TextFormatting nearest = TextFormatting.WHITE;
		long best = Long.MAX_VALUE;
		for (TextFormatting format : TextFormatting.values()) {
			if (!format.isColor()) {
				continue;
			}
			int color = COLORS[format.getColorIndex()];
			long dr = ((color >> 16) & 255) - ((rgb >> 16) & 255);
			long dg = ((color >> 8) & 255) - ((rgb >> 8) & 255);
			long db = (color & 255) - (rgb & 255);
			long distance = dr * dr + dg * dg + db * db;
			if (distance < best) {
				best = distance;
				nearest = format;
			}
		}
		return style.createShallowCopy().setColor(nearest);
	}

	// --- block states (batch 2) ----------------------------------------------------------------------------------------

	public static boolean is(net.minecraft.block.state.IBlockState state, io.github.xponer2.translator.util.TagKey<Block> tag) {
		return io.github.xponer2.translator.tags.TagManager.is(state, tag);
	}

	public static boolean canSurvive(net.minecraft.block.state.IBlockState state, World world, BlockPos pos) {
		return state.getBlock().canPlaceBlockAt(world, pos);
	}

	public static boolean isFaceSturdy(net.minecraft.block.state.IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos,
			net.minecraft.util.EnumFacing face) {
		return state.getBlockFaceShape(world, pos, face) == net.minecraft.block.state.BlockFaceShape.SOLID;
	}

	public static io.github.xponer2.translator.shape.VoxelShape getShape(net.minecraft.block.state.IBlockState state, net.minecraft.world.IBlockAccess world,
			BlockPos pos) {
		return io.github.xponer2.translator.shape.Shapes.create(state.getBoundingBox(world, pos));
	}

	public static io.github.xponer2.translator.shape.VoxelShape getCollisionShape(net.minecraft.block.state.IBlockState state,
			net.minecraft.world.IBlockAccess world, BlockPos pos) {
		AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
		return box == null ? io.github.xponer2.translator.shape.Shapes.empty() : io.github.xponer2.translator.shape.Shapes.create(box);
	}

	public static long getSeed(net.minecraft.block.state.IBlockState state, BlockPos pos) {
		return net.minecraft.util.math.MathHelper.getPositionRandom(pos);
	}

	public static net.minecraft.block.state.IBlockState rotate(net.minecraft.block.state.IBlockState state, net.minecraft.util.Rotation rotation) {
		return state.withRotation(rotation);
	}

	@SuppressWarnings("deprecation")
	public static net.minecraft.block.SoundType getSoundType(net.minecraft.block.state.IBlockState state) {
		return SoundFallback.complete(state.getBlock().getSoundType());
	}

	public static net.minecraft.block.SoundType getSoundType(net.minecraft.block.state.IBlockState state, World world, BlockPos pos, Entity entity) {
		return SoundFallback.complete(state.getBlock().getSoundType(state, world, pos, entity));
	}

	public static java.util.Collection<net.minecraft.block.properties.IProperty<?>> getProperties(net.minecraft.block.state.IBlockState state) {
		return state.getPropertyKeys();
	}

	public static Object cycle(net.minecraft.block.state.IBlockState state, net.minecraft.block.properties.IProperty<?> property) {
		return state.cycleProperty(property);
	}

	public static boolean isCollisionShapeFullBlock(net.minecraft.block.state.IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos) {
		return state.isFullCube();
	}

	public static int getLightBlock(net.minecraft.block.state.IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos) {
		return state.getLightOpacity(world, pos);
	}

	public static boolean isSolidRender(net.minecraft.block.state.IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos) {
		return state.isOpaqueCube();
	}

	public static boolean canOcclude(net.minecraft.block.state.IBlockState state) {
		return state.isOpaqueCube();
	}

	public static boolean requiresCorrectToolForDrops(net.minecraft.block.state.IBlockState state) {
		return !state.getMaterial().isToolNotRequired();
	}

	public static boolean isRedstoneConductor(net.minecraft.block.state.IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos) {
		return state.isNormalCube();
	}

	public static float getDestroyProgress(net.minecraft.block.state.IBlockState state, EntityPlayer player, net.minecraft.world.IBlockAccess world, BlockPos pos) {
		return world instanceof World level ? state.getPlayerRelativeBlockHardness(player, level, pos) : 0F;
	}

	// --- worlds (batch 2) --------------------------------------------------------------------------------------------

	public static boolean isLoaded(World world, BlockPos pos) {
		return world.isBlockLoaded(pos);
	}

	public static boolean hasNeighborSignal(World world, BlockPos pos) {
		return world.isBlockPowered(pos);
	}

	public static void scheduleTick(World world, BlockPos pos, Block block, int delay) {
		world.scheduleUpdate(pos, block, delay);
	}

	public static void scheduleTick(World world, BlockPos pos, net.minecraftforge.fluids.Fluid fluid, int delay) {
		if (fluid.getBlock() != null) {
			world.scheduleUpdate(pos, fluid.getBlock(), delay);
		}
	}

	public static <T extends Entity> java.util.List<T> getEntitiesOfClass(World world, Class<T> type, AxisAlignedBB box,
			java.util.function.Predicate<? super T> filter) {
		return world.getEntitiesWithinAABB(type, box, filter::test);
	}

	public static <T extends Entity> java.util.List<T> getEntitiesOfClass(World world, Class<T> type, AxisAlignedBB box) {
		return world.getEntitiesWithinAABB(type, box);
	}

	public static java.util.List<Entity> getEntities(World world, Entity except, AxisAlignedBB box, java.util.function.Predicate<? super Entity> filter) {
		return world.getEntitiesInAABBexcluding(except, box, filter::test);
	}

	public static java.util.List<Entity> getEntities(World world, Entity except, AxisAlignedBB box) {
		return world.getEntitiesWithinAABBExcludingEntity(except, box);
	}

	public static void blockUpdated(World world, BlockPos pos, Block block) {
		world.notifyNeighborsOfStateChange(pos, block, false);
	}

	public static java.util.List<EntityPlayer> players(World world) {
		return world.playerEntities;
	}

	public static void playSound(World world, EntityPlayer player, Entity at, net.minecraft.util.SoundEvent sound, net.minecraft.util.SoundCategory category,
			float volume, float pitch) {
		world.playSound(player, at.posX, at.posY, at.posZ, sound, category, volume, pitch);
	}

	/**
	 * 1.21 {@code level.playSound(Entity, BlockPos, sound, source, volume, pitch)}: any entity; 1.12.2 takes the player the
	 * sound isn't sent to (Macaw's cabinets opened and closed in silence).
	 */
	public static void playSound(World world, @javax.annotation.Nullable Entity entity, BlockPos pos, net.minecraft.util.SoundEvent sound,
			net.minecraft.util.SoundCategory category, float volume, float pitch) {
		world.playSound(entity instanceof EntityPlayer player ? player : null, pos, sound, category, volume, pitch);
	}

	public static void playSound(World world, @javax.annotation.Nullable Entity entity, double x, double y, double z, net.minecraft.util.SoundEvent sound,
			net.minecraft.util.SoundCategory category, float volume, float pitch) {
		world.playSound(entity instanceof EntityPlayer player ? player : null, x, y, z, sound, category, volume, pitch);
	}

	/** 1.21 {@code level.getLightEngine()}. */
	public static io.github.xponer2.translator.world.LevelLightEngine getLightEngine(World world) {
		return new io.github.xponer2.translator.world.LevelLightEngine(world);
	}

	public static void playSound(World world, EntityPlayer player, BlockPos pos, net.minecraft.util.SoundEvent sound, net.minecraft.util.SoundCategory category) {
		world.playSound(player, pos, sound, category, 1F, 1F);
	}

	public static boolean isOutsideBuildHeight(World world, int y) {
		return y < 0 || y >= world.getHeight();
	}

	public static int getRawBrightness(World world, BlockPos pos, int darkening) {
		return Math.max(world.getLightFor(net.minecraft.world.EnumSkyBlock.SKY, pos) - darkening, world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, pos));
	}

	public static boolean noCollision(World world, Entity entity, AxisAlignedBB box) {
		return world.getCollisionBoxes(entity, box).isEmpty();
	}

	/** Game events (sculk) don't exist on 1.12.2: nothing listens. */
	public static void gameEvent(World world, Entity entity, io.github.xponer2.translator.registry.Holder<?> event, BlockPos pos) {
	}

	public static void gameEvent(World world, Entity entity, io.github.xponer2.translator.registry.Holder<?> event, net.minecraft.util.math.Vec3d pos) {
	}

	public static void addAlwaysVisibleParticle(World world, io.github.xponer2.translator.util.particle.ParticleOptions particle, double x, double y,
			double z, double xSpeed, double ySpeed, double zSpeed) {
		io.github.xponer2.translator.util.particle.Particles.addParticle(world, particle, x, y, z, xSpeed, ySpeed, zSpeed);
	}

	// --- item stacks (batch 2) ---------------------------------------------------------------------------------------

	public static void hurtAndBreak(net.minecraft.item.ItemStack stack, int amount, net.minecraft.entity.EntityLivingBase entity,
			net.minecraft.inventory.EntityEquipmentSlot slot) {
		stack.damageItem(amount, entity);
	}

	public static void hurtAndBreak(net.minecraft.item.ItemStack stack, int amount, WorldServer world, EntityPlayerMP player,
			java.util.function.Consumer<Item> broken) {
		Item item = stack.getItem();
		if (stack.attemptDamageItem(amount, world.rand, player)) {
			stack.shrink(1);
			broken.accept(item);
		}
	}

	/** 1.21 {@code stack.hurtAndBreak(amount, level, entity, onBroken)}: any entity (none in creative), as the player one. */
	public static void hurtAndBreak(net.minecraft.item.ItemStack stack, int amount, WorldServer world, net.minecraft.entity.EntityLivingBase entity,
			java.util.function.Consumer<Item> broken) {
		if (entity instanceof net.minecraft.entity.player.EntityPlayer && ((net.minecraft.entity.player.EntityPlayer) entity).capabilities.isCreativeMode) {
			return;
		}
		Item item = stack.getItem();
		if (stack.attemptDamageItem(amount, world.rand, entity instanceof EntityPlayerMP ? (EntityPlayerMP) entity : null)) {
			stack.shrink(1);
			broken.accept(item);
		}
	}

	/** 1.21 {@code stack.interactLivingEntity(player, target, hand)} (the table names it interactWithEntity, 1.12.2's gives a boolean). */
	public static net.minecraft.util.EnumActionResult interactWithEntity(net.minecraft.item.ItemStack stack, net.minecraft.entity.player.EntityPlayer player,
			net.minecraft.entity.EntityLivingBase target, net.minecraft.util.EnumHand hand) {
		return stack.interactWithEntity(player, target, hand) ? net.minecraft.util.EnumActionResult.SUCCESS : net.minecraft.util.EnumActionResult.PASS;
	}

	public static net.minecraft.item.ItemStack getCraftingRemainingItem(net.minecraft.item.ItemStack stack) {
		return stack.getItem().getContainerItem(stack);
	}

	public static boolean hasCraftingRemainingItem(net.minecraft.item.ItemStack stack) {
		return stack.getItem().hasContainerItem(stack);
	}

	public static void setDamageValue(net.minecraft.item.ItemStack stack, int damage) {
		stack.setItemDamage(damage);
	}

	public static int getUseDuration(net.minecraft.item.ItemStack stack, net.minecraft.entity.EntityLivingBase entity) {
		return stack.getMaxItemUseDuration();
	}

	public static void consume(net.minecraft.item.ItemStack stack, int amount, net.minecraft.entity.EntityLivingBase entity) {
		if (!(entity instanceof EntityPlayer player && player.capabilities.isCreativeMode)) {
			stack.shrink(amount);
		}
	}

	public static void limitSize(net.minecraft.item.ItemStack stack, int max) {
		if (stack.getCount() > max) {
			stack.setCount(max);
		}
	}

	public static net.minecraft.nbt.NBTBase saveOptional(net.minecraft.item.ItemStack stack, io.github.xponer2.translator.registry.HolderLookup.Provider registries) {
		return stack.isEmpty() ? new net.minecraft.nbt.NBTTagCompound() : stack.writeToNBT(new net.minecraft.nbt.NBTTagCompound());
	}

	// --- entities and players (batch 2) --------------------------------------------------------------------------------

	public static int getId(Entity entity) {
		return entity.getEntityId();
	}

	public static net.minecraft.util.math.Vec3d getDeltaMovement(Entity entity) {
		return new net.minecraft.util.math.Vec3d(entity.motionX, entity.motionY, entity.motionZ);
	}

	public static void setDeltaMovement(Entity entity, double x, double y, double z) {
		entity.motionX = x;
		entity.motionY = y;
		entity.motionZ = z;
	}

	public static void setDeltaMovement(Entity entity, net.minecraft.util.math.Vec3d motion) {
		setDeltaMovement(entity, motion.x, motion.y, motion.z);
	}

	public static ITextComponent getName(Entity entity) {
		return new net.minecraft.util.text.TextComponentString(entity.getName());
	}

	public static float getBbWidth(Entity entity) {
		return entity.width;
	}

	public static float getBbHeight(Entity entity) {
		return entity.height;
	}

	public static void igniteForSeconds(Entity entity, float seconds) {
		entity.setFire(Math.max(1, (int) seconds));
	}

	public static void setRemainingFireTicks(Entity entity, int ticks) {
		if (ticks <= 0) {
			entity.extinguish();
		} else {
			entity.setFire(Math.max(1, ticks / 20));
		}
	}

	public static net.minecraft.nbt.NBTTagCompound getPersistentData(Entity entity) {
		return entity.getEntityData();
	}

	public static net.minecraft.util.math.Vec3d getEyePosition(Entity entity) {
		return entity.getPositionEyes(1F);
	}

	public static boolean isSpectator(Entity entity) {
		return entity instanceof EntityPlayer player && player.isSpectator();
	}

	public static boolean isRemoved(Entity entity) {
		return entity.isDead;
	}

	public static boolean isSteppingCarefully(Entity entity) {
		return entity.isSneaking();
	}

	public static boolean isSuppressingBounce(Entity entity) {
		return entity.isSneaking();
	}

	public static boolean causeFallDamage(Entity entity, float distance, float multiplier, net.minecraft.util.DamageSource source) {
		entity.fall(distance, multiplier);
		return true;
	}

	public static void makeStuckInBlock(Entity entity, net.minecraft.block.state.IBlockState state, net.minecraft.util.math.Vec3d factor) {
		entity.setInWeb();
	}

	public static double distanceToSqr(Entity entity, net.minecraft.util.math.Vec3d pos) {
		return entity.getDistanceSq(pos.x, pos.y, pos.z);
	}

	public static void playSound(Entity entity, net.minecraft.util.SoundEvent sound) {
		entity.playSound(sound, 1F, 1F);
	}

	public static Random getRandom(Entity entity) {
		return entity instanceof net.minecraft.entity.EntityLivingBase living ? living.getRNG() : entity.world.rand;
	}

	public static boolean isSwimming(Entity entity) {
		return entity.isInWater() && entity.isSprinting();
	}

	public static boolean isCrouching(EntityPlayer player) {
		return player.isSneaking();
	}

	public static boolean isSecondaryUseActive(EntityPlayer player) {
		return player.isSneaking();
	}

	public static boolean hasInfiniteMaterials(EntityPlayer player) {
		return player.capabilities.isCreativeMode;
	}

	public static boolean hasPermissions(EntityPlayer player, int level) {
		return player.canUseCommand(level, "");
	}

	public static boolean canInteractWithBlock(EntityPlayer player, BlockPos pos, double extraReach) {
		double reach = 4.5 + extraReach + 1;
		return player.getDistanceSqToCenter(pos) <= reach * reach;
	}

	public static void playNotifySound(EntityPlayer player, net.minecraft.util.SoundEvent sound, net.minecraft.util.SoundCategory category, float volume,
			float pitch) {
		player.world.playSound(null, player.posX, player.posY, player.posZ, sound, category, volume, pitch);
	}

	/** The player's inventory menu (1.21 field {@code inventoryMenu}). */
	public static net.minecraft.inventory.ContainerPlayer inventoryMenu(EntityPlayer player) {
		return (net.minecraft.inventory.ContainerPlayer) player.inventoryContainer;
	}

	public static boolean addEffect(net.minecraft.entity.EntityLivingBase entity, net.minecraft.potion.PotionEffect effect) {
		entity.addPotionEffect(effect);
		return true;
	}

	public static boolean isHolding(net.minecraft.entity.EntityLivingBase entity, Item item) {
		return entity.getHeldItemMainhand().getItem() == item || entity.getHeldItemOffhand().getItem() == item;
	}

	/** The RGB of the 16 formatting colors, by color index. */
	private static final int[] COLORS = { 0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF,
			0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };
}
