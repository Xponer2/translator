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

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Entity calls of 1.21.1 mods that 1.12.2 entities lack (getters and small state; what 1.12.2 doesn't have, like
 * freezing or spin attacks, answers as an entity without it).
 */
public final class EntityGapBridges {
	private EntityGapBridges() {
	}

	/** 1.21 {@code player.inventoryMenu}: 1.12.2's inventory container (a ContainerPlayer, declared as Container). */
	@javax.annotation.Nullable
	public static net.minecraft.inventory.ContainerPlayer inventoryContainer(net.minecraft.entity.EntityLivingBase entity) {
		return entity instanceof net.minecraft.entity.player.EntityPlayer player && player.inventoryContainer instanceof net.minecraft.inventory.ContainerPlayer menu
				? menu : null;
	}

	// --- 1.21 goals whose 1.12.2 classes take other arguments ---------------------------------------------------------

	private static <T> com.google.common.base.Predicate<T> guava(@javax.annotation.Nullable java.util.function.Predicate<T> predicate) {
		return predicate == null ? null : predicate::test;
	}

	/** 1.21 {@code new HurtByTargetGoal(mob, ignored...)}: 1.12.2's, not calling others for help (setAlertOthers does). */
	public static net.minecraft.entity.ai.EntityAIHurtByTarget newEntityAIHurtByTarget(net.minecraft.entity.EntityCreature mob, Class<?>[] ignored) {
		return new net.minecraft.entity.ai.EntityAIHurtByTarget(mob, false, ignored);
	}

	/** 1.21 {@code hurtByTarget.setAlertOthers(types...)}: 1.12.2 decides that when the goal is made. */
	public static net.minecraft.entity.ai.EntityAIHurtByTarget setAlertOthers(net.minecraft.entity.ai.EntityAIHurtByTarget goal, Class<?>[] types) {
		return goal;
	}

	/** 1.21 {@code new NearestAttackableTargetGoal(mob, type, interval, mustSee, mustReach, condition)}. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static net.minecraft.entity.ai.EntityAINearestAttackableTarget newEntityAINearestAttackableTarget(net.minecraft.entity.EntityLiving mob, Class type,
			int interval, boolean mustSee, boolean mustReach, @javax.annotation.Nullable java.util.function.Predicate condition) {
		if (mob instanceof net.minecraft.entity.EntityCreature creature) {
			return new net.minecraft.entity.ai.EntityAINearestAttackableTarget(creature, type, interval, mustSee, mustReach, guava(condition));
		}
		return new io.github.xponer2.translator.entity.LivingTargetGoal(mob, type, interval, mustSee, condition);
	}

	@SuppressWarnings("rawtypes")
	public static net.minecraft.entity.ai.EntityAINearestAttackableTarget newEntityAINearestAttackableTarget(net.minecraft.entity.EntityLiving mob, Class type,
			boolean mustSee) {
		return newEntityAINearestAttackableTarget(mob, type, 10, mustSee, false, null);
	}

	@SuppressWarnings("rawtypes")
	public static net.minecraft.entity.ai.EntityAINearestAttackableTarget newEntityAINearestAttackableTarget(net.minecraft.entity.EntityLiving mob, Class type,
			boolean mustSee, boolean mustReach) {
		return newEntityAINearestAttackableTarget(mob, type, 10, mustSee, mustReach, null);
	}

	@SuppressWarnings("rawtypes")
	public static net.minecraft.entity.ai.EntityAINearestAttackableTarget newEntityAINearestAttackableTarget(net.minecraft.entity.EntityLiving mob, Class type,
			boolean mustSee, @javax.annotation.Nullable java.util.function.Predicate condition) {
		return newEntityAINearestAttackableTarget(mob, type, 10, mustSee, false, condition);
	}

	/** 1.21 {@code new AvoidEntityGoal(mob, type, distance, walk, sprint, condition)}. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static net.minecraft.entity.ai.EntityAIAvoidEntity newEntityAIAvoidEntity(net.minecraft.entity.EntityCreature mob, Class type, float distance,
			double walkSpeed, double sprintSpeed, @javax.annotation.Nullable java.util.function.Predicate condition) {
		return new net.minecraft.entity.ai.EntityAIAvoidEntity(mob, type, condition == null ? com.google.common.base.Predicates.alwaysTrue() : guava(condition), distance,
				walkSpeed, sprintSpeed);
	}

	/** 1.21 {@code new RandomStrollGoal(mob, speed, interval, checkNoActionTime)}. */
	public static net.minecraft.entity.ai.EntityAIWander newEntityAIWander(net.minecraft.entity.EntityCreature mob, double speed, int interval, boolean checkNoActionTime) {
		return new net.minecraft.entity.ai.EntityAIWander(mob, speed, interval);
	}

