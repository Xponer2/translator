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

import java.util.Set;

import io.github.xponer2.translator.shape.VoxelShape;
import io.github.xponer2.translator.util.ItemInteractionResult;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * World calls of 1.21.1 mods on 1.12.2 classes: a block state's support and interaction shapes and its use (the
 * block's own 1.21 code when it is a 1.21.1 mod's block, else the 1.12.2 activation), an empty chunk section, a mob's
 * goals.
 */
public final class WorldGapBridges {
	private WorldGapBridges() {
	}

	// --- BlockState -----------------------------------------------------------------------------------------------------

	public static VoxelShape getBlockSupportShape(IBlockState state, IBlockAccess world, BlockPos pos) {
		return ApiBridges.getCollisionShape(state, world, pos);
	}

	public static VoxelShape getInteractionShape(IBlockState state, IBlockAccess world, BlockPos pos) {
		return ApiBridges.getShape(state, world, pos);
	}

	/** 1.21 {@code state.useItemOn(stack, level, player, hand, hit)}. */
	public static ItemInteractionResult useItemOn(IBlockState state, ItemStack stack, World level, EntityPlayer player, EnumHand hand, RayTraceResult hit) {
		BlockPos pos = hit.getBlockPos();
		if (state.getBlock() instanceof io.github.xponer2.translator.runtime.mc.Block block) {
			return block.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		return activate(state, level, pos, player, hand, hit) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	/** 1.21 {@code state.useWithoutItem(level, player, hit)}. */
	public static EnumActionResult useWithoutItem(IBlockState state, World level, EntityPlayer player, RayTraceResult hit) {
		BlockPos pos = hit.getBlockPos();
		if (state.getBlock() instanceof io.github.xponer2.translator.runtime.mc.Block block) {
			return block.useWithoutItem(state, level, pos, player, hit);
		}
		return activate(state, level, pos, player, EnumHand.MAIN_HAND, hit) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
	}

	private static boolean activate(IBlockState state, World level, BlockPos pos, EntityPlayer player, EnumHand hand, RayTraceResult hit) {
		EnumFacing side = hit.sideHit == null ? EnumFacing.UP : hit.sideHit;
		var vec = hit.hitVec == null ? new net.minecraft.util.math.Vec3d(pos) : hit.hitVec;
		return state.getBlock().onBlockActivated(level, pos, state, player, hand, side, (float) (vec.x - pos.getX()), (float) (vec.y - pos.getY()),
				(float) (vec.z - pos.getZ()));
	}

	public static VoxelShape getFaceOcclusionShape(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
		return state.isOpaqueCube() ? ApiBridges.getShape(state, world, pos).getFaceShape(face) : io.github.xponer2.translator.shape.Shapes.empty();
	}

	public static VoxelShape getVisualShape(IBlockState state, IBlockAccess world, BlockPos pos, io.github.xponer2.translator.shape.CollisionContext context) {
		return ApiBridges.getCollisionShape(state, world, pos);
	}

	// --- entities -------------------------------------------------------------------------------------------------------

	/** 1.21 {@code lightningBolt.setVisualOnly(visual)}: 1.12.2's effect-only bolt (no fire, no damage). */
	public static void setVisualOnly(net.minecraft.entity.effect.EntityLightningBolt bolt, boolean visual) {
		setField(net.minecraft.entity.effect.EntityLightningBolt.class, "field_184529_d", "effectOnly", bolt, visual);
	}

	/** 1.21 {@code arrow.getPierceLevel()}: 1.12.2 arrows don't pierce. */
	public static byte getPierceLevel(net.minecraft.entity.projectile.EntityArrow arrow) {
		return 0;
	}

	/** 1.21 {@code hurtByTargetGoal.setAlertOthers(types)}: the mob calls others for help (1.12.2's constructor flag). */
	public static net.minecraft.entity.ai.EntityAIHurtByTarget setAlertOthers(net.minecraft.entity.ai.EntityAIHurtByTarget goal, Class<?>[] except) {
		setField(net.minecraft.entity.ai.EntityAIHurtByTarget.class, "field_75312_a", "entityCallsForHelp", goal, true);
		return goal;
	}

	// --- structures -----------------------------------------------------------------------------------------------------

	private static final java.util.Map<net.minecraft.world.gen.structure.template.PlacementSettings, BlockPos> PIVOTS = java.util.Collections
			.synchronizedMap(new java.util.WeakHashMap<>());

	/** 1.21 {@code settings.setRotationPivot(pos)}: kept for getRotationPivot (1.12.2 rotates around the origin). */
	public static net.minecraft.world.gen.structure.template.PlacementSettings setRotationPivot(
			net.minecraft.world.gen.structure.template.PlacementSettings settings, BlockPos pivot) {
		PIVOTS.put(settings, pivot);
		return settings;
	}

	public static BlockPos getRotationPivot(net.minecraft.world.gen.structure.template.PlacementSettings settings) {
		return PIVOTS.getOrDefault(settings, BlockPos.ORIGIN);
	}

	/**
	 * 1.21 {@code templateManager.getOrCreate(id)}: the 1.21 template of that name (empty when there is none), read from
	 * the data of the mods and of Minecraft 1.21.1; the manager is 1.12.2's, a handle only.
	 */
	public static net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate getOrCreate(
			net.minecraft.world.gen.structure.template.TemplateManager manager, net.minecraft.util.ResourceLocation id) {
		return io.github.xponer2.translator.world.StructureTemplates.getOrCreate(id);
	}

	/** 1.21 {@code templateManager.get(id)}: the 1.21 template of that name, if there is one. */
	public static java.util.Optional<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate> get(
			net.minecraft.world.gen.structure.template.TemplateManager manager, net.minecraft.util.ResourceLocation id) {
		return io.github.xponer2.translator.world.StructureTemplates.get(id);
	}

	/** 1.21 {@code templateManager.readStructure(tag)}: a template from its saved form. */
	public static net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate readStructure(
			net.minecraft.world.gen.structure.template.TemplateManager manager, net.minecraft.nbt.NBTTagCompound tag) {
		return io.github.xponer2.translator.world.StructureTemplates.read(tag);
	}

	// --- Vec3 and JOML ----------------------------------------------------------------------------------------------------

	/** 1.21 {@code new Vec3(vector3f)} (a constructor reference in Jade's entity sync). */
	public static net.minecraft.util.math.Vec3d newVec3d(org.joml.Vector3f vector) {
		return new net.minecraft.util.math.Vec3d(vector.x(), vector.y(), vector.z());
	}

	public static org.joml.Vector3f toVector3f(net.minecraft.util.math.Vec3d vector) {
		return new org.joml.Vector3f((float) vector.x, (float) vector.y, (float) vector.z);
	}

	// --- NeoForge capability invalidation ---------------------------------------------------------------------------------

	/**
	 * NeoForge {@code level.invalidateCapabilities(pos)}: 1.12.2 capabilities are asked for each time (there is no cache
	 * to clear), so nothing to do; the call was skipped with the rest of its method before (74 call sites).
	 */
	public static void invalidateCapabilities(World world, BlockPos pos) {
	}

	public static void invalidateCapabilities(World world, net.minecraft.util.math.ChunkPos pos) {
	}

	/** NeoForge {@code serverLevel.registerCapabilityListener(pos, listener)}: no cache, no invalidation to listen to. */
	public static void registerCapabilityListener(net.minecraft.world.WorldServer world, BlockPos pos, Object listener) {
	}

	// --- level data (WorldInfo) --------------------------------------------------------------------------------------------

	/** 1.21 {@code levelData.getSpawnPos()}: the 1.12.2 spawn coordinates. */
	public static BlockPos getSpawnPos(net.minecraft.world.storage.WorldInfo info) {
		return new BlockPos(info.getSpawnX(), info.getSpawnY(), info.getSpawnZ());
	}

	/** 1.21 {@code levelData.getSpawnAngle()}: 1.12.2 has no spawn angle. */
	public static float getSpawnAngle(net.minecraft.world.storage.WorldInfo info) {
		return 0.0F;
	}

	// --- dye colours: the 1.21 names --------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code dyeColor.getSerializedName()} / {@code getName()}: the 1.21 name, "light_gray" where 1.12.2 says
	 * "silver" (X lines). Mods make their block and item ids from it (Create's light_gray_postbox, seat, toolbox...):
	 * with "silver" their models, recipes and names didn't match.
	 */
	public static String getName(net.minecraft.item.EnumDyeColor color) {
		return color == net.minecraft.item.EnumDyeColor.SILVER ? "light_gray" : color.getName();
	}

	public static String getTranslationKey(net.minecraft.item.EnumDyeColor color) {
		return color == net.minecraft.item.EnumDyeColor.SILVER ? "light_gray" : color.getName();
	}

	// --- LazyLoadedValue ----------------------------------------------------------------------------------------------------

	/** 1.21 {@code new LazyLoadedValue(supplier)}: 1.12.2's LazyLoadBase loading from it (60 call sites: Jade, Twilight Forest...). */
	public static <T> net.minecraft.util.LazyLoadBase<T> newLazyLoadBase(java.util.function.Supplier<T> supplier) {
		return new net.minecraft.util.LazyLoadBase<T>() {
			@Override
			protected T load() {
				return supplier.get();
			}
		};
	}

	// --- mob spawns of biomes and structures -------------------------------------------------------------------------

	/**
	 * 1.21 {@code new MobSpawnSettings.SpawnerData(type, weight, min, max)}: the 1.12.2 spawn entry of the type's
	 * entity class (217 sites: Biomes O' Plenty's and Twilight Forest's biome and structure spawns).
	 */
	@SuppressWarnings("unchecked")
	public static net.minecraft.world.biome.Biome.SpawnListEntry newBiome_SpawnListEntry(io.github.xponer2.translator.registry.EntityType<?> type, int weight,
			int minCount, int maxCount) {
		return new net.minecraft.world.biome.Biome.SpawnListEntry((Class<? extends net.minecraft.entity.EntityLiving>) type.getEntityClass(), weight, minCount,
				maxCount);
	}

	/** 1.21 {@code spawnerData.type}: the type of the entry's entity class (null for one without a registration). */
	@javax.annotation.Nullable
	public static io.github.xponer2.translator.registry.EntityType<?> type(net.minecraft.world.biome.Biome.SpawnListEntry entry) {
		var registration = net.minecraftforge.fml.common.registry.EntityRegistry.getEntry(entry.entityClass);
		return registration == null ? null : io.github.xponer2.translator.registry.EntityType.of(registration);
	}

	// --- items ----------------------------------------------------------------------------------------------------------

	/** 1.21 {@code stack.canFitInsideContainerItems()}: not a shulker box (bags and belts keep them out). */
	public static boolean canFitInsideContainerItems(ItemStack stack) {
		return !(net.minecraft.block.Block.getBlockFromItem(stack.getItem()) instanceof net.minecraft.block.BlockShulkerBox);
	}

	// --- bone meal on a vanilla block ------------------------------------------------------------------------------------

	/** 1.21 {@code bonemealable.performBonemeal(level, random, pos, state)}: 1.12.2 grow (8 mods grow vanilla plants so). */
	public static void performBonemeal(net.minecraft.block.IGrowable block, net.minecraft.world.WorldServer world, java.util.Random random, BlockPos pos,
			net.minecraft.block.state.IBlockState state) {
		block.grow(world, random, pos, state);
	}

	/** 1.21 {@code bonemealable.isValidBonemealTarget(level, pos, state)}: 1.12.2 canGrow on that side. */
	public static boolean isValidBonemealTarget(net.minecraft.block.IGrowable block, World world, BlockPos pos, net.minecraft.block.state.IBlockState state) {
		return block.canGrow(world, pos, state, world.isRemote);
	}

	// --- loot tables of the data packs ---------------------------------------------------------------------------------

	/** 1.21 {@code server.reloadableRegistries()}: the loot tables by key (the world's LootTableManager). */
	public static io.github.xponer2.translator.world.ReloadableRegistries reloadableRegistries(net.minecraft.server.MinecraftServer server) {
		return new io.github.xponer2.translator.world.ReloadableRegistries(server);
	}

	// --- sounds -------------------------------------------------------------------------------------------------------------

	/** 1.21 {@code level.playSound(player, x, y, z, soundHolder, source, volume, pitch)} (60 call sites). */
	public static void playSound(World world, @javax.annotation.Nullable EntityPlayer player, double x, double y, double z,
			io.github.xponer2.translator.registry.Holder<?> sound, net.minecraft.util.SoundCategory category, float volume, float pitch) {
		if (sound != null && sound.value() instanceof net.minecraft.util.SoundEvent event) {
			world.playSound(player, x, y, z, event, category, volume, pitch);
		}
	}

	/** 1.21 {@code level.playSound(player, x, y, z, sound, source)}: volume and pitch 1. */
	public static void playSound(World world, @javax.annotation.Nullable EntityPlayer player, double x, double y, double z,
			net.minecraft.util.SoundEvent sound, net.minecraft.util.SoundCategory category) {
		world.playSound(player, x, y, z, sound, category, 1.0F, 1.0F);
	}

	// --- Vec3 record accessors as method references (Vec3::x) -----------------------------------------------------------

	public static double x(net.minecraft.util.math.Vec3d vector) {
		return vector.x;
	}

	public static double y(net.minecraft.util.math.Vec3d vector) {
		return vector.y;
	}

	public static double z(net.minecraft.util.math.Vec3d vector) {
		return vector.z;
	}

	// --- SimpleContainer --------------------------------------------------------------------------------------------------

	/** 1.21 {@code new SimpleContainer(size)}: 1.12.2's InventoryBasic without a title (9 mods' internal inventories). */
	public static net.minecraft.inventory.InventoryBasic newInventoryBasic(int size) {
		return new net.minecraft.inventory.InventoryBasic("", false, size);
	}

	public static net.minecraft.inventory.InventoryBasic newInventoryBasic(ItemStack[] stacks) {
		var inventory = new net.minecraft.inventory.InventoryBasic("", false, stacks.length);
		for (int i = 0; i < stacks.length; i++) {
			inventory.setInventorySlotContents(i, stacks[i]);
		}
		return inventory;
	}

	// --- damage sources by their 1.21 type --------------------------------------------------------------------------------

	/**
	 * 1.21 {@code new DamageSource(typeHolder[, direct[, causing[, position]]])}: the 1.12.2 source named by the type's
	 * id path ("generic", or a mod's own type), with its entities (10 mods make their damage this way).
	 */
	public static net.minecraft.util.DamageSource newDamageSource(io.github.xponer2.translator.registry.Holder<?> type) {
		return new net.minecraft.util.DamageSource(damageName(type));
	}

	public static net.minecraft.util.DamageSource newDamageSource(io.github.xponer2.translator.registry.Holder<?> type,
			@javax.annotation.Nullable net.minecraft.entity.Entity direct) {
		return direct == null ? newDamageSource(type) : new net.minecraft.util.EntityDamageSource(damageName(type), direct);
	}

	public static net.minecraft.util.DamageSource newDamageSource(io.github.xponer2.translator.registry.Holder<?> type,
			@javax.annotation.Nullable net.minecraft.entity.Entity direct, @javax.annotation.Nullable net.minecraft.entity.Entity causing) {
		if (direct == null && causing == null) {
			return newDamageSource(type);
		}
		return direct != null && causing != null && direct != causing
				? new net.minecraft.util.EntityDamageSourceIndirect(damageName(type), direct, causing)
				: new net.minecraft.util.EntityDamageSource(damageName(type), direct != null ? direct : causing);
	}

	public static net.minecraft.util.DamageSource newDamageSource(io.github.xponer2.translator.registry.Holder<?> type,
			@javax.annotation.Nullable net.minecraft.entity.Entity direct, @javax.annotation.Nullable net.minecraft.entity.Entity causing,
			@javax.annotation.Nullable net.minecraft.util.math.Vec3d position) {
		return newDamageSource(type, direct, causing);
	}

	private static String damageName(io.github.xponer2.translator.registry.Holder<?> type) {
		try {
			var key = type.unwrapKey();
			if (key.isPresent()) {
				return key.get().location().getPath();
			}
		} catch (RuntimeException e) {
			// a direct holder: its value's own name
		}
		return "generic";
	}

	// --- block state properties -------------------------------------------------------------------------------------------

	/** 1.21 {@code property.getValue(name)}: the value of that name (1.12.2 parseValue gives a Guava Optional). */
	public static <T extends Comparable<T>> java.util.Optional<T> getValue(net.minecraft.block.properties.IProperty<T> property, String name) {
		return java.util.Optional.ofNullable(property.parseValue(name).orNull());
	}

	// --- events -------------------------------------------------------------------------------------------------------------

	/** 1.21 {@code event.getDrops()}: the dropped items (a Collection in 1.21, the 1.12.2 list). */
	public static java.util.Collection<net.minecraft.entity.item.EntityItem> getDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
		return event.getDrops();
	}

	// --- crash reports -----------------------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code new CrashReportCategory(title)}: 1.12.2's takes the report it belongs to too (one of its own here). JEI
	 * describes an ingredient that crashed with one.
	 */
	public static net.minecraft.crash.CrashReportCategory newCrashReportCategory(String title) {
		return new net.minecraft.crash.CrashReportCategory(new net.minecraft.crash.CrashReport(title, new Throwable(title)), title);
	}

	/** 1.21 {@code category.setDetail(name, value)}: 1.12.2 addCrashSection (a supplier of the value: addDetail). */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static net.minecraft.crash.CrashReportCategory setDetail(net.minecraft.crash.CrashReportCategory category, String name, @javax.annotation.Nullable Object value) {
		if (value instanceof java.util.concurrent.Callable detail) {
			category.addDetail(name, () -> String.valueOf(detail.call()));
		} else {
			category.addCrashSection(name, value);
		}
		return category;
	}

	// --- ResourceLocation -----------------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code id.compareNamespaced(other)}: by namespace, then path (Mekanism sorts its QIO list with a reference to
	 * it; without one the comparator was null and Mekanism's config, so the whole mod, failed).
	 */
	public static int compareNamespaced(net.minecraft.util.ResourceLocation id, net.minecraft.util.ResourceLocation other) {
		int namespace = id.getNamespace().compareTo(other.getNamespace());
		return namespace != 0 ? namespace : id.getPath().compareTo(other.getPath());
	}

	/**
	 * 1.21 {@code settings.addProcessor(processor)}: 1.12.2 templates take their processor at placement (an
	 * ITemplateProcessor), a 1.21 one isn't applied: the template is placed as it is.
	 */
	public static net.minecraft.world.gen.structure.template.PlacementSettings addProcessor(
			net.minecraft.world.gen.structure.template.PlacementSettings settings, Object processor) {
		return settings;
	}

	// --- NBT, containers, connections ---------------------------------------------------------------------------------

	/** 1.21 {@code tag.sizeInBytes()}: the size the tag takes written (mods keep packets under a limit with it). */
	public static int sizeInBytes(net.minecraft.nbt.NBTBase tag) {
		if (tag instanceof net.minecraft.nbt.NBTTagCompound compound) {
			var bytes = new java.io.ByteArrayOutputStream();
			try (var out = new java.io.DataOutputStream(bytes)) {
				net.minecraft.nbt.CompressedStreamTools.write(compound, out);
				return bytes.size();
			} catch (java.io.IOException e) {
				// a byte array stream doesn't fail
			}
		}
		return tag.toString().length();
	}

	/** 1.21 {@code simpleContainer.getItems()}: the container's own list (changes go to it). */
	@SuppressWarnings("unchecked")
	public static net.minecraft.util.NonNullList<ItemStack> getItems(net.minecraft.inventory.InventoryBasic container) {
		for (String name : new String[] {"field_70482_c", "inventoryContents"}) {
			try {
				var field = net.minecraft.inventory.InventoryBasic.class.getDeclaredField(name);
				field.setAccessible(true);
				return (net.minecraft.util.NonNullList<ItemStack>) field.get(container);
			} catch (ReflectiveOperationException | RuntimeException e) {
				// the other name
			}
		}
		var copy = net.minecraft.util.NonNullList.withSize(container.getSizeInventory(), ItemStack.EMPTY);
		for (int i = 0; i < copy.size(); i++) {
			copy.set(i, container.getStackInSlot(i));
		}
		return copy;
	}

	public static net.minecraft.network.NetworkManager getConnection(net.minecraft.network.NetHandlerPlayServer handler) {
		return handler.getNetworkManager();
	}

	// --- text -----------------------------------------------------------------------------------------------------------

	/** 1.21 {@code style.applyTo(other)}: this style over the other (what this one leaves unset comes from the other). */
	public static net.minecraft.util.text.Style applyTo(net.minecraft.util.text.Style style, net.minecraft.util.text.Style other) {
		var result = style.createShallowCopy();
		result.setParentStyle(other);
		return result;
	}

	// --- Level ----------------------------------------------------------------------------------------------------------

	/** 1.21 {@code level.dimensionTypeRegistration()}: the holder of the world's dimension type. */
	public static io.github.xponer2.translator.registry.Holder<net.minecraft.world.DimensionType> dimensionTypeRegistration(World world) {
		return io.github.xponer2.translator.registry.Holder.direct(world.provider.getDimensionType());
	}

	private static void setField(Class<?> owner, String srg, String mcp, Object target, Object value) {
		try {
			java.lang.reflect.Field field;
			try {
				field = owner.getDeclaredField(mcp);
			} catch (NoSuchFieldException e) {
				field = owner.getDeclaredField(srg);
			}
			field.setAccessible(true);
			java.lang.reflect.Field modifiers = java.lang.reflect.Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
			modifiers.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("setting " + owner.getSimpleName() + "." + mcp, e);
		}
	}

	/** 1.21 {@code level.dayTime()}: the world time of day (ticks). */
	public static long dayTime(World world) {
		return world.getWorldTime();
	}

	public static int getLogicalHeight(World world) {
		return world.getActualHeight();
	}

	// --- ServerChunkCache -----------------------------------------------------------------------------------------------

	@javax.annotation.Nullable
	public static net.minecraft.world.chunk.Chunk getChunkNow(net.minecraft.world.gen.ChunkProviderServer chunks, int x, int z) {
		return chunks.getLoadedChunk(x, z);
	}

	/** 1.21 {@code useShapeForLightOcclusion()}: 1.12.2 lights by whole blocks, never by a block's shape. */
	public static boolean useShapeForLightOcclusion(IBlockState state) {
		return false;
	}

	/**
	 * 1.21 {@code getGenerator()}: the 1.21 ChunkGenerator, which 1.12.2 hasn't (its IChunkGenerator is another kind).
	 * The mods pass it on to TreeGrower.growTree and ConfiguredFeature.place (BOP's saplings, mushrooms and moss): a
	 * generator that only tells the heights a placed feature asks for ({@link io.github.xponer2.translator.world.DataFeatures#generator}).
	 */
	@javax.annotation.Nullable
	public static Object getGenerator(net.minecraft.world.gen.ChunkProviderServer chunks) {
		return io.github.xponer2.translator.world.DataFeatures.generator();
	}

	/** 1.21 {@code ensureCanWrite(pos)} of a world generation level: a 1.12.2 world writes anywhere (a feature placed in it). */
	public static boolean ensureCanWrite(World world, BlockPos pos) {
		return true;
	}

	/** 1.21 {@code isFluidAtPosition(pos, test)}: the test on the fluid there (empty when the block isn't a fluid). */
	public static boolean isFluidAtPosition(World world, BlockPos pos, java.util.function.Predicate<Object> test) {
		return test.test(io.github.xponer2.translator.util.FluidState.of(world.getBlockState(pos)));
	}

	/** 1.21 {@code getMinSection()}: 1.12.2 starts at section 0. */
	public static int getMinSection(World world) {
		return 0;
	}

	/** 1.21 {@code getMaxSection()}: one past the top section (16 in 1.12.2). */
	public static int getMaxSection(World world) {
		return world.getHeight() >> 4;
	}

	/**
	 * 1.21 {@code getMaxLocalRawBrightness(pos, darkening)}: the light there with the sky darkened that much (a
	 * monster's spawn check in a thunderstorm), as 1.12.2's EntityMob does it.
	 */
	public static int getMaxLocalRawBrightness(World world, BlockPos pos, int darkening) {
		int before = world.getSkylightSubtracted();
		world.setSkylightSubtracted(darkening);
		try {
			return world.getLightFromNeighbors(pos);
		} finally {
			world.setSkylightSubtracted(before);
		}
	}

	/** 1.21 {@code noCollision(entity)}: no block or entity box in the entity's way. */
	public static boolean noCollision(World world, net.minecraft.entity.Entity entity) {
		return world.getCollisionBoxes(entity, entity.getEntityBoundingBox()).isEmpty();
	}

	/** 1.21 {@code isUnobstructed(entity)}: no other entity in the entity's box. */
	public static boolean isUnobstructed(World world, net.minecraft.entity.Entity entity) {
		return world.checkNoEntityCollision(entity.getEntityBoundingBox(), entity);
	}

	/** 1.21 {@code getMoonBrightness()}: the moon phase's brightness (1 full moon .. 0 new moon). */
	public static float getMoonBrightness(World world) {
		return world.getCurrentMoonPhaseFactor();
	}

	/** 1.21 {@code hasNearbyAlivePlayer(x, y, z, range)}: a player (not a spectator) within the range. */
	public static boolean hasNearbyAlivePlayer(World world, double x, double y, double z, double range) {
		return world.isAnyPlayerWithinRangeAt(x, y, z, range);
	}

	/** 1.21 {@code addFreshEntityWithPassengers(entity)}: the entity and its passengers in the world. */
	public static void addFreshEntityWithPassengers(World world, net.minecraft.entity.Entity entity) {
		world.spawnEntity(entity);
		for (net.minecraft.entity.Entity one : entity.getRecursivePassengers()) {
			world.spawnEntity(one);
		}
	}

	/** 1.21 {@code isPositionTicking(chunkPos)}: the chunk (packed x, z) is loaded. */
	public static boolean isPositionTicking(net.minecraft.world.gen.ChunkProviderServer chunks, long chunkPos) {
		return chunks.chunkExists((int) chunkPos, (int) (chunkPos >> 32));
	}

	// --- containers -----------------------------------------------------------------------------------------------------

	public static boolean hasAnyMatching(net.minecraft.inventory.IInventory inventory, java.util.function.Predicate<ItemStack> test) {
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (!stack.isEmpty() && test.test(stack)) {
				return true;
			}
		}
		return false;
	}

	/** 1.21 {@code container.canTakeItem(target, slot, stack)}: 1.12.2 inventories don't refuse it. */
	public static boolean canTakeItem(net.minecraft.inventory.IInventory inventory, net.minecraft.inventory.IInventory target, int slot, ItemStack stack) {
		return true;
	}

	// --- damage, food, attributes ---------------------------------------------------------------------------------------

	/** 1.21 {@code source.isDirect()}: the entity that hit is the one that caused it (no projectile between). */
	public static boolean isDirect(net.minecraft.util.DamageSource source) {
		return source.getImmediateSource() == source.getTrueSource();
	}

	public static void setExhaustion(net.minecraft.util.FoodStats food, float exhaustion) {
		setField(net.minecraft.util.FoodStats.class, "field_75126_c", "foodExhaustionLevel", food, exhaustion);
	}

	public static net.minecraft.nbt.NBTTagCompound save(net.minecraft.entity.ai.attributes.AttributeModifier modifier) {
		return net.minecraft.entity.SharedMonsterAttributes.writeAttributeModifierToNBT(modifier);
	}

	/**
	 * 1.21 {@code stack.forEachModifier(slotGroup, action)}: the stack's attribute modifiers in the slots of the group,
	 * as 1.21 holders and modifiers (1.12.2 gives them by attribute name per slot).
	 */
	public static void forEachModifier(ItemStack stack, io.github.xponer2.translator.entity.EquipmentSlotGroup group,
			java.util.function.BiConsumer<io.github.xponer2.translator.registry.Holder<io.github.xponer2.translator.entity.Attribute>,
					io.github.xponer2.translator.entity.AttributeModifier> action) {
		var seen = new java.util.HashSet<java.util.UUID>();
		for (net.minecraft.inventory.EntityEquipmentSlot slot : net.minecraft.inventory.EntityEquipmentSlot.values()) {
			if (group.test(slot)) {
				modifiers(stack, slot, seen, action);
			}
		}
	}

	public static void forEachModifier(ItemStack stack, net.minecraft.inventory.EntityEquipmentSlot slot,
			java.util.function.BiConsumer<io.github.xponer2.translator.registry.Holder<io.github.xponer2.translator.entity.Attribute>,
					io.github.xponer2.translator.entity.AttributeModifier> action) {
		modifiers(stack, slot, new java.util.HashSet<>(), action);
	}

	private static void modifiers(ItemStack stack, net.minecraft.inventory.EntityEquipmentSlot slot, Set<java.util.UUID> seen,
			java.util.function.BiConsumer<io.github.xponer2.translator.registry.Holder<io.github.xponer2.translator.entity.Attribute>,
					io.github.xponer2.translator.entity.AttributeModifier> action) {
		var operations = io.github.xponer2.translator.entity.AttributeModifier.Operation.values();
		for (var entry : stack.getAttributeModifiers(slot).entries()) {
			var modifier = entry.getValue();
			if (!seen.add(modifier.getID())) {
				continue;
			}
			String path = modifier.getName().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
			action.accept(io.github.xponer2.translator.entity.Attributes.byForgeName(entry.getKey()),
					new io.github.xponer2.translator.entity.AttributeModifier(new net.minecraft.util.ResourceLocation("minecraft",
							path.isEmpty() ? modifier.getID().toString() : path), modifier.getAmount(),
							operations[Math.max(0, Math.min(operations.length - 1, modifier.getOperation()))]));
		}
	}

	/** 1.21 {@code goalSelector.availableGoals}: the 1.12.2 task entries. */
	public static Set<EntityAITasks.EntityAITaskEntry> availableGoals(EntityAITasks tasks) {
		return tasks.taskEntries;
	}

	/** 1.21 {@code liquidBlock.fluid}: the fluid of a 1.21.1 mod's fluid block. */
	public static net.minecraftforge.fluids.Fluid fluid(io.github.xponer2.translator.runtime.mc.LiquidBlock block) {
		return block.getFluid();
	}

	// --- profiler -------------------------------------------------------------------------------------------------------

	/** 1.12.2's profiler has no tick bounds. */
	public static void startTick(net.minecraft.profiler.Profiler profiler) {
	}

	public static void endTick(net.minecraft.profiler.Profiler profiler) {
	}

	// --- StackedContents -----------------------------------------------------------------------------------------------

	/** 1.21 {@code stackedContents.accountSimpleStack(stack)}: a stack without data counted for recipe matching. */
	public static void accountSimpleStack(net.minecraft.client.util.RecipeItemHelper contents, ItemStack stack) {
		contents.accountStack(stack);
	}

	// --- LevelChunkSection ----------------------------------------------------------------------------------------------

	public static boolean hasOnlyAir(ExtendedBlockStorage section) {
		return section.isEmpty();
	}

	// --- GoalSelector ---------------------------------------------------------------------------------------------------

	/** 1.21 {@code goalSelector.getAvailableGoals()}: the 1.12.2 task entries (each with its goal and priority). */
	public static Set<EntityAITasks.EntityAITaskEntry> getAvailableGoals(EntityAITasks tasks) {
		return tasks.taskEntries;
	}

	/** 1.21 {@code section.acquire()} / {@code release()}: the lock of a section 1.12.2 hasn't. */
	public static void acquire(@javax.annotation.Nullable ExtendedBlockStorage section) {
	}

	public static void release(@javax.annotation.Nullable ExtendedBlockStorage section) {
	}

	/**
	 * 1.21 {@code section.setBlockState(x, y, z, state, useLocks)} (an ore feature writes straight into the chunk's
	 * sections): 1.12.2's set, the state that was there.
	 */
	public static net.minecraft.block.state.IBlockState setBlockState(@javax.annotation.Nullable ExtendedBlockStorage section, int x, int y, int z,
			net.minecraft.block.state.IBlockState state, boolean useLocks) {
		if (section == null) {
			return net.minecraft.init.Blocks.AIR.getDefaultState();
		}
		net.minecraft.block.state.IBlockState old = section.get(x, y, z);
		section.set(x, y, z, state);
		return old;
	}
}
