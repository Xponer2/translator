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

	/** 1.21 {@code isSaddled()} (Saddleable): a saddled pig or horse. */
	public static boolean isSaddled(Entity entity) {
		if (entity instanceof net.minecraft.entity.passive.EntityPig) {
			return ((net.minecraft.entity.passive.EntityPig) entity).getSaddled();
		}
		return entity instanceof net.minecraft.entity.passive.AbstractHorse && ((net.minecraft.entity.passive.AbstractHorse) entity).isHorseSaddled();
	}

	/** 1.21 {@code fallingBlock.getBlockState()}: the falling block (1.12.2's getBlock). */
	@Nullable
	public static IBlockState getBlockState(net.minecraft.entity.item.EntityFallingBlock falling) {
		return falling.getBlock();
	}

	/** 1.21 {@code moveRelative(speed, input)}: the input turned by the entity's yaw, added to its motion (1.12.2's). */
	public static void moveRelative(Entity entity, float speed, Vec3d input) {
		entity.moveRelative((float) input.x, (float) input.y, (float) input.z, speed);
	}

	/**
	 * 1.21 {@code getControllingPassenger()}: the living rider that steers (1.12.2's returns any entity; the mods' riding
	 * code asked for a LivingEntity and was left without one).
	 */
	@Nullable
	public static net.minecraft.entity.EntityLivingBase getControllingPassenger(Entity entity) {
		Entity rider = entity.getControllingPassenger();
		return rider instanceof net.minecraft.entity.EntityLivingBase ? (net.minecraft.entity.EntityLivingBase) rider : null;
	}

	/** 1.21 {@code isLeashed()} (Leashable): a mob on a lead. */
	public static boolean isLeashed(Entity entity) {
		return entity instanceof net.minecraft.entity.EntityLiving && ((net.minecraft.entity.EntityLiving) entity).getLeashed();
	}

	/** 1.21 {@code setOldPosAndRot()}: the previous position and rotation are the current ones (no interpolation). */
	public static void setOldPosAndRot(Entity entity) {
		entity.prevPosX = entity.lastTickPosX = entity.posX;
		entity.prevPosY = entity.lastTickPosY = entity.posY;
		entity.prevPosZ = entity.lastTickPosZ = entity.posZ;
		entity.prevRotationYaw = entity.rotationYaw;
		entity.prevRotationPitch = entity.rotationPitch;
	}

	// the game's mobs as 1.12.2's own since v181: what the mods call on them (their checks were never true before)

	/** 1.21 {@code new EnderMan(type, level)}: 1.12.2's enderman. */
	public static net.minecraft.entity.monster.EntityEnderman newEntityEnderman(@Nullable io.github.xponer2.translator.registry.EntityType<?> type, World world) {
		return new net.minecraft.entity.monster.EntityEnderman(world);
	}

	/** 1.21 {@code enderman.getCarriedBlock()}. */
	@Nullable
	public static IBlockState getCarriedBlock(net.minecraft.entity.monster.EntityEnderman enderman) {
		return enderman.getHeldBlockState();
	}

	/** 1.21 {@code enderman.setCarriedBlock(state)}. */
	public static void setCarriedBlock(net.minecraft.entity.monster.EntityEnderman enderman, @Nullable IBlockState state) {
		enderman.setHeldBlockState(state);
	}

	/** 1.21 {@code rabbit.getVariant()}: the carried 1.21 variant of 1.12.2's rabbit type (the same ids, 99 the killer bunny). */
	@Nullable
	public static Object getVariant(net.minecraft.entity.passive.EntityRabbit rabbit) {
		try {
			Class<?> variant = Class.forName("net.minecraft.world.entity.animal.Rabbit$Variant", true, net.minecraft.launchwrapper.Launch.classLoader);
			return variant.getMethod("byId", int.class).invoke(null, rabbit.getRabbitType());
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	/** 1.21 {@code rabbit.setVariant(variant)}: by the variant's id. */
	public static void setVariant(net.minecraft.entity.passive.EntityRabbit rabbit, @Nullable Object variant) {
		if (variant == null) {
			return;
		}
		try {
			rabbit.setRabbitType((Integer) variant.getClass().getMethod("id").invoke(variant));
		} catch (ReflectiveOperationException | RuntimeException e) {
			// not a rabbit variant: the rabbit stays as it is
		}
	}

	/**
	 * 1.21 {@code mob.mobInteract(player, hand)}: 1.12.2's interaction of a player with the mob (its leash, then its own:
	 * curing a zombie villager with a golden apple from Create's deployer).
	 */
	public static net.minecraft.util.EnumActionResult mobInteract(net.minecraft.entity.EntityLiving mob, net.minecraft.entity.player.EntityPlayer player,
			net.minecraft.util.EnumHand hand) {
		return mob.processInitialInteract(player, hand) ? net.minecraft.util.EnumActionResult.SUCCESS : net.minecraft.util.EnumActionResult.PASS;
	}

	/**
	 * 1.21 {@code mob.convertTo(type, keepEquipment)}: a mob of the type in this one's place (its position, name, lack of AI,
	 * persistence, equipment when asked), this one removed; null on the client or when the type makes no mob.
	 */
	@Nullable
	public static net.minecraft.entity.EntityLiving convertTo(net.minecraft.entity.EntityLiving mob, io.github.xponer2.translator.registry.EntityType<?> type,
			boolean keepEquipment) {
		if (mob.isDead || mob.world.isRemote || type == null) {
			return null;
		}
		Object made = type.create(mob.world);
		if (!(made instanceof net.minecraft.entity.EntityLiving)) {
			return null;
		}
		net.minecraft.entity.EntityLiving next = (net.minecraft.entity.EntityLiving) made;
		next.copyLocationAndAnglesFrom(mob);
		next.setNoAI(mob.isAIDisabled());
		if (mob.hasCustomName()) {
			next.setCustomNameTag(mob.getCustomNameTag());
			next.setAlwaysRenderNameTag(mob.getAlwaysRenderNameTag());
		}
		if (mob.isNoDespawnRequired()) {
			next.enablePersistence();
		}
		next.setInvisible(mob.isInvisible());
		if (keepEquipment) {
			for (net.minecraft.inventory.EntityEquipmentSlot slot : net.minecraft.inventory.EntityEquipmentSlot.values()) {
				ItemStack stack = mob.getItemStackFromSlot(slot);
				if (!stack.isEmpty()) {
					next.setItemStackToSlot(slot, stack.copy());
				}
			}
		}
		mob.world.spawnEntity(next);
		mob.setDead();
		return next;
	}

	/** 1.21 {@code new ShulkerBoxBlockEntity(pos, state)}: 1.12.2's of the box's colour, at the position. */
	public static net.minecraft.tileentity.TileEntityShulkerBox newTileEntityShulkerBox(BlockPos pos, @Nullable IBlockState state) {
		net.minecraft.item.EnumDyeColor color = state != null && state.getBlock() instanceof net.minecraft.block.BlockShulkerBox
				? ((net.minecraft.block.BlockShulkerBox) state.getBlock()).getColor() : null;
		net.minecraft.tileentity.TileEntityShulkerBox box = color != null ? new net.minecraft.tileentity.TileEntityShulkerBox(color)
				: new net.minecraft.tileentity.TileEntityShulkerBox();
		box.setPos(pos);
		return box;
	}

	/** 1.21 {@code isTraderLlama()}: 1.12.2 has no wandering traders' llamas. */
	public static boolean isTraderLlama(net.minecraft.entity.passive.EntityLlama llama) {
		return false;
	}

	/** 1.21 {@code entity.gameEvent(event[, cause])}: game events (sculk) don't exist on 1.12.2, nothing listens. */
	public static void gameEvent(Entity entity, Object event) {
	}

	public static void gameEvent(Entity entity, Object event, @Nullable Entity cause) {
	}

	/** 1.21 {@code creeper.isPowered()}: a charged creeper. */
	public static boolean isPowered(net.minecraft.entity.monster.EntityCreeper creeper) {
		return creeper.getPowered();
	}

	/** 1.21 {@code wither.isPowered()}: the wither's armor at half health. */
	public static boolean isPowered(net.minecraft.entity.boss.EntityWither wither) {
		return wither.isArmored();
	}

	/**
	 * 1.21 {@code new EmptyLevelChunk(level, pos, biome)}: 1.12.2's empty chunk at that position (it has no biome of its
	 * own; Mekanism made one for the places that aren't loaded).
	 */
	public static net.minecraft.world.chunk.EmptyChunk newEmptyChunk(World world, net.minecraft.util.math.ChunkPos pos,
			@Nullable io.github.xponer2.translator.registry.Holder<?> biome) {
		return new net.minecraft.world.chunk.EmptyChunk(world, pos.x, pos.z);
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

	// --- projectiles ---------------------------------------------------------------------------------------------------

	/**
	 * NeoForge {@code ProjectileImpactEvent.getProjectile()}: the projectile when it is a 1.21 one (the mods' projectiles
	 * extend the carried 1.21 Projectile), null for 1.12.2's own (an arrow of the game isn't a 1.21 Projectile here). It
	 * was always null: the mods' reactions to their own projectiles hitting (the Aether's darts, Iron's spells, Twilight
	 * Forest's throwables) never ran.
	 */
	@Nullable
	public static Object getProjectile(net.minecraftforge.event.entity.ProjectileImpactEvent event) {
		Entity entity = event.getEntity();
		Class<?> projectile = projectileClass();
		return entity != null && projectile != null && projectile.isInstance(entity) ? entity : null;
	}

	private static volatile Class<?> projectile;
	private static volatile boolean projectileLooked;

	@Nullable
	private static Class<?> projectileClass() {
		if (!projectileLooked) {
			try {
				projectile = Class.forName("net.minecraft.world.entity.projectile.Projectile", false, net.minecraft.launchwrapper.Launch.classLoader);
			} catch (ClassNotFoundException | LinkageError | RuntimeException e) {
				projectile = null;
			}
			projectileLooked = true;
		}
		return projectile;
	}

	// --- explosions, lightning ------------------------------------------------------------------------------------------

	/** Whether a 1.21 {@code Explosion.BlockInteraction} (a carried enum) breaks blocks: all but KEEP. */
	private static boolean breaksBlocks(@Nullable Object interaction) {
		return !(interaction instanceof Enum<?> && "KEEP".equals(((Enum<?>) interaction).name()));
	}

	/** 1.21 {@code new Explosion(level, source, x, y, z, radius, fire, blockInteraction)}: 1.12.2's explosion. */
	public static net.minecraft.world.Explosion newExplosion(World world, @Nullable Entity source, double x, double y, double z, float radius, boolean fire,
			@Nullable Object interaction) {
		return new net.minecraft.world.Explosion(world, source, x, y, z, radius, fire, breaksBlocks(interaction));
	}

	/** The same with the blocks it blows up given. */
	public static net.minecraft.world.Explosion newExplosion(World world, @Nullable Entity source, double x, double y, double z, float radius, boolean fire,
			@Nullable Object interaction, List<BlockPos> toBlow) {
		return new net.minecraft.world.Explosion(world, source, x, y, z, radius, fire, breaksBlocks(interaction), toBlow);
	}

	/**
	 * 1.21's explosion with its damage source, damage calculator and effects: 1.12.2's explosion of that size (its own
	 * damage and particles).
	 */
	public static net.minecraft.world.Explosion newExplosion(World world, @Nullable Entity source, @Nullable net.minecraft.util.DamageSource damage,
			@Nullable Object calculator, double x, double y, double z, float radius, boolean fire, @Nullable Object interaction, @Nullable Object smallParticle,
			@Nullable Object largeParticle, @Nullable Object sound) {
		return new net.minecraft.world.Explosion(world, source, x, y, z, radius, fire, breaksBlocks(interaction));
	}

	/** NeoForge {@code state.canDropFromExplosion(level, pos, explosion)}: the block's. */
	public static boolean canDropFromExplosion(IBlockState state, @Nullable IBlockAccess world, @Nullable BlockPos pos, net.minecraft.world.Explosion explosion) {
		return state.getBlock().canDropFromExplosion(explosion);
	}

	/** NeoForge {@code state.onBlockExploded(level, pos, explosion)}: the block's (Forge's, removing it). */
	public static void onBlockExploded(IBlockState state, World world, BlockPos pos, net.minecraft.world.Explosion explosion) {
		state.getBlock().onBlockExploded(world, pos, explosion);
	}

	/**
	 * 1.21 {@code new LightningBolt(type, level)}: 1.12.2's bolt, which the mod then moves where it strikes. Made below
	 * the world: 1.12.2's constructor sets fire around where it's made (here: around the origin), and nothing is loaded
	 * there; it sets fire where it is on its updates, as 1.21's.
	 */
	public static net.minecraft.entity.effect.EntityLightningBolt newEntityLightningBolt(@Nullable io.github.xponer2.translator.registry.EntityType<?> type,
			World world) {
		return new net.minecraft.entity.effect.EntityLightningBolt(world, 0.0D, -100.0D, 0.0D, false);
	}

	/** 1.21 {@code new Slime(type, level)} and the like for vanilla entities: 1.12.2's, made with the world. */
	public static net.minecraft.entity.monster.EntitySlime newEntitySlime(@Nullable io.github.xponer2.translator.registry.EntityType<?> type, World world) {
		return new net.minecraft.entity.monster.EntitySlime(world);
	}

	public static net.minecraft.entity.passive.EntityWolf newEntityWolf(@Nullable io.github.xponer2.translator.registry.EntityType<?> type, World world) {
		return new net.minecraft.entity.passive.EntityWolf(world);
	}

	public static net.minecraft.entity.passive.EntitySheep newEntitySheep(@Nullable io.github.xponer2.translator.registry.EntityType<?> type, World world) {
		return new net.minecraft.entity.passive.EntitySheep(world);
	}

	public static net.minecraft.entity.item.EntityTNTPrimed newEntityTNTPrimed(@Nullable io.github.xponer2.translator.registry.EntityType<?> type, World world) {
		return new net.minecraft.entity.item.EntityTNTPrimed(world);
	}

	public static net.minecraft.entity.projectile.EntityEvokerFangs newEntityEvokerFangs(@Nullable io.github.xponer2.translator.registry.EntityType<?> type, World world) {
		return new net.minecraft.entity.projectile.EntityEvokerFangs(world);
	}

	/** 1.21 {@code new FallingBlockEntity(type, level)}: 1.12.2's, falling as sand until the mod sets its block. */
	public static net.minecraft.entity.item.EntityFallingBlock newEntityFallingBlock(@Nullable io.github.xponer2.translator.registry.EntityType<?> type,
			World world) {
		net.minecraft.entity.item.EntityFallingBlock entity = new net.minecraft.entity.item.EntityFallingBlock(world);
		io.github.xponer2.translator.runtime.mc.FallingBlockEntity.withDefaultBlock(entity);
		return entity;
	}

	/**
	 * {@code level.addFreshEntity(entity)} of the mods (1.12.2's spawnEntity, forced here): a lightning bolt is a weather
	 * effect in 1.12.2, which only addWeatherEffect sends to the clients (spawned as an entity it struck unseen).
	 */
	public static boolean spawnEntity(World world, Entity entity) {
		if (entity instanceof net.minecraft.entity.effect.EntityLightningBolt) {
			return world.addWeatherEffect(entity);
		}
		return world.spawnEntity(entity);
	}

	// --- entities ------------------------------------------------------------------------------------------------------

	/** An entity class's 1.21 {@code getDefaultGravity()} (a carried projectile's, a mod's), null when it has none. */
	private static final ClassValue<java.lang.reflect.Method> DEFAULT_GRAVITY = new ClassValue<java.lang.reflect.Method>() {
		@Override
		protected java.lang.reflect.Method computeValue(Class<?> type) {
			for (Class<?> current = type; current != null && current != Entity.class; current = current.getSuperclass()) {
				try {
					java.lang.reflect.Method method = current.getDeclaredMethod("getDefaultGravity");
					if (method.getReturnType() == double.class) {
						method.setAccessible(true);
						return method;
					}
				} catch (NoSuchMethodException | SecurityException e) {
					// the superclass's
				}
			}
			return null;
		}
	};

	/** 1.21 {@code entity.getGravity()}: none without gravity, else the class's default (1.21's Entity has 0). */
	public static double getGravity(Entity entity) {
		if (entity.hasNoGravity()) {
			return 0.0D;
		}
		java.lang.reflect.Method method = DEFAULT_GRAVITY.get(entity.getClass());
		if (method == null) {
			return 0.0D;
		}
		try {
			return (Double) method.invoke(entity);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return 0.0D;
		}
	}

	/**
	 * 1.21 {@code entity.applyGravity()}: falls by its gravity (the carried ThrowableProjectile's tick calls it: Supplementaries'
	 * thrown bombs and slimeballs hung in the air).
	 */
	public static void applyGravity(Entity entity) {
		double gravity = getGravity(entity);
		if (gravity != 0.0D) {
			entity.motionY -= gravity;
		}
	}

	/**
	 * 1.21 {@code mob.finalizeSpawn(level, difficulty, spawnType, groupData)} a mod calls on a mob it made: a mod's mob
	 * (a Translator facade) runs its own, a vanilla one 1.12.2's onInitialSpawn.
	 */
	@Nullable
	public static net.minecraft.entity.IEntityLivingData finalizeSpawn(net.minecraft.entity.EntityLiving mob, @Nullable World level,
			net.minecraft.world.DifficultyInstance difficulty, @Nullable io.github.xponer2.translator.entity.MobSpawnType spawnType,
			@Nullable net.minecraft.entity.IEntityLivingData data) {
		if (mob instanceof io.github.xponer2.translator.entity.SpawnFinalizing) {
			return ((io.github.xponer2.translator.entity.SpawnFinalizing) mob).finalizeSpawn(level == null ? mob.world : level, difficulty,
					spawnType == null ? io.github.xponer2.translator.entity.MobSpawnType.NATURAL : spawnType, data);
		}
		return mob.onInitialSpawn(difficulty, data);
	}

	/** The carried 1.21 {@code EntityDimensions.scalable(width, height)}, found when first used. */
	private static volatile java.lang.reflect.Method scalableDimensions;

	@Nullable
	private static Object dimensions(float width, float height) {
		try {
			java.lang.reflect.Method scalable = scalableDimensions;
			if (scalable == null) {
				scalable = Class.forName("net.minecraft.world.entity.EntityDimensions", true, net.minecraft.launchwrapper.Launch.classLoader)
						.getMethod("scalable", float.class, float.class);
				scalableDimensions = scalable;
			}
			return scalable.invoke(null, width, height);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("EntityDimensions.scalable", e);
			return null;
		}
	}

	/** 1.21 {@code type.getDimensions()}: the size its builder gave (a carried EntityDimensions). */
	@Nullable
	public static Object getDimensions(io.github.xponer2.translator.registry.EntityType<?> type) {
		return dimensions(type.getWidth(), type.getHeight());
	}

	/** 1.21 {@code entity.getDimensions(pose)}: its 1.12.2 size (1.12.2 has one size for all poses). */
	@Nullable
	public static Object getDimensions(Entity entity, @Nullable Object pose) {
		return dimensions(entity.width, entity.height);
	}

	/** 1.21 {@code entity.updateInWaterStateAndDoFluidPushing()}: 1.12.2's water check (pushed by the flow, in water or not). */
	public static boolean updateInWaterStateAndDoFluidPushing(Entity entity) {
		return entity.handleWaterMovement();
	}

	/** 1.21 {@code entity.getY(scale)}: the height of that share of the entity above its feet. */
	public static double getY(Entity entity, double scale) {
		return entity.posY + entity.height * scale;
	}

	/** 1.21 {@code entity.canUsePortal(allowPassengers)}: alive, not riding unless passengers may, and no boss. */
	public static boolean canUsePortal(Entity entity, boolean allowPassengers) {
		return !entity.isDead && (allowPassengers || !entity.isRiding()) && entity.isNonBoss();
	}

	/** 1.21 {@code entity.interact(player, hand)}: 1.12.2's interaction of a player with the entity. */
	public static net.minecraft.util.EnumActionResult interact(Entity entity, net.minecraft.entity.player.EntityPlayer player, net.minecraft.util.EnumHand hand) {
		return entity.processInitialInteract(player, hand) ? net.minecraft.util.EnumActionResult.SUCCESS : net.minecraft.util.EnumActionResult.PASS;
	}

	/** 1.21 {@code entity.teleportTo(x, y, z)}: there, the clients told (a player's connection too). */
	public static void teleportTo(Entity entity, double x, double y, double z) {
		entity.setPositionAndUpdate(x, y, z);
	}

	/**
	 * 1.21 {@code entity.teleportTo(level, x, y, z, relative, yaw, pitch)}: to that place of that level (another
	 * dimension through Forge's teleporter hook); the relative axes are taken as absolute.
	 */
	public static boolean teleportTo(Entity entity, WorldServer level, double x, double y, double z, @Nullable java.util.Set<?> relative, float yaw, float pitch) {
		if (entity.world.isRemote || level == null) {
			return false;
		}
		Vec3d pos = new Vec3d(x, y, z);
		if (level == entity.world) {
			place(entity, pos, yaw, pitch);
			return true;
		}
		Entity moved = entity.changeDimension(level.provider.getDimension(), (world, mover, entityYaw) -> place(mover, pos, yaw, pitch));
		if (moved != null) {
			place(moved, pos, yaw, pitch);
		}
		return moved != null;
	}

	/** 1.21 {@code serverLevel.getPoiManager()}: 1.12.2 keeps no points of interest (a manager that finds none). */
	public static io.github.xponer2.translator.world.PoiManager getPoiManager(World world) {
		return io.github.xponer2.translator.world.PoiManager.NONE;
	}

	/** 1.21 {@code level.getBiomeManager()}: over the world's biomes (a mod level's own manager when it has one). */
	public static io.github.xponer2.translator.world.BiomeManager getBiomeManager(World world) {
		return io.github.xponer2.translator.world.BiomeManager.of(world);
	}

	// --- block states: 1.21/NeoForge methods with a 1.12.2 counterpart ----------------------------------------------------

	/** 1.21 {@code state.attack(level, pos, player)}: the block is left-clicked. */
	public static void attack(IBlockState state, World world, BlockPos pos, net.minecraft.entity.player.EntityPlayer player) {
		state.getBlock().onBlockClicked(world, pos, player);
	}

	/** NeoForge {@code state.getWeakChanges(level, pos)}: Forge's. */
	public static boolean getWeakChanges(IBlockState state, IBlockAccess world, BlockPos pos) {
		return state.getBlock().getWeakChanges(world, pos);
	}

	/** NeoForge {@code state.isFireSource(level, pos, side)}: Forge's (netherrack's eternal fire). */
	public static boolean isFireSource(IBlockState state, World world, BlockPos pos, net.minecraft.util.EnumFacing side) {
		return state.getBlock().isFireSource(world, pos, side);
	}

	/** 1.21 {@code state.isRandomlyTicking()}. */
	public static boolean isRandomlyTicking(IBlockState state) {
		return state.getBlock().getTickRandomly();
	}

	/** 1.21 {@code state.isSuffocating(level, pos)}. */
	public static boolean isSuffocating(IBlockState state, @Nullable IBlockAccess world, @Nullable BlockPos pos) {
		return state.causesSuffocation();
	}

	/** 1.21 {@code state.propagatesSkylightDown(level, pos)}: light passes it untouched. */
	public static boolean propagatesSkylightDown(IBlockState state, IBlockAccess world, BlockPos pos) {
		return state.getLightOpacity(world, pos) == 0;
	}

	/** 1.21 {@code state.tick(level, pos, random)}: its scheduled update. */
	public static void tick(IBlockState state, WorldServer world, BlockPos pos, java.util.Random random) {
		state.getBlock().updateTick(world, pos, state, random);
	}

	/** NeoForge {@code state.canStickTo(other)}: slime and honey stick to other blocks (1.12.2 has slime only). */
	public static boolean canStickTo(IBlockState state, IBlockState other) {
		return true;
	}

	/** 1.21 {@code state.hidesNeighborFace(level, pos, neighbor, direction)}: 1.12.2 culls by its own rules. */
	public static boolean hidesNeighborFace(IBlockState state, IBlockAccess world, BlockPos pos, IBlockState neighbor, net.minecraft.util.EnumFacing direction) {
		return false;
	}

	/** 1.21 {@code state.supportsExternalFaceHiding()}. */
	public static boolean supportsExternalFaceHiding(IBlockState state) {
		return true;
	}

	// --- entities: 1.21 methods with a 1.12.2 counterpart --------------------------------------------------------------

	/** 1.21 {@code living.addEffect(effect, source)}: 1.12.2's potion effect (the source isn't kept). */
	public static boolean addEffect(net.minecraft.entity.EntityLivingBase living, net.minecraft.potion.PotionEffect effect, @Nullable Entity source) {
		if (!living.isPotionApplicable(effect)) {
			return false;
		}
		living.addPotionEffect(effect);
		return true;
	}

	/** 1.21 {@code entity.isCrouching()}: sneaking. */
	public static boolean isCrouching(Entity entity) {
		return entity.isSneaking();
	}

	/** 1.21 {@code entity.isDiscrete()}: sneaking (the name tag hidden). */
	public static boolean isDiscrete(Entity entity) {
		return entity.isSneaking();
	}

	/** 1.21 {@code living.hasInfiniteMaterials()}: a creative player. */
	public static boolean hasInfiniteMaterials(net.minecraft.entity.EntityLivingBase living) {
		return living instanceof net.minecraft.entity.player.EntityPlayer player && player.capabilities.isCreativeMode;
	}

	/** NeoForge {@code living.removeEffectsCuredBy(cure)}: the effects milk cures (1.12.2 Forge's cure item). */
	public static boolean removeEffectsCuredBy(net.minecraft.entity.EntityLivingBase living, @Nullable Object cure) {
		boolean had = !living.getActivePotionEffects().isEmpty();
		living.curePotionEffects(new ItemStack(net.minecraft.init.Items.MILK_BUCKET));
		return had && living.getActivePotionEffects().isEmpty();
	}

	/** 1.21 {@code living.eat(level, stack[, food])}: one eaten (a player's hunger filled by a food item), the rest returned. */
	public static ItemStack eat(net.minecraft.entity.EntityLivingBase living, World world, ItemStack stack) {
		if (living instanceof net.minecraft.entity.player.EntityPlayer player && stack.getItem() instanceof net.minecraft.item.ItemFood food) {
			player.getFoodStats().addStats(food, stack);
		}
		world.playSound(null, living.posX, living.posY, living.posZ, net.minecraft.init.SoundEvents.ENTITY_PLAYER_BURP, net.minecraft.util.SoundCategory.PLAYERS,
				0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
		if (!(living instanceof net.minecraft.entity.player.EntityPlayer player && player.capabilities.isCreativeMode)) {
			stack.shrink(1);
		}
		return stack;
	}

	public static ItemStack eat(net.minecraft.entity.EntityLivingBase living, World world, ItemStack stack, @Nullable Object food) {
		return eat(living, world, stack);
	}

	/** 1.21 {@code entity.getTicksFrozen()}: 1.12.2 has no freezing. */
	public static int getTicksFrozen(Entity entity) {
		return 0;
	}

	/** 1.21 {@code entity.getTicksRequiredToFreeze()}: 140, as 1.21. */
	public static int getTicksRequiredToFreeze(Entity entity) {
		return 140;
	}

	/** Mobs a goal made aggressive (1.21 {@code mob.setAggressive}: synced data there; here what the goals read). */
	private static final java.util.Map<Entity, Boolean> AGGRESSIVE = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

	/** 1.21 {@code mob.setAggressive(aggressive)} (Ars Nouveau's, Twilight Forest's, Iron's Spells' attack goals). */
	public static void setAggressive(net.minecraft.entity.EntityLiving mob, boolean aggressive) {
		if (aggressive) {
			AGGRESSIVE.put(mob, Boolean.TRUE);
		} else {
			AGGRESSIVE.remove(mob);
		}
	}

	/** 1.21 {@code mob.isAggressive()} (the Dead King's spells check it). */
	public static boolean isAggressive(net.minecraft.entity.EntityLiving mob) {
		return AGGRESSIVE.containsKey(mob);
	}

	/** 1.21 {@code player.createCommandSourceStack()}: the player's command source (Iron's Spells' lore books, AE2). */
	@Nullable
	public static Object createCommandSourceStack(net.minecraft.entity.player.EntityPlayer player) {
		net.minecraft.server.MinecraftServer server = player.getServer();
		if (server == null) {
			return null;
		}
		try {
			return io.github.xponer2.translator.runtime.neoforge.ModCommands.source(server, player);
		} catch (ReflectiveOperationException | RuntimeException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("createCommandSourceStack", e);
			return null;
		}
	}

	/** 1.21 {@code new BlockHitResult(miss, location, direction, pos, inside)} (BlockHitResult.miss of the carried code). */
	public static RayTraceResult newRayTraceResult(boolean miss, Vec3d location, net.minecraft.util.EnumFacing direction, BlockPos pos, boolean inside) {
		return new RayTraceResult(miss ? RayTraceResult.Type.MISS : RayTraceResult.Type.BLOCK, location, direction, pos);
	}

	/** 1.21 {@code state.hasLargeCollisionShape()}: a collision taller than the block (fences, walls, gates). */
	public static boolean hasLargeCollisionShape(IBlockState state) {
		Block block = state.getBlock();
		return block instanceof net.minecraft.block.BlockFence || block instanceof net.minecraft.block.BlockWall || block instanceof net.minecraft.block.BlockFenceGate;
	}

	/** 1.21 {@code entity.setTicksFrozen(ticks)}: 1.12.2 has no freezing (Iron's Spells' ice spells' slow stays theirs). */
	public static void setTicksFrozen(Entity entity, int ticks) {
	}

	/** 1.21 {@code block.handlePrecipitation(state, level, pos, precipitation)}: Forge's rain filling (cauldrons). */
	public static void handlePrecipitation(Block block, IBlockState state, World world, BlockPos pos, @Nullable Object precipitation) {
		block.fillWithRain(world, pos);
	}

	/** 1.21 {@code entity.isFullyFrozen()}. */
	public static boolean isFullyFrozen(Entity entity) {
		return false;
	}

	/** 1.21 {@code entity.hasPassenger(other)}. */
	public static boolean hasPassenger(Entity entity, Entity other) {
		return entity.isPassenger(other);
	}

	/** 1.21 {@code entity.hasControllingPassenger()}. */
	public static boolean hasControllingPassenger(Entity entity) {
		return entity.getControllingPassenger() != null;
	}

	/** 1.21 {@code entity.absMoveTo(x, y, z, yaw, pitch)}: there, turned so. */
	public static void absMoveTo(Entity entity, double x, double y, double z, float yaw, float pitch) {
		entity.setPositionAndRotation(x, y, z, yaw, pitch);
	}

	/** 1.21 {@code entity.ignoreExplosion(explosion)}: 1.12.2 explosions hit every entity. */
	public static boolean ignoreExplosion(Entity entity, @Nullable net.minecraft.world.Explosion explosion) {
		return false;
	}

	// --- targeting (1.21 TargetingConditions, a carried class: its fields read, its code needs what 1.12.2 lacks) ----------

	/** The fields of the carried TargetingConditions (isCombat, range, checkLineOfSight, selector), found once. */
	private static final ClassValue<java.lang.reflect.Field[]> CONDITIONS = new ClassValue<java.lang.reflect.Field[]>() {
		@Override
		protected java.lang.reflect.Field[] computeValue(Class<?> type) {
			java.lang.reflect.Field[] fields = new java.lang.reflect.Field[4];
			String[] names = {"isCombat", "range", "checkLineOfSight", "selector"};
			for (int i = 0; i < names.length; i++) {
				try {
					fields[i] = type.getDeclaredField(names[i]);
					fields[i].setAccessible(true);
				} catch (NoSuchFieldException | SecurityException e) {
					// a field of another name: that test isn't made
				}
			}
			return fields;
		}
	};

	/**
	 * Whether the target passes 1.21 targeting conditions for the source: alive and not itself; a combat target no
	 * spectator nor creative player; within the range; seen when they ask for sight; their selector's.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static boolean matches(@Nullable Object conditions, @Nullable net.minecraft.entity.EntityLivingBase source, @Nullable net.minecraft.entity.EntityLivingBase target) {
		if (target == null || target.isDead || target == source || target.getHealth() <= 0.0F) {
			return false;
		}
		if (conditions == null) {
			return true;
		}
		java.lang.reflect.Field[] fields = CONDITIONS.get(conditions.getClass());
		try {
			boolean combat = fields[0] != null && fields[0].getBoolean(conditions);
			if (combat && target instanceof net.minecraft.entity.player.EntityPlayer player && (player.isSpectator() || player.isCreative())) {
				return false;
			}
			double range = fields[1] == null ? -1.0D : fields[1].getDouble(conditions);
			if (range > 0.0D && source != null && source.getDistanceSq(target) > range * range) {
				return false;
			}
			Object selector = fields[3] == null ? null : fields[3].get(conditions);
			if (selector instanceof java.util.function.Predicate && !((java.util.function.Predicate) selector).test(target)) {
				return false;
			}
			boolean sight = fields[2] != null && fields[2].getBoolean(conditions);
			if (sight && source instanceof net.minecraft.entity.EntityLiving mob && !mob.getEntitySenses().canSee(target)) {
				return false;
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			return true;
		}
		return true;
	}

	/** 1.21 {@code level.getNearbyEntities(type, conditions, source, box)}: Iron's Spells' and Supplementaries' target searches. */
	public static <T extends net.minecraft.entity.EntityLivingBase> List<T> getNearbyEntities(World world, Class<T> type, @Nullable Object conditions,
			@Nullable net.minecraft.entity.EntityLivingBase source, AxisAlignedBB box) {
		List<T> found = new java.util.ArrayList<>();
		for (T entity : world.getEntitiesWithinAABB(type, box)) {
			if (matches(conditions, source, entity)) {
				found.add(entity);
			}
		}
		return found;
	}

	/** 1.21 {@code level.getNearbyPlayers(conditions, source, box)}. */
	public static List<net.minecraft.entity.player.EntityPlayer> getNearbyPlayers(World world, @Nullable Object conditions,
			@Nullable net.minecraft.entity.EntityLivingBase source, AxisAlignedBB box) {
		List<net.minecraft.entity.player.EntityPlayer> found = new java.util.ArrayList<>();
		for (net.minecraft.entity.player.EntityPlayer player : world.playerEntities) {
			if (box.contains(player.getPositionVector()) && matches(conditions, source, player)) {
				found.add(player);
			}
		}
		return found;
	}

	/** 1.21 {@code level.getNearestEntity(type, conditions, source, x, y, z, box)}. */
	@Nullable
	public static <T extends net.minecraft.entity.EntityLivingBase> T getNearestEntity(World world, Class<T> type, @Nullable Object conditions,
			@Nullable net.minecraft.entity.EntityLivingBase source, double x, double y, double z, AxisAlignedBB box) {
		return getNearestEntity(world, world.getEntitiesWithinAABB(type, box), conditions, source, x, y, z);
	}

	/** 1.21 {@code level.getNearestEntity(entities, conditions, source, x, y, z)}. */
	@Nullable
	public static <T extends net.minecraft.entity.EntityLivingBase> T getNearestEntity(World world, List<? extends T> entities, @Nullable Object conditions,
			@Nullable net.minecraft.entity.EntityLivingBase source, double x, double y, double z) {
		T nearest = null;
		double best = Double.MAX_VALUE;
		for (T entity : entities) {
			if (matches(conditions, source, entity)) {
				double distance = entity.getDistanceSq(x, y, z);
				if (distance < best) {
					best = distance;
					nearest = entity;
				}
			}
		}
		return nearest;
	}

	/** 1.21 {@code level.getNearestPlayer(conditions, source, x, y, z)}. */
	@Nullable
	public static net.minecraft.entity.player.EntityPlayer getNearestPlayer(World world, @Nullable Object conditions, @Nullable net.minecraft.entity.EntityLivingBase source,
			double x, double y, double z) {
		return getNearestEntity(world, world.playerEntities, conditions, source, x, y, z);
	}

	/** 1.21 {@code serverLevel.getPlayers(predicate)}. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static List<net.minecraft.entity.player.EntityPlayerMP> getPlayers(WorldServer world, java.util.function.Predicate filter) {
		return world.getPlayers(net.minecraft.entity.player.EntityPlayerMP.class, player -> filter.test(player));
	}

	// --- blocks of a world ---------------------------------------------------------------------------------------------

	/** 1.21 {@code level.neighborChanged(state, pos, block, fromPos, moved)}: the state's own update. */
	public static void neighborChanged(World world, IBlockState state, BlockPos pos, Block block, BlockPos from, boolean moved) {
		state.neighborChanged(world, pos, block, from);
	}

	/** 1.21 {@code level.hasChunksAt(fromX, fromZ, toX, toZ)} (block coordinates). */
	public static boolean hasChunksAt(World world, int fromX, int fromZ, int toX, int toZ) {
		return world.isAreaLoaded(new BlockPos(fromX, 0, fromZ), new BlockPos(toX, 0, toZ));
	}

	public static boolean hasChunksAt(World world, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
		return world.isAreaLoaded(new BlockPos(fromX, fromY, fromZ), new BlockPos(toX, toY, toZ));
	}

	public static boolean hasChunksAt(World world, BlockPos from, BlockPos to) {
		return world.isAreaLoaded(from, to);
	}

	/** 1.21 {@code level.hasSignal(pos, direction)}. */
	public static boolean hasSignal(World world, BlockPos pos, net.minecraft.util.EnumFacing direction) {
		return world.getRedstonePower(pos, direction) > 0;
	}

	/** 1.21 {@code level.noBlockCollision(entity, box)}: nothing solid there. */
	public static boolean noBlockCollision(World world, @Nullable Entity entity, AxisAlignedBB box) {
		return world.getCollisionBoxes(entity, box).isEmpty();
	}

	/** 1.21 {@code level.getBlockStates(box)}: the states of the blocks the box covers. */
	public static java.util.stream.Stream<IBlockState> getBlockStates(World world, AxisAlignedBB box) {
		BlockPos from = new BlockPos(Math.floor(box.minX), Math.floor(box.minY), Math.floor(box.minZ));
		BlockPos to = new BlockPos(Math.floor(box.maxX), Math.floor(box.maxY), Math.floor(box.maxZ));
		java.util.List<IBlockState> states = new java.util.ArrayList<>();
		for (BlockPos pos : BlockPos.getAllInBoxMutable(from, to)) {
			states.add(world.getBlockState(pos));
		}
		return states.stream();
	}

	/** 1.21 {@code level.setBlock(pos, state, flags, recursionLeft)}: 1.12.2's setBlockState. */
	public static boolean setBlock(World world, BlockPos pos, IBlockState state, int flags, int recursionLeft) {
		return world.setBlockState(pos, state, flags);
	}

	/** 1.21 {@code level.explode(...)} with its particles and sound: 1.12.2's explosion. */
	public static net.minecraft.world.Explosion explode(World world, @Nullable Entity source, @Nullable Object damageSource, @Nullable Object calculator, double x,
			double y, double z, float radius, boolean fire, @Nullable Object interaction, @Nullable Object smallParticle, @Nullable Object largeParticle,
			@Nullable Object sound) {
		return ApiBridges.explode(world, source, x, y, z, radius, fire, interaction);
	}

	/** 1.21 {@code level.explode(source, damage, calculator, pos, radius, fire, interaction)}. */
	public static net.minecraft.world.Explosion explode(World world, @Nullable Entity source, @Nullable Object damageSource, @Nullable Object calculator, Vec3d pos,
			float radius, boolean fire, @Nullable Object interaction) {
		return ApiBridges.explode(world, source, pos.x, pos.y, pos.z, radius, fire, interaction);
	}

	/** 1.21 {@code level.getNearestPlayer(conditions, entity)}: the nearest player to the entity (the conditions aren't read). */
	@Nullable
	public static net.minecraft.entity.player.EntityPlayer getNearestPlayer(World world, @Nullable Object conditions, Entity entity) {
		net.minecraft.entity.EntityLivingBase source = entity instanceof net.minecraft.entity.EntityLivingBase living ? living : null;
		return getNearestEntity(world, world.playerEntities, conditions, source, entity.posX, entity.posY, entity.posZ);
	}

	/** The same near a place. */
	@Nullable
	public static net.minecraft.entity.player.EntityPlayer getNearestPlayer(World world, @Nullable Object conditions, double x, double y, double z) {
		return getNearestEntity(world, world.playerEntities, conditions, null, x, y, z);
	}

	// --- blocks and chunks ---------------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code state.isFaceSturdy(level, pos, face, supportType)} (a carried enum): FULL and RIGID need a solid face,
	 * CENTER a post's middle too (1.12.2's centre and pole face shapes). Block.canSupportCenter (candle holders, sconces
	 * and cranks of Supplementaries, Create's) asks it.
	 */
	public static boolean isFaceSturdy(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.util.EnumFacing face, @Nullable Object supportType) {
		net.minecraft.block.state.BlockFaceShape shape = state.getBlockFaceShape(world, pos, face);
		if (shape == net.minecraft.block.state.BlockFaceShape.SOLID) {
			return true;
		}
		return supportType instanceof Enum<?> && "CENTER".equals(((Enum<?>) supportType).name())
				&& (shape == net.minecraft.block.state.BlockFaceShape.CENTER || shape == net.minecraft.block.state.BlockFaceShape.CENTER_BIG
						|| shape == net.minecraft.block.state.BlockFaceShape.MIDDLE_POLE || shape == net.minecraft.block.state.BlockFaceShape.MIDDLE_POLE_THICK);
	}

	/** NeoForge {@code state.rotate(level, pos, rotation)}: 1.12.2's rotated state. */
	public static IBlockState rotate(IBlockState state, @Nullable World world, @Nullable BlockPos pos, net.minecraft.util.Rotation rotation) {
		return state.withRotation(rotation);
	}

	/** 1.21 {@code level.getBrightness(layer, pos)}: the light of that layer (a world's or chunk cache's; else full sky, no block light). */
	public static int getBrightness(IBlockAccess world, net.minecraft.world.EnumSkyBlock layer, BlockPos pos) {
		if (world instanceof World) {
			return ((World) world).getLightFor(layer, pos);
		}
		if (world instanceof net.minecraft.world.ChunkCache) {
			return ((net.minecraft.world.ChunkCache) world).getLightFor(layer, pos);
		}
		return layer == net.minecraft.world.EnumSkyBlock.SKY ? 15 : 0;
	}

	/** 1.21 {@code chunk.getFluidState(pos)}: of the block there. */
	public static io.github.xponer2.translator.util.FluidState getFluidState(Chunk chunk, BlockPos pos) {
		return io.github.xponer2.translator.util.FluidState.of(chunk.getBlockState(pos));
	}

	public static io.github.xponer2.translator.util.FluidState getFluidState(Chunk chunk, int x, int y, int z) {
		return io.github.xponer2.translator.util.FluidState.of(chunk.getBlockState(x, y, z));
	}

	/** 1.21 {@code chunkSource.broadcast(entity, packet)}: to the players tracking the entity (Mekanism's, Ars', Iron's effects). */
	public static void broadcast(net.minecraft.world.gen.ChunkProviderServer chunks, Entity entity, net.minecraft.network.Packet<?> packet) {
		chunks.world.getEntityTracker().sendToTracking(entity, packet);
	}

	/** 1.21 {@code chunkSource.broadcastAndSend(entity, packet)}: to them and to the entity itself if it's a player. */
	public static void broadcastAndSend(net.minecraft.world.gen.ChunkProviderServer chunks, Entity entity, net.minecraft.network.Packet<?> packet) {
		chunks.world.getEntityTracker().sendToTrackingAndSelf(entity, packet);
	}

	/**
	 * 1.21 {@code chunkSource.addRegionTicket(type, pos, distance, value)}: 1.12.2 has no chunk tickets; the chunk is
	 * loaded now, which is what a mod's teleport or machine needs at that moment.
	 */
	public static void addRegionTicket(net.minecraft.world.gen.ChunkProviderServer chunks, @Nullable Object type, net.minecraft.util.math.ChunkPos pos,
			int distance, @Nullable Object value) {
		chunks.provideChunk(pos.x, pos.z);
	}

	/** 1.21 {@code chunkSource.removeRegionTicket(...)}: nothing to release. */
	public static void removeRegionTicket(net.minecraft.world.gen.ChunkProviderServer chunks, @Nullable Object type, net.minecraft.util.math.ChunkPos pos,
			int distance, @Nullable Object value) {
	}

	/** 1.21 {@code entity.addDeltaMovement(vec)}: pushed by it (a player's client told, as 1.12.2 knockback). */
	public static void addDeltaMovement(Entity entity, Vec3d by) {
		entity.motionX += by.x;
		entity.motionY += by.y;
		entity.motionZ += by.z;
		entity.velocityChanged = true;
	}

	/** 1.21 {@code entity.canChangeDimensions(from, to)}: alive and no boss. */
	public static boolean canChangeDimensions(Entity entity, @Nullable World from, @Nullable World to) {
		return !entity.isDead && entity.isNonBoss();
	}

	/** 1.21 {@code entity.getMaxAirSupply()}: 1.12.2's 300 ticks. */
	public static int getMaxAirSupply(Entity entity) {
		return 300;
	}

	/** 1.21 {@code level.getDescription()}: the dimension's name. */
	public static net.minecraft.util.text.ITextComponent getDescription(World world) {
		return new net.minecraft.util.text.TextComponentString(world.provider.getDimensionType().getName());
	}

	/** 1.21 {@code effect.hiddenEffect}: 1.12.2 keeps no hidden effect under another. */
	@Nullable
	public static net.minecraft.potion.PotionEffect hiddenEffect(net.minecraft.potion.PotionEffect effect) {
		return null;
	}

	/** 1.21 {@code state.destroySpeed}: the block's hardness. */
	public static float destroySpeed(IBlockState state) {
		try {
			return state.getBlockHardness(null, null);
		} catch (RuntimeException e) {
			return 1.0F;
		}
	}

	/** 1.21 {@code new ListTag(capacity)}: an empty list. */
	public static net.minecraft.nbt.NBTTagList newNBTTagList(int capacity) {
		return new net.minecraft.nbt.NBTTagList();
	}

	/** NeoForge {@code fluidStack.save(registries)}: Forge's NBT of the stack. */
	public static net.minecraft.nbt.NBTBase save(net.minecraftforge.fluids.FluidStack stack, @Nullable Object registries) {
		return stack.writeToNBT(new net.minecraft.nbt.NBTTagCompound());
	}

	/** 1.21 {@code spawner.getSpawner()}: 1.12.2's spawner logic. */
	public static net.minecraft.tileentity.MobSpawnerBaseLogic getSpawner(net.minecraft.tileentity.TileEntityMobSpawner spawner) {
		return spawner.getSpawnerBaseLogic();
	}

	/** 1.21 {@code spawner.setEntityId(type, random)}: what it spawns (the Aether's and Twilight Forest's dungeon spawners). */
	public static void setEntityId(net.minecraft.tileentity.TileEntityMobSpawner spawner, io.github.xponer2.translator.registry.EntityType<?> type,
			@Nullable java.util.Random random) {
		net.minecraft.util.ResourceLocation id = io.github.xponer2.translator.registry.EntityType.getKey(type);
		if (id != null) {
			spawner.getSpawnerBaseLogic().setEntityId(id);
			spawner.markDirty();
		}
	}

	/** 1.21 {@code movingPiston.getMovedState()}: 1.12.2's piston state. */
	public static IBlockState getMovedState(net.minecraft.tileentity.TileEntityPiston piston) {
		return piston.getPistonState();
	}

	/** 1.21 {@code movingPiston.getMovementDirection()}: where the block moves (the piston's facing, back when retracting). */
	public static net.minecraft.util.EnumFacing getMovementDirection(net.minecraft.tileentity.TileEntityPiston piston) {
		return piston.isExtending() ? piston.getFacing() : piston.getFacing().getOpposite();
	}

	/** 1.21 {@code mapData.setColor(x, z, color)}: the map pixel, sent to the map's viewers. */
	public static void setColor(net.minecraft.world.storage.MapData data, int x, int z, byte color) {
		int index = x + z * 128;
		if (index >= 0 && index < data.colors.length && data.colors[index] != color) {
			data.colors[index] = color;
			data.updateMapData(x, z);
		}
	}

	/** 1.21 {@code entity.setPos(vec)}: there (Iron's Spells' spell projectiles and areas). */
	public static void setPos(Entity entity, Vec3d pos) {
		entity.setPosition(pos.x, pos.y, pos.z);
	}

	/** The same under the table's general name. */
	public static void setPosition(Entity entity, Vec3d pos) {
		entity.setPosition(pos.x, pos.y, pos.z);
	}

	/** A mod entity class's 1.21 {@code getDimensions(pose)} (its own size by pose or state), null when it has none. */
	private static final ClassValue<java.lang.reflect.Method> OWN_DIMENSIONS = new ClassValue<java.lang.reflect.Method>() {
		@Override
		protected java.lang.reflect.Method computeValue(Class<?> type) {
			for (Class<?> current = type; current != null && !current.getName().startsWith("net.minecraft.")
					&& !current.getName().startsWith("io.github.xponer2.translator."); current = current.getSuperclass()) {
				for (java.lang.reflect.Method method : current.getDeclaredMethods()) {
					if ("getDimensions".equals(method.getName()) && method.getParameterCount() == 1 && method.getParameterTypes()[0].isEnum()) {
						method.setAccessible(true);
						return method;
					}
				}
			}
			return null;
		}
	};

	/**
	 * 1.21 {@code entity.refreshDimensions()}: its size again from its {@code getDimensions(pose)} (a mod's own: Iron's
	 * Spells' areas grow, its ice spider's parts), applied as 1.12.2's setSize does; a mod entity without one keeps its
	 * type's size.
	 */
	/** 1.21 {@code entity.dimensions} (read): its 1.12.2 size as a carried EntityDimensions. */
	@Nullable
	public static Object dimensions(Entity entity) {
		return getDimensions(entity, null);
	}

	/**
	 * 1.21 {@code entity.dimensions = size} (1.12.2's Entity has no such field): its 1.12.2 size, as setSize makes it.
	 * Twilight Forest's part entities (the Snow Queen's ice shields, the Hydra's heads) set their size so in their
	 * constructors, and kept 1.12.2's 0.6 x 1.8.
	 */
	public static void dimensions(Entity entity, @Nullable Object dimensions) {
		if (dimensions == null) {
			return;
		}
		float width;
		float height;
		try {
			width = ((Number) dimensions.getClass().getMethod("width").invoke(dimensions)).floatValue();
			height = ((Number) dimensions.getClass().getMethod("height").invoke(dimensions)).floatValue();
		} catch (ReflectiveOperationException | RuntimeException e) {
			return;
		}
		if (width != entity.width || height != entity.height) {
			entity.width = width;
			entity.height = height;
			AxisAlignedBB box = entity.getEntityBoundingBox();
			entity.setEntityBoundingBox(new AxisAlignedBB(box.minX, box.minY, box.minZ, box.minX + width, box.minY + height, box.minZ + width));
		}
	}

	public static void refreshDimensions(Entity entity) {
		java.lang.reflect.Method own = OWN_DIMENSIONS.get(entity.getClass());
		float width;
		float height;
		try {
			if (own != null) {
				Object pose = Enum.valueOf((Class) own.getParameterTypes()[0], "STANDING");
				Object dimensions = own.invoke(entity, pose);
				if (dimensions == null) {
					return;
				}
				width = ((Number) dimensions.getClass().getMethod("width").invoke(dimensions)).floatValue();
				height = ((Number) dimensions.getClass().getMethod("height").invoke(dimensions)).floatValue();
			} else {
				return;
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(entity.getClass().getName() + ".refreshDimensions", e);
			return;
		}
		if (width != entity.width || height != entity.height) {
			entity.width = width;
			entity.height = height;
			AxisAlignedBB box = entity.getEntityBoundingBox();
			entity.setEntityBoundingBox(new AxisAlignedBB(box.minX, box.minY, box.minZ, box.minX + width, box.minY + height, box.minZ + width));
		}
	}

	/**
	 * 1.21 {@code state.onProjectileHit(level, state, hit, projectile)}: the block's own (a mod block's 1.21 method; a
	 * 1.12.2 block has none).
	 */
	public static void onProjectileHit(IBlockState state, World world, IBlockState hitState, RayTraceResult hit, Entity projectile) {
		Block block = state.getBlock();
		for (java.lang.reflect.Method method : block.getClass().getMethods()) {
			if ("onProjectileHit".equals(method.getName()) && method.getParameterCount() == 4) {
				try {
					method.invoke(block, world, hitState, hit, projectile);
				} catch (ReflectiveOperationException | RuntimeException e) {
					io.github.xponer2.translator.runtime.RuntimeStubs.failed(block.getClass().getName() + ".onProjectileHit", e);
				}
				return;
			}
		}
	}

	/** 1.21 {@code level.getChunkForCollisions(x, z)}: the world where that chunk is loaded (collisions read blocks from it), else null. */
	@Nullable
	public static IBlockAccess getChunkForCollisions(World world, int chunkX, int chunkZ) {
		return world.isBlockLoaded(new BlockPos(chunkX << 4, 0, chunkZ << 4)) ? world : null;
	}

	/** 1.21 {@code savedData.setDirty()}: saved with the world (Twilight Forest's magic and maze maps). */
	public static void setDirty(net.minecraft.world.storage.WorldSavedData data) {
		data.markDirty();
	}

	/** 1.21 {@code block.defaultDestroyTime()}: its hardness. */
	public static float defaultDestroyTime(Block block) {
		try {
			return block.getDefaultState().getBlockHardness(null, null);
		} catch (RuntimeException e) {
			return 1.0F;
		}
	}

	/** 1.21 {@code box.encapsulate(other)}: the box grown over the other (1.12.2's expandTo), itself. */
	public static net.minecraft.world.gen.structure.StructureBoundingBox encapsulate(net.minecraft.world.gen.structure.StructureBoundingBox box,
			net.minecraft.world.gen.structure.StructureBoundingBox other) {
		box.expandTo(other);
		return box;
	}

	/** 1.21 {@code box.encapsulate(pos)}: the box grown over the position, itself. */
	public static net.minecraft.world.gen.structure.StructureBoundingBox encapsulate(net.minecraft.world.gen.structure.StructureBoundingBox box, BlockPos pos) {
		box.expandTo(new net.minecraft.world.gen.structure.StructureBoundingBox(pos, pos));
		return box;
	}

	/** 1.21 {@code vec.zRot(angle)}: turned about the z axis. */
	public static Vec3d zRot(Vec3d vec, float angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		return new Vec3d(vec.x * cos + vec.y * sin, vec.y * cos - vec.x * sin, vec.z);
	}

	/** 1.21 {@code vec2.distanceToSqr(other)}. */
	public static float distanceToSqr(net.minecraft.util.math.Vec2f a, net.minecraft.util.math.Vec2f b) {
		float dx = b.x - a.x;
		float dy = b.y - a.y;
		return dx * dx + dy * dy;
	}

	/** 1.21 {@code box.move(dx, dy, dz)}: the box itself moved (1.12.2's offset). */
	public static net.minecraft.world.gen.structure.StructureBoundingBox move(net.minecraft.world.gen.structure.StructureBoundingBox box, int dx, int dy, int dz) {
		box.offset(dx, dy, dz);
		return box;
	}

	public static net.minecraft.world.gen.structure.StructureBoundingBox move(net.minecraft.world.gen.structure.StructureBoundingBox box, Vec3i by) {
		return move(box, by.getX(), by.getY(), by.getZ());
	}

	// --- packets -------------------------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code new ClientboundGameEventPacket(type, value)}: 1.12.2's game state packet of the type's number (a
	 * carried 1.21 type: its {@code id}; the numbers are the same: 1 rain starts, 7 rain level, 10 elder guardian, ...).
	 */
	public static net.minecraft.network.play.server.SPacketChangeGameState newSPacketChangeGameState(Object type, float value) {
		int id = 0;
		try {
			java.lang.reflect.Field field = type.getClass().getDeclaredField("id");
			field.setAccessible(true);
			id = field.getInt(type);
		} catch (ReflectiveOperationException | RuntimeException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("ClientboundGameEventPacket: the type " + type, e);
		}
		return new net.minecraft.network.play.server.SPacketChangeGameState(id, value);
	}

	/** 1.21 {@code entity.thunderHit(level, bolt)}: 1.12.2's onStruckByLightning. */
	public static void thunderHit(Entity entity, @Nullable WorldServer level, net.minecraft.entity.effect.EntityLightningBolt bolt) {
		entity.onStruckByLightning(bolt);
	}

	/** The same under the table's name (1.12.2's takes the bolt only). */
	public static void onStruckByLightning(Entity entity, @Nullable WorldServer level, net.minecraft.entity.effect.EntityLightningBolt bolt) {
		entity.onStruckByLightning(bolt);
	}

	/** 1.21 {@code new ClientboundUpdateMobEffectPacket(entityId, effect, blend)}: 1.12.2's effect packet. */
	public static net.minecraft.network.play.server.SPacketEntityEffect newSPacketEntityEffect(int entityId, net.minecraft.potion.PotionEffect effect, boolean blend) {
		return new net.minecraft.network.play.server.SPacketEntityEffect(entityId, effect);
	}

	/** 1.21 {@code new ClientboundExplodePacket(x, y, z, power, blocks, knockback, interaction, particles, sound)}: 1.12.2's. */
	public static net.minecraft.network.play.server.SPacketExplosion newSPacketExplosion(double x, double y, double z, float power, List<BlockPos> blocks,
			@Nullable Vec3d knockback, @Nullable Object interaction, @Nullable Object smallParticle, @Nullable Object largeParticle, @Nullable Object sound) {
		return new net.minecraft.network.play.server.SPacketExplosion(x, y, z, power, blocks == null ? Collections.emptyList() : blocks, knockback);
	}

	/** 1.21 {@code new ClientboundBlockUpdatePacket(pos, state)}: 1.12.2's block change packet of that state. */
	public static net.minecraft.network.play.server.SPacketBlockChange newSPacketBlockChange(BlockPos pos, IBlockState state) {
		var packet = new net.minecraft.network.play.server.SPacketBlockChange();
		packet.blockState = state;
		if (BLOCK_CHANGE_POS != null) {
			try {
				BLOCK_CHANGE_POS.set(packet, pos);
			} catch (IllegalAccessException e) {
				// the packet keeps no position: the client ignores it
			}
		}
		return packet;
	}

	/** 1.21 {@code new ClientboundBlockUpdatePacket(level, pos)}: the state there now. */
	public static net.minecraft.network.play.server.SPacketBlockChange newSPacketBlockChange(IBlockAccess world, BlockPos pos) {
		return world instanceof World ? new net.minecraft.network.play.server.SPacketBlockChange((World) world, pos)
				: newSPacketBlockChange(pos, world.getBlockState(pos));
	}

	private static final java.lang.reflect.Field BLOCK_CHANGE_POS = blockChangePos();

	private static java.lang.reflect.Field blockChangePos() {
		for (java.lang.reflect.Field field : net.minecraft.network.play.server.SPacketBlockChange.class.getDeclaredFields()) {
			if (field.getType() == BlockPos.class) {
				field.setAccessible(true);
				return field;
			}
		}
		return null;
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

	// --- structures (carried 1.21 pieces, templates and their processors) ------------------------------------------------

	/**
	 * 1.21 {@code chest.setLootTable(key, seed)} (a structure piece's chest, {@code StructurePiece.createChest}): 1.12.2's
	 * loot table of the chest, filled when it is first opened.
	 */
	public static void setLootTable(net.minecraft.tileentity.TileEntityLockableLoot chest, io.github.xponer2.translator.registry.ResourceKey<?> table,
			long seed) {
		chest.setLootTable(table == null ? null : table.location(), seed);
	}

	/** 1.21 {@code rotation.rotation()}: the OctahedralGroup of the turn (templates and jigsaw blocks turn their states by it). */
	public static Object rotation(net.minecraft.util.Rotation rotation) {
		String name = switch (rotation) {
			case CLOCKWISE_90 -> "ROT_90_Y_NEG";
			case CLOCKWISE_180 -> "ROT_180_FACE_XZ";
			case COUNTERCLOCKWISE_90 -> "ROT_90_Y_POS";
			default -> "IDENTITY";
		};
		try {
			return Class.forName("com.mojang.math.OctahedralGroup", true, net.minecraft.launchwrapper.Launch.classLoader).getField(name).get(null);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("No carried OctahedralGroup", e);
		}
	}

	/** 1.21 {@code listTag.getList(index)}: the list at that index, an empty one when it is something else. */
	public static net.minecraft.nbt.NBTTagList getList(net.minecraft.nbt.NBTTagList list, int index) {
		return index >= 0 && index < list.tagCount() && list.get(index) instanceof net.minecraft.nbt.NBTTagList inner ? inner
				: new net.minecraft.nbt.NBTTagList();
	}

	/** 1.21 {@code state.hasDynamicShape()}: no 1.12.2 block changes its shape by itself (shulker boxes, pistons do in 1.21). */
	public static boolean hasDynamicShape(net.minecraft.block.Block block) {
		return false;
	}

	/**
	 * 1.21 {@code chunkSource.randomState()} (the Aether's surface rule processor): the noise-less RandomState the 1.21
	 * structures get (heights come from the 1.12.2 terrain).
	 */
	public static Object randomState(net.minecraft.world.gen.ChunkProviderServer source) {
		return io.github.xponer2.translator.world.StructureGeneration.randomState();
	}

	/** 1.21 {@code chunk.getAllReferences()}: the mods' structures reaching into the chunk, by their start chunks. */
	public static java.util.Map<Object, it.unimi.dsi.fastutil.longs.LongSet> getAllReferences(net.minecraft.world.chunk.Chunk chunk) {
		return io.github.xponer2.translator.world.StructureGeneration.references(chunk);
	}

	/** 1.21 {@code chunk.getReferencesForStructure(structure)}. */
	public static it.unimi.dsi.fastutil.longs.LongSet getReferencesForStructure(net.minecraft.world.chunk.Chunk chunk, Object structure) {
		var found = io.github.xponer2.translator.world.StructureGeneration.references(chunk).get(structure);
		return found == null ? it.unimi.dsi.fastutil.longs.LongSets.EMPTY_SET : found;
	}

	/** 1.21 {@code chunk.hasAnyStructureReferences()}. */
	public static boolean hasAnyStructureReferences(net.minecraft.world.chunk.Chunk chunk) {
		return !io.github.xponer2.translator.world.StructureGeneration.references(chunk).isEmpty();
	}

	/** 1.21 {@code chunk.getStartForStructure(structure)}: the start of that structure in the chunk, else null. */
	public static Object getStartForStructure(net.minecraft.world.chunk.Chunk chunk, Object structure) {
		return io.github.xponer2.translator.world.StructureGeneration.startFor(chunk, structure);
	}

	/**
	 * 1.21 {@code Entity.DATA_POSE}: 1.12.2 syncs no pose, so a key no entity has (id -1): the mods' "DATA_POSE.equals(key)"
	 * in onSyncedDataUpdated is false. The carried Entity's own static initializer defines it with a serializer 1.12.2
	 * lacks and threw (Iron's Spells' ice spider could not be made).
	 */
	public static final net.minecraft.network.datasync.DataParameter<Integer> DATA_POSE = new net.minecraft.network.datasync.DataParameter<>(-1,
			net.minecraft.network.datasync.DataSerializers.VARINT);

	/**
	 * NeoForge {@code state.isPortalFrame(level, pos)}: the block's own answer when it has one (a mod block's override),
	 * else obsidian (NeoForge's default). PortalShape names it as a method reference; Twilight Forest's fires light
	 * portals by it.
	 */
	public static boolean isPortalFrame(IBlockState state, net.minecraft.world.IBlockAccess level, net.minecraft.util.math.BlockPos pos) {
		net.minecraft.block.Block block = state.getBlock();
		try {
			java.lang.reflect.Method own = block.getClass().getMethod("isPortalFrame", IBlockState.class, net.minecraft.world.IBlockAccess.class,
					net.minecraft.util.math.BlockPos.class);
			return (Boolean) own.invoke(block, state, level, pos);
		} catch (NoSuchMethodException e) {
			return block == net.minecraft.init.Blocks.OBSIDIAN;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return block == net.minecraft.init.Blocks.OBSIDIAN;
		}
	}

	/** 1.21 {@code redstoneSide.isConnected()}: up or side (Supplementaries' gunpowder trail connects by it). */
	public static boolean isConnected(Enum<?> side) {
		// 1.12.2's EnumAttachPosition (UP, SIDE, NONE) is package-private where the Translator compiles
		return !"NONE".equals(side.name());
	}

	/**
	 * 1.21 {@code lookControl.rotateTowards(from, to, maxDelta)}: from turned towards to by at most maxDelta degrees (the
	 * mods' look controls call it as super: Iron's Spells' Dead King, keeper and cultists).
	 */
	public static float rotateTowards(net.minecraft.entity.ai.EntityLookHelper control, float from, float to, float maxDelta) {
		float delta = net.minecraft.util.math.MathHelper.wrapDegrees(to - from);
		return from + net.minecraft.util.math.MathHelper.clamp(delta, -maxDelta, maxDelta);
	}

	/**
	 * 1.21 {@code difficultyInstance.getDifficulty()}: the world's difficulty it was made for (1.12.2 keeps it private).
	 * Twilight Forest's mobs pick their gear by it.
	 */
	public static net.minecraft.world.EnumDifficulty getDifficulty(net.minecraft.world.DifficultyInstance instance) {
		for (java.lang.reflect.Field field : net.minecraft.world.DifficultyInstance.class.getDeclaredFields()) {
			if (field.getType() == net.minecraft.world.EnumDifficulty.class) {
				try {
					field.setAccessible(true);
					return (net.minecraft.world.EnumDifficulty) field.get(instance);
				} catch (ReflectiveOperationException | RuntimeException e) {
					break;
				}
			}
		}
		return net.minecraft.world.EnumDifficulty.NORMAL;
	}

	/** 1.21 {@code serverLevel.findNearestMapStructure(tag, pos, radius, skipKnown)}: by the mods' structure sets. */
	@javax.annotation.Nullable
	public static net.minecraft.util.math.BlockPos findNearestMapStructure(net.minecraft.world.WorldServer world,
			io.github.xponer2.translator.util.TagKey<?> tag, net.minecraft.util.math.BlockPos pos, int radius, boolean skipKnown) {
		return io.github.xponer2.translator.world.StructureGeneration.findNearest(world, tag.location(), pos, radius);
	}

	/** 1.21 {@code chunkSource.getGeneratorState()}: the world's ChunkGeneratorStructureState (the mods' structure sets). */
	@javax.annotation.Nullable
	public static Object getGeneratorState(net.minecraft.world.gen.ChunkProviderServer source) {
		return io.github.xponer2.translator.world.StructureGeneration.generatorState(source.world);
	}

	/** 1.21 {@code serverLevel.structureManager()}: the world's 1.21 StructureManager over the mods' structures. */
	public static Object structureManager(net.minecraft.world.WorldServer world) {
		return io.github.xponer2.translator.world.StructureGeneration.structureManager(world);
	}
}