	/** 1.21 {@code new LookAtPlayerGoal(mob, type, distance, probability, onlyHorizontal)}. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static net.minecraft.entity.ai.EntityAIWatchClosest newEntityAIWatchClosest(net.minecraft.entity.EntityLiving mob, Class type, float distance,
			float probability, boolean onlyHorizontal) {
		return new net.minecraft.entity.ai.EntityAIWatchClosest(mob, type, distance, probability);
	}

	/** 1.21 {@code living.getArmorCoverPercentage()}: the share of armor slots with something in them. */
	public static float getArmorCoverPercentage(net.minecraft.entity.EntityLivingBase entity) {
		int worn = 0;
		int slots = 0;
		for (net.minecraft.item.ItemStack stack : entity.getArmorInventoryList()) {
			slots++;
			if (!stack.isEmpty()) {
				worn++;
			}
		}
		return slots == 0 ? 0.0F : (float) worn / (float) slots;
	}

	/** 1.21 {@code slot.isArmor()}: a humanoid armor slot. */
	public static boolean isArmor(net.minecraft.inventory.EntityEquipmentSlot slot) {
		return slot.getSlotType() == net.minecraft.inventory.EntityEquipmentSlot.Type.ARMOR;
	}

	/**
	 * 1.21 {@code living.getAttributes()}: the entity's attribute map (1.12.2 getAttributeMap declares the abstract
	 * map, an AttributeMap on both sides; 7 mods read their gear's and spells' attributes from it).
	 */
	@javax.annotation.Nullable
	public static net.minecraft.entity.ai.attributes.AttributeMap getAttributes(net.minecraft.entity.EntityLivingBase entity) {
		return entity.getAttributeMap() instanceof net.minecraft.entity.ai.attributes.AttributeMap map ? map : null;
	}

	// --- Entity ---------------------------------------------------------------------------------------------------------

	public static boolean canBeHitByProjectile(Entity entity) {
		return entity.isEntityAlive() && entity.canBeCollidedWith();
	}

	/** Where a lead is tied (1.21: a bit below the eyes). */
	public static Vec3d getRopeHoldPosition(Entity entity, float partialTicks) {
		double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
		double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
		double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
		return new Vec3d(x, y + entity.getEyeHeight() * 0.7D, z);
	}

	public static boolean isInWaterRainOrBubble(Entity entity) {
		return entity.isWet();
	}

	/** 1.21 calls it when the entity leaves the world; 1.12.2 entities have nothing to do then. */
	public static void onRemovedFromLevel(Entity entity) {
	}

	/** 1.21 {@code setRemoved(reason)}: the entity is gone (killed, discarded, unloaded, changed dimension). */
	public static void setRemoved(Entity entity, Object reason) {
		entity.setDead();
	}

	/** 1.21 portals start a dimension change by this; 1.12.2 portals are blocks that do it themselves. */
	/** Ticks each entity has stood in a 1.21 portal, and the world tick it was last seen there. */
	private static final java.util.Map<Entity, long[]> IN_PORTAL = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

	/**
	 * 1.21 {@code entity.setAsInsidePortal(portal, pos)} (a mod's portal block calls it each tick the entity is in it):
	 * once the entity has stood there the portal's transition time, the portal's destination (its
	 * {@code getPortalDestination}, a DimensionTransition) moves it, to another dimension through Forge's teleporter.
	 * Twilight Forest's portal does nothing without it.
	 */
	public static void setAsInsidePortal(Entity entity, Object portal, BlockPos pos) {
		if (entity.world.isRemote || !(entity.world instanceof net.minecraft.world.WorldServer level) || entity.isRiding() || entity.isBeingRidden()) {
			return;
		}
		if (entity.timeUntilPortal > 0) {
			entity.timeUntilPortal = entity.getPortalCooldown();
			return;
		}
		long now = level.getTotalWorldTime();
		long[] state = IN_PORTAL.computeIfAbsent(entity, key -> new long[] {0, Long.MIN_VALUE});
		if (state[1] == now) {
			return;
		}
		state[0] = state[1] == now - 1 ? state[0] + 1 : 1;
		state[1] = now;
		int needed = entity instanceof EntityPlayer player && !player.capabilities.isCreativeMode ? 80 : 1;
		Object time = call(portal, "getPortalTransitionTime", level, entity);
		if (time instanceof Integer ticks) {
			needed = Math.max(1, ticks);
		}
		if (state[0] < needed) {
			return;
		}
		IN_PORTAL.remove(entity);
		entity.timeUntilPortal = entity.getPortalCooldown();
		Object transition = call(portal, "getPortalDestination", level, entity, pos);
		if (transition == null) {
			return;
		}
		Object target = call(transition, "newLevel");
		Object position = call(transition, "pos");
		if (!(target instanceof net.minecraft.world.WorldServer destination) || !(position instanceof Vec3d at)) {
			return;
		}
		float yaw = call(transition, "yRot") instanceof Float f ? f : entity.rotationYaw;
		float pitch = call(transition, "xRot") instanceof Float f ? f : entity.rotationPitch;
		int dimension = destination.provider.getDimension();
		Entity moved = entity;
		if (entity.dimension != dimension) {
			moved = entity.changeDimension(dimension, (world, e, entityYaw) -> e.setLocationAndAngles(at.x, at.y, at.z, yaw, pitch));
		}
		if (moved instanceof net.minecraft.entity.player.EntityPlayerMP player) {
			player.connection.setPlayerLocation(at.x, at.y, at.z, yaw, pitch);
		} else if (moved != null) {
			moved.setLocationAndAngles(at.x, at.y, at.z, yaw, pitch);
		}
		Object after = call(transition, "postDimensionTransition");
		if (after != null && moved != null) {
			call(after, "onTransition", moved);
		}
	}

	/** A method of a carried 1.21 object (Portal, DimensionTransition) by name and argument count. */
	@javax.annotation.Nullable
	private static Object call(Object target, String name, Object... args) {
		for (var method : target.getClass().getMethods()) {
			if (method.getName().equals(name) && method.getParameterCount() == args.length) {
				try {
					method.setAccessible(true);
					return method.invoke(target, args);
				} catch (ReflectiveOperationException | IllegalArgumentException e) {
					return null;
				}
			}
		}
		return null;
	}

	public static IBlockState getBlockStateOn(Entity entity) {
		return entity.world.getBlockState(new BlockPos(entity.posX, entity.posY - 0.2D, entity.posZ));
	}

	public static Vec3d getKnownMovement(Entity entity) {
		return new Vec3d(entity.motionX, entity.motionY, entity.motionZ);
	}

	public static double getRandomX(Entity entity, double scale) {
		return entity.posX + entity.width * (2.0D * entity.world.rand.nextDouble() - 1.0D) * scale;
	}

	public static double getRandomY(Entity entity) {
		return entity.posY + entity.height * entity.world.rand.nextDouble();
	}

	public static double getRandomZ(Entity entity, double scale) {
		return entity.posZ + entity.width * (2.0D * entity.world.rand.nextDouble() - 1.0D) * scale;
	}

	/** The up direction of the entity's view (its look direction turned up by 90 degrees). */
	public static Vec3d getUpVector(Entity entity, float partialTicks) {
		float pitch = (entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks) - 90.0F;
		float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
		float yawCos = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
		float yawSin = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
		float pitchCos = -MathHelper.cos(-pitch * 0.017453292F);
		float pitchSin = MathHelper.sin(-pitch * 0.017453292F);
		return new Vec3d(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
	}

	// --- LivingEntity ---------------------------------------------------------------------------------------------------

	/** 1.21: alive, not invulnerable, not a spectator or creative player. */
	public static boolean canBeSeenAsEnemy(EntityLivingBase living) {
		if (living instanceof EntityPlayer player && (player.isSpectator() || player.isCreative())) {
			return false;
		}
		return living.isEntityAlive() && !living.getIsInvulnerable();
	}

	/** 1.12.2 has no freezing (powder snow). */
	public static boolean canFreeze(EntityLivingBase living) {
		return false;
	}

	public static AxisAlignedBB getHitbox(EntityLivingBase living) {
		return living.getEntityBoundingBox();
	}

	/** 1.12.2 has no riptide spin attack. */
	public static boolean isAutoSpinAttack(EntityLivingBase living) {
		return false;
	}

	/** 1.21 {@code lookAt(anchor, target)}: the head turned to the target, from the eyes. */
	public static void lookAt(EntityLivingBase living, Object anchor, Vec3d target) {
		double dx = target.x - living.posX;
		double dy = target.y - (living.posY + living.getEyeHeight());
		double dz = target.z - living.posZ;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		living.rotationPitch = MathHelper.wrapDegrees((float) -(MathHelper.atan2(dy, horizontal) * 57.2957763671875D));
		living.rotationYaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(dz, dx) * 57.2957763671875D) - 90.0F);
		living.rotationYawHead = living.rotationYaw;
		living.prevRotationPitch = living.rotationPitch;
		living.prevRotationYaw = living.rotationYaw;
	}

	// --- Mob, animals ---------------------------------------------------------------------------------------------------

	/** 1.21's aggressive flag only drives arm poses: 1.12.2 models don't have them. */
	public static void setAggressive(net.minecraft.entity.EntityLiving mob, boolean aggressive) {
	}

	public static boolean isOrderedToSit(net.minecraft.entity.passive.EntityTameable animal) {
		return animal.isSitting();
	}

	public static boolean canFallInLove(net.minecraft.entity.passive.EntityAnimal animal) {
		return !animal.isInLove();
	}

	public static IBlockState getCarriedBlock(net.minecraft.entity.monster.EntityEnderman enderman) {
		return enderman.getHeldBlockState();
	}

	/** 1.21 {@code navigation.isStuck()}: 1.12.2 navigation gives up instead of reporting it. */
	public static boolean isStuck(net.minecraft.pathfinding.PathNavigate navigation) {
		return false;
	}

	// --- Player ---------------------------------------------------------------------------------------------------------

	public static Iterable<ItemStack> getAllSlots(EntityLivingBase living) {
		return living.getEquipmentAndArmor();
	}

	/** 1.12.2 has no crawling or swimming pose. */
	public static boolean isVisuallySwimming(EntityLivingBase living) {
		return false;
	}

	public static BlockPos getRespawnPosition(net.minecraft.entity.player.EntityPlayerMP player) {
		return player.getBedLocation();
	}

	/** 1.21 {@code blockActionRestricted(level, pos, gameType)}: spectators can't, adventure players only where allowed. */
	public static boolean blockActionRestricted(EntityPlayer player, net.minecraft.world.World level, BlockPos pos, net.minecraft.world.GameType type) {
		if (type == net.minecraft.world.GameType.SPECTATOR) {
			return true;
		}
		return type == net.minecraft.world.GameType.ADVENTURE && !player.isAllowEdit();
	}

	// --- lightning ------------------------------------------------------------------------------------------------------

	/** 1.12.2 bolts have no cause or damage of their own (fire and 5 damage). */
	public static void setCause(net.minecraft.entity.effect.EntityLightningBolt bolt, net.minecraft.entity.player.EntityPlayerMP cause) {
	}

	public static void setDamage(net.minecraft.entity.effect.EntityLightningBolt bolt, float damage) {
	}

	/**
	 * 1.21 {@code entity.recreateFromPacket(packet)}: the entity set up from its spawn packet; on 1.12.2 the client
	 * does it itself when it makes the entity (mods call it as super from their own).
	 */
	public static void recreateFromPacket(Entity entity, Object packet) {
	}

	/** 1.21 {@code entity.getClassification(forSpawnCount)}: the 1.12.2 creature type the entity is (null: none). */
	@javax.annotation.Nullable
	public static net.minecraft.entity.EnumCreatureType getClassification(Entity entity, boolean forSpawnCount) {
		for (net.minecraft.entity.EnumCreatureType type : net.minecraft.entity.EnumCreatureType.values()) {
			if (entity.isCreatureType(type, forSpawnCount)) {
				return type;
			}
		}
		return null;
	}

	/** 1.21 {@code entity.getPickResult()}: the stack middle click gives (Forge 1.12.2 getPickedResult). */
	public static ItemStack getPickResult(Entity entity) {
		ItemStack stack = entity.getPickedResult(new net.minecraft.util.math.RayTraceResult(entity));
		return stack == null ? ItemStack.EMPTY : stack;
	}

	/** 1.21 {@code entity.isInFluidType()}: in water or lava (the 1.12.2 fluids an entity can be in). */
	public static boolean isInFluidType(Entity entity) {
		return entity.isInWater() || entity.isInLava();
	}

	public static boolean isInFluidType(Entity entity, Object test, boolean forAllTypes) {
		return isInFluidType(entity);
	}

	// --- goals: the carried 1.21 Goal of a mod's mob, run by the 1.12.2 task list -----------------------------------

	private static final java.util.Map<Object, net.minecraft.entity.ai.EntityAIBase> GOALS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

	/**
	 * 1.21 {@code goalSelector.addGoal(priority, goal)}: the Goal is the carried 1.21 class (a mod's mob extends it, or
	 * uses vanilla 1.21 goals), so the 1.12.2 list gets an adapter running it. Without it no mob of a 1.21.1 mod had
	 * any AI (the call was skipped).
	 */
	public static void addGoal(net.minecraft.entity.ai.EntityAITasks tasks, int priority, Object goal) {
		if (goal == null) {
			return;
		}
		tasks.addTask(priority, GOALS.computeIfAbsent(goal, CarriedGoal::new));
	}

	public static void removeGoal(net.minecraft.entity.ai.EntityAITasks tasks, Object goal) {
		var adapter = GOALS.get(goal);
		if (adapter != null) {
			tasks.removeTask(adapter);
		}
	}

	/** 1.21 {@code removeAllGoals(filter)}: the goals the filter takes (all, for a filter that isn't a Predicate). */
	@SuppressWarnings("unchecked")
	public static void removeAllGoals(net.minecraft.entity.ai.EntityAITasks tasks, Object filter) {
		for (var entry : new java.util.ArrayList<>(tasks.taskEntries)) {
			if (entry.action instanceof CarriedGoal carried
					&& (!(filter instanceof java.util.function.Predicate<?> test) || ((java.util.function.Predicate<Object>) test).test(carried.goal))) {
				tasks.removeTask(entry.action);
			}
		}
	}

	/** A carried 1.21 Goal as a 1.12.2 task: canUse, canContinueToUse, start, stop, tick, its flags as mutex bits. */
	private static final class CarriedGoal extends net.minecraft.entity.ai.EntityAIBase {
		private static final java.lang.invoke.MethodHandle CAN_USE = handle("canUse", boolean.class);
		private static final java.lang.invoke.MethodHandle CAN_CONTINUE = handle("canContinueToUse", boolean.class);
		private static final java.lang.invoke.MethodHandle INTERRUPTABLE = handle("isInterruptable", boolean.class);
		private static final java.lang.invoke.MethodHandle START = handle("start", void.class);
		private static final java.lang.invoke.MethodHandle STOP = handle("stop", void.class);
		private static final java.lang.invoke.MethodHandle TICK = handle("tick", void.class);
		private static final java.lang.invoke.MethodHandle FLAGS = handle("getFlags", java.util.EnumSet.class);

		final Object goal;

		CarriedGoal(Object goal) {
			this.goal = goal;
			int bits = 0;
			try {
				Object flags = FLAGS != null ? FLAGS.invoke(goal) : null;
				if (flags instanceof java.util.Set<?> set) {
					for (Object flag : set) {
						String name = ((Enum<?>) flag).name();
						bits |= "MOVE".equals(name) ? 1 : "LOOK".equals(name) ? 2 : "JUMP".equals(name) ? 4 : 0;
					}
				}
			} catch (Throwable e) {
				// no flags: runs beside every other task
			}
			this.setMutexBits(bits);
		}

		@javax.annotation.Nullable
		private static java.lang.invoke.MethodHandle handle(String name, Class<?> result) {
			try {
				Class<?> type = Class.forName("net.minecraft.world.entity.ai.goal.Goal", false, net.minecraft.launchwrapper.Launch.classLoader);
				return java.lang.invoke.MethodHandles.publicLookup().findVirtual(type, name, java.lang.invoke.MethodType.methodType(result))
						.asType(java.lang.invoke.MethodType.methodType(result, Object.class));
			} catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
				return null;
			}
		}

		private boolean test(@javax.annotation.Nullable java.lang.invoke.MethodHandle handle, boolean fallback) {
			if (handle == null) {
				return fallback;
			}
			try {
				return (boolean) handle.invokeExact(this.goal);
			} catch (RuntimeException | Error e) {
				io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.goal.getClass().getName() + " (goal)", e);
				return false;
			} catch (Throwable e) {
				return false;
			}
		}

		private void run(@javax.annotation.Nullable java.lang.invoke.MethodHandle handle) {
			if (handle == null) {
				return;
			}
			try {
				handle.invokeExact(this.goal);
			} catch (RuntimeException | Error e) {
				io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.goal.getClass().getName() + " (goal)", e);
			} catch (Throwable e) {
				// checked: none declared
			}
		}

		@Override
		public boolean shouldExecute() {
			return this.test(CAN_USE, false);
		}

		@Override
		public boolean shouldContinueExecuting() {
			return this.test(CAN_CONTINUE, false);
		}

		@Override
		public boolean isInterruptible() {
			return this.test(INTERRUPTABLE, true);
		}

		@Override
		public void startExecuting() {
			this.run(START);
		}

		@Override
		public void resetTask() {
			this.run(STOP);
		}

		@Override
		public void updateTask() {
			this.run(TICK);
		}
	}

	/** 1.21 {@code spawnAtLocation(item)}: one of the item dropped where the entity is (BOP's boats break into theirs). */
	@javax.annotation.Nullable
	public static net.minecraft.entity.item.EntityItem spawnAtLocation(Entity entity, io.github.xponer2.translator.util.ItemLike item) {
		return spawnAtLocation(entity, item, 0);
	}

	@javax.annotation.Nullable
	public static net.minecraft.entity.item.EntityItem spawnAtLocation(Entity entity, io.github.xponer2.translator.util.ItemLike item, int offset) {
		var dropped = io.github.xponer2.translator.util.ItemLike.item(item);
		return dropped == net.minecraft.init.Items.AIR ? null : entity.entityDropItem(new ItemStack(dropped), offset);
	}

	/** 1.21 {@code spawnAtLocation(stack)}: the stack dropped where the entity is (a mob's drops in its tick: Aether's moa). */
	@javax.annotation.Nullable
	public static net.minecraft.entity.item.EntityItem spawnAtLocation(Entity entity, ItemStack stack) {
		return spawnAtLocation(entity, stack, 0.0F);
	}

	@javax.annotation.Nullable
	public static net.minecraft.entity.item.EntityItem spawnAtLocation(Entity entity, ItemStack stack, float offset) {
		return stack == null || stack.isEmpty() ? null : entity.entityDropItem(stack, offset);
	}

	/**
	 * 1.21 {@code remove(reason)}: the entity is removed (1.12.2's setDead; the reason only says why). The table's
	 * setDead takes no reason, so the call stayed missing and the tick that made it was left out (Aether's cloud minion).
	 */
	public static void remove(Entity entity, Object reason) {
		entity.setDead();
	}

	/** 1.21 {@code LivingEntity.checkSlowFallDistance()}: a slow fall keeps the fall distance at most 1. */
	public static void checkSlowFallDistance(Entity entity) {
		if (entity.motionY > -0.5D && entity.fallDistance > 1.0F) {
			entity.fallDistance = 1.0F;
		}
	}
}
