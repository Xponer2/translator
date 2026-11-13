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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import io.github.xponer2.translator.registry.ResourceKey;
import io.github.xponer2.translator.runtime.RuntimeStubs;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootTable;

/**
 * 1.21 members of Entity, LivingEntity, Mob, PathfinderMob, Player and ServerPlayer that the 1.12.2 classes they map
 * to lack: what 1.21 does, on 1.12.2's fields (sleeping, multipart entities, loot and experience, poses, portals,
 * fluids, captured drops, equipment tables, the player's recipe book). A member a mod's entity declares itself
 * (getParts, shouldBeSaved, canAttack, ...) runs the entity's own method; its super call of it gets 1.21's.
 */
public final class EntityApiBridges {
	private EntityApiBridges() {
	}

	// --- an entity's own 1.21 method -------------------------------------------------------------------------------

	/** What {@link Own#call} gives when the entity has no method of its own (or the call is that method's super call). */
	private static final Object INHERITED = new Object();

	/**
	 * A 1.21 method a class below the 1.12.2 game classes declares (a mod's entity, a carried 1.21 class, a Translator
	 * facade), by name and parameter count, found once per class.
	 */
	private static final class Own extends ClassValue<Method> {
		private final String name;
		private final int parameters;

		Own(String name, int parameters) {
			this.name = name;
			this.parameters = parameters;
		}

		@Override
		@Nullable
		protected Method computeValue(Class<?> type) {
			for (Class<?> current = type; current != null && !isGame(current); current = current.getSuperclass()) {
				Method[] declared;
				try {
					declared = current.getDeclaredMethods();
				} catch (LinkageError | SecurityException e) {
					continue;
				}
				for (Method method : declared) {
					if (method.getName().equals(this.name) && method.getParameterCount() == this.parameters && !method.isBridge()
							&& !method.isSynthetic() && !Modifier.isStatic(method.getModifiers()) && !Modifier.isAbstract(method.getModifiers())) {
						try {
							method.setAccessible(true);
						} catch (RuntimeException e) {
							return null;
						}
						return method;
					}
				}
			}
			return null;
		}

		/** The entity's own method's result, or {@link #INHERITED}. */
		Object call(Object self, Object... args) {
			Method method = this.get(self.getClass());
			if (method == null || this.superCall(self.getClass())) {
				return INHERITED;
			}
			try {
				return method.invoke(self, args);
			} catch (InvocationTargetException e) {
				throw EntityApiBridges.<RuntimeException>sneaky(e.getCause());
			} catch (IllegalAccessException | IllegalArgumentException e) {
				RuntimeStubs.failed(self.getClass().getName() + "." + this.name, e);
				return INHERITED;
			}
		}

		/** Whether the bridge was called from the entity's own method of that name: its super call, which gets 1.21's. */
		private boolean superCall(Class<?> type) {
			for (StackTraceElement frame : new Throwable().getStackTrace()) {
				String owner = frame.getClassName();
				if (owner.startsWith(EntityApiBridges.class.getName())) {
					continue;
				}
				if (!frame.getMethodName().equals(this.name)) {
					return false;
				}
				for (Class<?> current = type; current != null && !isGame(current); current = current.getSuperclass()) {
					if (current.getName().equals(owner)) {
						return true;
					}
				}
				return false;
			}
			return false;
		}
	}

	/** A class of 1.12.2 or Forge: above every 1.21 class an entity's class extends. */
	private static boolean isGame(Class<?> type) {
		String name = type.getName();
		return type == Object.class || name.startsWith("net.minecraft.entity.") || name.startsWith("net.minecraft.client.entity.")
				|| name.startsWith("net.minecraftforge.");
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> T sneaky(Throwable thrown) throws T {
		throw (T) thrown;
	}

	// --- 1.12.2 members a static helper can't reach (by their names in development and in the game) -------------------

	@Nullable
	private static Field field(Class<?> owner, String mcp, String srg) {
		for (String name : new String[] {mcp, srg}) {
			try {
				Field field = owner.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException | RuntimeException e) {
				// the other name
			}
		}
		return null;
	}

	@Nullable
	private static Method method(Class<?> owner, String mcp, String srg, Class<?>... parameters) {
		for (String name : new String[] {mcp, srg}) {
			try {
				Method method = owner.getDeclaredMethod(name, parameters);
				method.setAccessible(true);
				return method;
			} catch (NoSuchMethodException | RuntimeException e) {
				// the other name
			}
		}
		return null;
	}

	@Nullable
	private static Object get(@Nullable Field field, Object target) {
		try {
			return field == null ? null : field.get(target);
		} catch (IllegalAccessException e) {
			return null;
		}
	}

	private static void set(@Nullable Field field, Object target, Object value) {
		try {
			if (field != null) {
				field.set(target, value);
			}
		} catch (IllegalAccessException | IllegalArgumentException e) {
			RuntimeStubs.failed("setting " + field.getName(), e);
		}
	}

	@Nullable
	private static Object invoke(@Nullable Method method, Object target, Object... args) {
		if (method == null) {
			return null;
		}
		try {
			return method.invoke(target, args);
		} catch (InvocationTargetException e) {
			throw EntityApiBridges.<RuntimeException>sneaky(e.getCause());
		} catch (IllegalAccessException | IllegalArgumentException e) {
			RuntimeStubs.failed(method.getName(), e);
			return null;
		}
	}

	private static final Field IN_WEB = field(Entity.class, "isInWeb", "field_70134_J");
	private static final Method SET_FLAG = method(Entity.class, "setFlag", "func_70052_a", int.class, boolean.class);
	private static final Method SET_SIZE = method(Entity.class, "setSize", "func_70105_a", float.class, float.class);
	private static final Method PLAY_STEP_SOUND = method(Entity.class, "playStepSound", "func_180429_a", BlockPos.class, net.minecraft.block.Block.class);
	private static final Field IDLE_TIME = field(EntityLivingBase.class, "idleTime", "field_70708_bq");
	private static final Field ATTACKING_PLAYER = field(EntityLivingBase.class, "attackingPlayer", "field_70717_bb");
	private static final Field RECENTLY_HIT = field(EntityLivingBase.class, "recentlyHit", "field_70718_bc");
	private static final Method EXPERIENCE_POINTS = method(EntityLivingBase.class, "getExperiencePoints", "func_70693_a", EntityPlayer.class);
	private static final Field DEATH_LOOT_TABLE = field(EntityLiving.class, "deathLootTable", "field_184659_bA");
	private static final Field DEATH_LOOT_TABLE_SEED = field(EntityLiving.class, "deathLootTableSeed", "field_184653_bB");
	private static final Method LOOT_TABLE = method(EntityLiving.class, "getLootTable", "func_184647_J");
	private static final Field HAND_DROP_CHANCES = field(EntityLiving.class, "inventoryHandsDropChances", "field_82174_bp");
	private static final Field ARMOR_DROP_CHANCES = field(EntityLiving.class, "inventoryArmorDropChances", "field_184655_bs");
	private static final Field SLEEPING_FLAG = field(EntityPlayer.class, "sleeping", "field_71083_bS");
	private static final Field SLEEP_TIMER = field(EntityPlayer.class, "sleepTimer", "field_71076_b");
	private static final Method SPAWN_SHOULDER_ENTITIES = method(EntityPlayer.class, "spawnShoulderEntities", "func_192030_dh");

	/** A method of a carried 1.21 object (a record's accessor) by name, null when it has none. */
	@Nullable
	private static Object accessor(Object target, String name) {
		try {
			Method method = target.getClass().getMethod(name);
			method.setAccessible(true);
			return method.invoke(target);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	@Nullable
	private static Object field(Object target, String name) {
		try {
			Field field = target.getClass().getField(name);
			return field.get(target);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	private static String poseName(@Nullable Object pose) {
		return pose instanceof Enum<?> value ? value.name() : "STANDING";
	}

	// --- Entity: position, movement, portals --------------------------------------------------------------------------

	/** 1.21 {@code entity.getX(scale)}: that share of the entity's width off its middle. */
	public static double getX(Entity entity, double scale) {
		return entity.posX + entity.width * scale;
	}

	/** 1.21 {@code entity.getZ(scale)}. */
	public static double getZ(Entity entity, double scale) {
		return entity.posZ + entity.width * scale;
	}

	/** 1.21 {@code entity.moveTo(pos, yaw, pitch)}: there, turned so, no movement in between (1.12.2's setLocationAndAngles). */
	public static void moveTo(Entity entity, Vec3d pos, float yaw, float pitch) {
		entity.setLocationAndAngles(pos.x, pos.y, pos.z, yaw, pitch);
	}

	/** 1.21 {@code entity.moveTo(blockPos, yaw, pitch)}: the middle of the block's bottom. */
	public static void moveTo(Entity entity, BlockPos pos, float yaw, float pitch) {
		entity.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, pitch);
	}

	/** 1.21 {@code entity.absMoveTo(x, y, z)}: there (inside the world border's range), no movement in between. */
	public static void absMoveTo(Entity entity, double x, double y, double z) {
		double clampedX = MathHelper.clamp(x, -3.0E7D, 3.0E7D);
		double clampedZ = MathHelper.clamp(z, -3.0E7D, 3.0E7D);
		entity.prevPosX = clampedX;
		entity.prevPosY = y;
		entity.prevPosZ = clampedZ;
		entity.setPosition(clampedX, y, clampedZ);
	}

	/**
	 * 1.21 {@code entity.syncPacketPositionCodec(x, y, z)}: the base the next relative move packets start from (1.12.2's
	 * server position of the entity: JourneyMap's radar players).
	 */
	public static void syncPacketPositionCodec(Entity entity, double x, double y, double z) {
		entity.serverPosX = net.minecraft.entity.EntityTracker.getPositionLong(x);
		entity.serverPosY = net.minecraft.entity.EntityTracker.getPositionLong(y);
		entity.serverPosZ = net.minecraft.entity.EntityTracker.getPositionLong(z);
	}

	/** 1.21 {@code entity.setPortalCooldown()}: no portal takes it for the entity's dimension change delay. */
	public static void setPortalCooldown(Entity entity) {
		entity.timeUntilPortal = entity.getPortalCooldown();
	}

	public static void setPortalCooldown(Entity entity, int ticks) {
		entity.timeUntilPortal = ticks;
	}

	/** 1.21 {@code entity.placePortalTicket(pos)}: the chunk of the portal it arrives at is loaded. */
	public static void placePortalTicket(Entity entity, BlockPos pos) {
		if (entity.world instanceof WorldServer world) {
			world.getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
		}
	}

	/**
	 * 1.21 {@code entity.getRelativePortalPosition(axis, portal)}: where in the portal the entity stands, as shares of its
	 * width and height and the offset across it (Aether's portal keeps that place on the other side).
	 */
	public static Vec3d getRelativePortalPosition(Entity entity, EnumFacing.Axis axis, Object portal) {
		BlockPos corner = field(portal, "minCorner") instanceof BlockPos pos ? pos : new BlockPos(entity);
		int width = field(portal, "axis1Size") instanceof Integer size ? size : 1;
		int height = field(portal, "axis2Size") instanceof Integer size ? size : 1;
		double room = width - (double) entity.width;
		double headroom = height - (double) entity.height;
		double along = 0.5D;
		if (room > 0.0D) {
			double start = coordinate(corner, axis) + entity.width / 2.0D;
			along = MathHelper.clamp((coordinate(entity, axis) - start) / room, 0.0D, 1.0D);
		}
		double up = headroom > 0.0D ? MathHelper.clamp((entity.posY - corner.getY()) / headroom, 0.0D, 1.0D) : 0.0D;
		EnumFacing.Axis across = axis == EnumFacing.Axis.X ? EnumFacing.Axis.Z : EnumFacing.Axis.X;
		return new Vec3d(along, up, coordinate(entity, across) - (coordinate(corner, across) + 0.5D));
	}

	private static double coordinate(BlockPos pos, EnumFacing.Axis axis) {
		return axis == EnumFacing.Axis.X ? pos.getX() : axis == EnumFacing.Axis.Y ? pos.getY() : pos.getZ();
	}

	private static double coordinate(Entity entity, EnumFacing.Axis axis) {
		return axis == EnumFacing.Axis.X ? entity.posX : axis == EnumFacing.Axis.Y ? entity.posY : entity.posZ;
	}

	/** 1.21 {@code entity.isInRain()}: rain falls on its feet or its head. */
	public static boolean isInRain(Entity entity) {
		BlockPos pos = new BlockPos(entity);
		return entity.world.isRainingAt(pos) || entity.world.isRainingAt(new BlockPos(pos.getX(), entity.getEntityBoundingBox().maxY, pos.getZ()));
	}

	/** 1.21 {@code entity.isInWaterOrBubble()}: 1.12.2 has no bubble columns. */
	public static boolean isInWaterOrBubble(Entity entity) {
		return entity.isInWater();
	}

	/** 1.21 {@code entity.isDescending()}: its shift key is down. */
	public static boolean isDescending(Entity entity) {
		return entity.isSneaking();
	}

	/** 1.21 {@code entity.getNearestViewDirection()}: the side it looks most towards. */
	public static EnumFacing getNearestViewDirection(Entity entity) {
		Vec3d look = entity.getLook(1.0F);
		return EnumFacing.getFacingFromVector((float) look.x, (float) look.y, (float) look.z);
	}

	/** 1.21 {@code entity.extinguishFire()}: put out, with the hiss when it burned. */
	public static void extinguishFire(Entity entity) {
		if (!entity.world.isRemote && entity.isBurning()) {
			entity.playSound(net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.7F,
					1.6F + (entity.world.rand.nextFloat() - entity.world.rand.nextFloat()) * 0.4F);
		}
		entity.extinguish();
	}

	/** 1.21 {@code entity.getPercentFrozen()}: its freeze ticks as a share of what freezes it. */
	public static float getPercentFrozen(Entity entity) {
		int needed = LevelGapBridges.getTicksRequiredToFreeze(entity);
		return needed <= 0 ? 0.0F : (float) Math.min(LevelGapBridges.getTicksFrozen(entity), needed) / needed;
	}

	/** 1.21 {@code entity.revive()}: no longer removed. */
	public static void revive(Entity entity) {
		entity.isDead = false;
	}

	private static final Vec3d IN_WEB_MULTIPLIER = new Vec3d(0.25D, 0.05D, 0.25D);

	/** 1.21 {@code entity.stuckSpeedMultiplier}: 1.12.2's cobweb slowdown of this tick (zero when not stuck). */
	public static Vec3d stuckSpeedMultiplier(Entity entity) {
		return Boolean.TRUE.equals(get(IN_WEB, entity)) ? IN_WEB_MULTIPLIER : Vec3d.ZERO;
	}

	/** Writing it: zero frees the entity (Twilight Forest's boots), any other value sticks it as a cobweb does. */
	public static void stuckSpeedMultiplier(Entity entity, @Nullable Vec3d multiplier) {
		set(IN_WEB, entity, multiplier != null && multiplier.lengthSquared() > 1.0E-7D);
	}

	/** 1.21 {@code entity.verticalCollisionBelow}: its last move hit ground below (1.12.2's onGround is just that). */
	public static boolean verticalCollisionBelow(Entity entity) {
		return entity.onGround;
	}

	// --- Entity: riding ------------------------------------------------------------------------------------------------

	/** 1.21 {@code entity.getControlledVehicle()}: what it rides and steers, else null. */
	@Nullable
	public static Entity getControlledVehicle(Entity entity) {
		Entity vehicle = entity.getRidingEntity();
		return vehicle != null && vehicle.getControllingPassenger() == entity ? vehicle : null;
	}

	/** 1.21 {@code entity.hasPassenger(test)}: a passenger passes the test. */
	public static boolean hasPassenger(Entity entity, java.util.function.Predicate<Entity> test) {
		for (Entity passenger : entity.getPassengers()) {
			if (test.test(passenger)) {
				return true;
			}
		}
		return false;
	}

	private static final Own VEHICLE_ATTACHMENT_POINT = new Own("getVehicleAttachmentPoint", 1);

	/**
	 * 1.21 {@code entity.getVehicleAttachmentPoint(vehicle)}: the point of the rider that sits on the vehicle's seat,
	 * from its feet (1.12.2's riding offset: a player sits 0.35 deep).
	 */
	public static Vec3d getVehicleAttachmentPoint(Entity entity, Entity vehicle) {
		Object own = VEHICLE_ATTACHMENT_POINT.call(entity, vehicle);
		return own instanceof Vec3d point ? point : new Vec3d(0.0D, -entity.getYOffset(), 0.0D);
	}

	private static final Own POSITION_RIDER = new Own("positionRider", 2);
	private static final Own RIDING_POSITION = new Own("getPassengerRidingPosition", 1);
	private static final Own ATTACHMENT_POINT = new Own("getPassengerAttachmentPoint", 3);

	/**
	 * 1.12.2's {@code updatePassenger(passenger)} of every entity starts here (core hook): a vehicle whose class has
	 * 1.21's rider placing of its own (positionRider, getPassengerRidingPosition, getPassengerAttachmentPoint) places its
	 * riders by it, as 1.21 does; true when it did. 1.12.2 never called them: Twilight Forest's trolls and goblin knights,
	 * Iron's Spells' ice spiders, the seats of Create and Handcrafted, Aether's moas kept 1.12.2's seat.
	 */
	public static boolean placesRiders(Entity vehicle, Entity passenger) {
		Class<?> type = vehicle.getClass();
		if (POSITION_RIDER.get(type) == null && RIDING_POSITION.get(type) == null && ATTACHMENT_POINT.get(type) == null) {
			return false;
		}
		if (!vehicle.isPassenger(passenger)) {
			return true;
		}
		try {
			positionRider(vehicle, passenger, MoveFunctions.SET_POSITION.get());
			return true;
		} catch (RuntimeException | LinkageError e) {
			RuntimeStubs.failed(type.getName() + ".positionRider", e);
			return false;
		}
	}

	/**
	 * 1.21 {@code vehicle.positionRider(passenger, move)}: the vehicle's own, else (and for its super call) the rider's
	 * seat point on the vehicle's riding position.
	 */
	public static void positionRider(Entity vehicle, Entity passenger, @Nullable Object move) {
		if (POSITION_RIDER.call(vehicle, passenger, move) != INHERITED) {
			return;
		}
		Vec3d riding = getPassengerRidingPosition(vehicle, passenger);
		Vec3d seat = getVehicleAttachmentPoint(passenger, vehicle);
		MoveFunctions.accept(move, passenger, riding.x - seat.x, riding.y - seat.y, riding.z - seat.z);
	}

	/** 1.21 {@code vehicle.positionRider(passenger)}: placed as above, by setPosition. */
	public static void positionRider(Entity vehicle, Entity passenger) {
		if (vehicle.isPassenger(passenger)) {
			positionRider(vehicle, passenger, MoveFunctions.SET_POSITION.get());
		}
	}

	/**
	 * 1.21 {@code vehicle.getPassengerRidingPosition(passenger)}: the vehicle's own, else its position plus its passenger
	 * attachment point, turned with a living vehicle's body.
	 */
	public static Vec3d getPassengerRidingPosition(Entity vehicle, Entity passenger) {
		Object own = RIDING_POSITION.call(vehicle, passenger);
		if (own instanceof Vec3d position) {
			return position;
		}
		Vec3d point = getPassengerAttachmentPoint(vehicle, passenger, CarriedBridges.getDimensions(vehicle, null), 1.0F);
		if (vehicle instanceof EntityLivingBase living) {
			point = point.rotateYaw(-living.renderYawOffset * 0.017453292F);
		}
		return new Vec3d(vehicle.posX + point.x, vehicle.posY + point.y, vehicle.posZ + point.z);
	}

	/**
	 * 1.21 {@code vehicle.getPassengerAttachmentPoint(passenger, dimensions, scale)}: the vehicle's own, else 1.12.2's
	 * seat height (getMountedYOffset).
	 */
	public static Vec3d getPassengerAttachmentPoint(Entity vehicle, Entity passenger, @Nullable Object dimensions, float scale) {
		Object own = ATTACHMENT_POINT.call(vehicle, passenger, dimensions, scale);
		return own instanceof Vec3d point ? point : new Vec3d(0.0D, vehicle.getMountedYOffset(), 0.0D);
	}

	/** 1.21's {@code Entity.MoveFunction} (carried): how a placed rider is moved. */
	private static final class MoveFunctions {
		private static final String TYPE = "net.minecraft.world.entity.Entity$MoveFunction";
		/** One that sets the rider's position (1.21's {@code Entity::setPos}); null when the interface isn't carried. */
		static final java.util.function.Supplier<Object> SET_POSITION = com.google.common.base.Suppliers.memoize(() -> {
			try {
				Class<?> type = Class.forName(TYPE, true, net.minecraft.launchwrapper.Launch.classLoader);
				return java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
					switch (method.getName()) {
						case "accept":
							((Entity) args[0]).setPosition((Double) args[1], (Double) args[2], (Double) args[3]);
							return null;
						case "equals":
							return proxy == args[0];
						case "hashCode":
							return System.identityHashCode(proxy);
						default:
							return "MoveFunction[setPosition]";
					}
				});
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				RuntimeStubs.failed(TYPE, e);
				return null;
			}
		})::get;
		private static final ClassValue<Method> ACCEPT = new ClassValue<Method>() {
			@Override
			@Nullable
			protected Method computeValue(Class<?> type) {
				for (Class<?> itf : type.getInterfaces()) {
					if (TYPE.equals(itf.getName())) {
						try {
							return itf.getMethod("accept", Entity.class, double.class, double.class, double.class);
						} catch (NoSuchMethodException e) {
							return null;
						}
					}
				}
				return null;
			}
		};

		private MoveFunctions() {
		}

		/** {@code move.accept(entity, x, y, z)}; without a move function the entity is set there. */
		static void accept(@Nullable Object move, Entity entity, double x, double y, double z) {
			Method accept = move == null || move == SET_POSITION.get() ? null : ACCEPT.get(move.getClass());
			if (accept == null) {
				entity.setPosition(x, y, z);
				return;
			}
			try {
				accept.invoke(move, entity, x, y, z);
			} catch (InvocationTargetException e) {
				throw EntityApiBridges.<RuntimeException>sneaky(e.getCause());
			} catch (IllegalAccessException e) {
				entity.setPosition(x, y, z);
			}
		}
	}

	/** 1.21 {@code mob.setLeashedTo(holder, sendPacket)}: 1.12.2's lead (Twilight Forest's leashed zombies of the lich tower). */
	public static void setLeashedTo(Entity entity, @Nullable Entity holder, boolean sendPacket) {
		if (entity instanceof EntityLiving living && holder != null) {
			living.setLeashHolder(holder, sendPacket);
		}
	}

	/** 1.21 {@code tamable.setOrderedToSit(sit)}: 1.12.2's sitting, its sit goal too. */
	public static void setOrderedToSit(net.minecraft.entity.passive.EntityTameable tamable, boolean sit) {
		tamable.setSitting(sit);
		if (tamable.getAISit() != null) {
			tamable.getAISit().setSitting(sit);
		}
	}

	/** 1.21 {@code tamable.isOrderedToSit()}. */
	public static boolean isOrderedToSit(net.minecraft.entity.passive.EntityTameable tamable) {
		return tamable.isSitting();
	}

	private static final Field HORSE_CHEST = field(net.minecraft.entity.passive.AbstractHorse.class, "horseChest", "field_110296_bG");

	/** 1.21 {@code horse.equipSaddle(stack, sound)}: the saddle into its saddle slot (1.12.2 then saddles it), with the sound. */
	public static void equipSaddle(net.minecraft.entity.passive.AbstractHorse horse, ItemStack stack, @Nullable Object source) {
		if (get(HORSE_CHEST, horse) instanceof net.minecraft.inventory.IInventory chest && chest.getSizeInventory() > 0) {
			chest.setInventorySlotContents(0, stack);
		} else {
			horse.setHorseSaddled(true);
		}
		if (source != null) {
			horse.playSound(net.minecraft.init.SoundEvents.ENTITY_HORSE_SADDLE, 0.5F, 1.0F);
		}
	}

	/** 1.21 {@code boat.setVariant(type)}: 1.12.2's boat type (Supplementaries' plunderers' boats). */
	public static void setVariant(net.minecraft.entity.item.EntityBoat boat, @Nullable net.minecraft.entity.item.EntityBoat.Type type) {
		if (type != null) {
			boat.setBoatType(type);
		}
	}

	/** 1.21 {@code boat.getVariant()}. */
	public static net.minecraft.entity.item.EntityBoat.Type getVariant(net.minecraft.entity.item.EntityBoat boat) {
		return boat.getBoatType();
	}

	/** 1.21 {@code mob.wantsToPickUp(stack)}: 1.21's default (canHoldItem: any stack). */
	public static boolean wantsToPickUp(EntityLiving mob, ItemStack stack) {
		return !stack.isEmpty();
	}

	/**
	 * 1.21 {@code mob.equipItemIfPossible(stack)}: worn in its slot when that slot is empty (then always dropped), the
	 * stack it put on, else none.
	 */
	public static ItemStack equipItemIfPossible(EntityLiving mob, ItemStack stack) {
		EntityEquipmentSlot slot = EntityLiving.getSlotForItemStack(stack);
		if (stack.isEmpty() || !mob.getItemStackFromSlot(slot).isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack worn = stack.copy();
		worn.setCount(1);
		mob.setItemStackToSlot(slot, worn);
		mob.setDropChance(slot, 2.0F);
		mob.enablePersistence();
		return worn;
	}

	private static final Method DO_BLOCK_COLLISIONS = method(Entity.class, "doBlockCollisions", "func_145775_I");

	/**
	 * 1.21 {@code entity.checkInsideBlocks()}: the blocks its box is in act on it (1.12.2's doBlockCollisions: cobwebs,
	 * pressure plates, portals).
	 */
	public static void checkInsideBlocks(Entity entity) {
		if (DO_BLOCK_COLLISIONS != null) {
			try {
				DO_BLOCK_COLLISIONS.invoke(entity);
			} catch (InvocationTargetException e) {
				throw EntityApiBridges.<RuntimeException>sneaky(e.getCause());
			} catch (IllegalAccessException e) {
				RuntimeStubs.failed("Entity.doBlockCollisions", e);
			}
		}
	}

	/** 1.21 {@code entity.tryCheckInsideBlocks()} (Moonlight's projectiles, every tick): as above, a failure reported. */
	public static void tryCheckInsideBlocks(Entity entity) {
		try {
			checkInsideBlocks(entity);
		} catch (RuntimeException e) {
			RuntimeStubs.failed(entity.getClass().getName() + ".checkInsideBlocks", e);
		}
	}

	/**
	 * 1.21 {@code entity.maybeBackOffFromEdge(movement, type)}: a sneaking player stops at an edge; anything else moves
	 * as asked (the move is left to 1.12.2's own edge check). Stubbed it gave no movement and Moonlight's projectiles
	 * failed in every move.
	 */
	public static Vec3d maybeBackOffFromEdge(Entity entity, Vec3d movement, @Nullable Object type) {
		return movement;
	}

	/** 1.21 {@code living.canUseSlot(slot)}: every slot (1.21's default; a body slot only horses and wolves have). */
	public static boolean canUseSlot(EntityLivingBase living, @Nullable EntityEquipmentSlot slot) {
		return true;
	}

	/** 1.21 {@code entity.deflection(projectile)}: none (1.21's default; breezes and the like deflect, 1.12.2 has none). */
	@Nullable
	public static Object deflection(Entity entity, @Nullable Object projectile) {
		try {
			return CarriedBridges.carried("net.minecraft.world.entity.projectile.ProjectileDeflection").getField("NONE").get(null);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	/**
	 * 1.21 {@code mob.isSunBurnTick()}: by day on the server, in bright sky light, by chance as the light is brighter,
	 * when not wet (Twilight Forest's and Supplementaries' undead burn as skeletons do).
	 */
	public static boolean isSunBurnTick(EntityLiving mob) {
		World world = mob.world;
		if (world == null || world.isRemote || !world.isDaytime()) {
			return false;
		}
		float brightness = mob.getBrightness();
		BlockPos pos = mob.getRidingEntity() instanceof net.minecraft.entity.item.EntityBoat
				? new BlockPos(mob.posX, Math.round(mob.posY), mob.posZ).up()
				: new BlockPos(mob.posX, Math.round(mob.posY), mob.posZ);
		return brightness > 0.5F && mob.getRNG().nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F && !mob.isWet() && world.canSeeSky(pos);
	}

	/** 1.21 {@code level.getRaidAt(pos)}: 1.12.2 has no raids. */
	@Nullable
	public static Object getRaidAt(WorldServer world, @Nullable BlockPos pos) {
		return null;
	}

	/** 1.21 {@code level.isRaided(pos)}: no raids in 1.12.2. */
	public static boolean isRaided(WorldServer world, @Nullable BlockPos pos) {
		return false;
	}

	/** 1.21 {@code spiderGroupData.effect} (read; a Holder in 1.21, a Potion in 1.12.2): the effect as a holder. */
	@Nullable
	public static io.github.xponer2.translator.registry.Holder<?> effect(net.minecraft.entity.monster.EntitySpider.GroupData data) {
		return data.effect == null ? null : io.github.xponer2.translator.registry.Holder.direct(data.effect);
	}

	/** Writing it (Twilight Forest's hedge spiders give their own effects). */
	public static void effect(net.minecraft.entity.monster.EntitySpider.GroupData data, @Nullable io.github.xponer2.translator.registry.Holder<?> effect) {
		data.effect = effect != null && effect.value() instanceof net.minecraft.potion.Potion potion ? potion : null;
	}

	// --- Entity: 1.21 fields 1.12.2 hasn't ------------------------------------------------------------------------------

	private static final Map<Entity, boolean[]> FLAGS = Collections.synchronizedMap(new WeakHashMap<>());
	private static final int WAS_ON_FIRE = 0;
	private static final int MINOR_COLLISION = 1;

	private static boolean flag(Entity entity, int index) {
		boolean[] flags = FLAGS.get(entity);
		return flags != null && flags[index];
	}

	private static void flag(Entity entity, int index, boolean value) {
		boolean[] flags = FLAGS.get(entity);
		if (flags == null) {
			if (!value) {
				return;
			}
			flags = new boolean[2];
			FLAGS.put(entity, flags);
		}
		flags[index] = value;
	}

	/** 1.21 {@code entity.isInPowderSnow} (read): 1.12.2 has no powder snow. */
	public static boolean isInPowderSnow(Entity entity) {
		return false;
	}

	/** Writing it: nothing to keep. */
	public static void isInPowderSnow(Entity entity, boolean value) {
	}

	/** 1.21 {@code entity.wasOnFire} (read): what the entity's own tick kept (Moonlight's projectiles). */
	public static boolean wasOnFire(Entity entity) {
		return flag(entity, WAS_ON_FIRE);
	}

	public static void wasOnFire(Entity entity, boolean value) {
		flag(entity, WAS_ON_FIRE, value);
	}

	/** 1.21 {@code entity.minorHorizontalCollision} (read): what the entity's own move kept. */
	public static boolean minorHorizontalCollision(Entity entity) {
		return flag(entity, MINOR_COLLISION);
	}

	public static void minorHorizontalCollision(Entity entity, boolean value) {
		flag(entity, MINOR_COLLISION, value);
	}

	private static final Own SHOW_VEHICLE_HEALTH = new Own("showVehicleHealth", 0);

	/** 1.21 {@code entity.showVehicleHealth()}: a living mount's health shows (the entity's own answer first). */
	public static boolean showVehicleHealth(Entity entity) {
		Object own = SHOW_VEHICLE_HEALTH.call(entity);
		return own instanceof Boolean shown ? shown : entity instanceof EntityLivingBase;
	}

	private static final Own LEASH_OFFSET = new Own("getLeashOffset", 0);
	private static final Own LEASH_OFFSET_AT = new Own("getLeashOffset", 1);

	/** 1.21 {@code entity.getLeashOffset(partialTick)}: where a lead is tied, its own or below the eyes at the front. */
	public static Vec3d getLeashOffset(Entity entity, float partialTick) {
		Object own = LEASH_OFFSET_AT.call(entity, partialTick);
		if (own instanceof Vec3d offset) {
			return offset;
		}
		own = LEASH_OFFSET.call(entity);
		return own instanceof Vec3d offset ? offset : new Vec3d(0.0D, entity.getEyeHeight(), entity.width * 0.4F);
	}

	// --- Entity: what a mod's entity says of itself --------------------------------------------------------------------

	private static final Own PARTS = new Own("getParts", 0);

	/** NeoForge {@code entity.getParts()}: a multipart mod entity's parts (Twilight Forest's bosses), else null. */
	@Nullable
	public static Object[] getParts(Entity entity) {
		Object own = PARTS.call(entity);
		return own instanceof Object[] parts && !parts.getClass().getComponentType().getName().startsWith("net.minecraft.entity.") ? parts : null;
	}

	private static final Own SHOULD_BE_SAVED = new Own("shouldBeSaved", 0);

	/** 1.21 {@code entity.shouldBeSaved()}: not removed, not a player, not riding, not a player's lone mount. */
	public static boolean shouldBeSaved(Entity entity) {
		Object own = SHOULD_BE_SAVED.call(entity);
		if (own instanceof Boolean saved) {
			return saved;
		}
		if (entity instanceof EntityPlayer || entity.isDead || entity.isRiding()) {
			return false;
		}
		List<Entity> passengers = entity.getPassengers();
		return passengers.size() != 1 || !(passengers.get(0) instanceof EntityPlayer);
	}

	/**
	 * 1.21 {@code entity.getRemovalReason()}: null while it is in the world; a removed living entity with no health was
	 * killed, any other was discarded (1.12.2 marks both dead).
	 */
	@Nullable
	public static Object getRemovalReason(Entity entity) {
		if (!entity.isDead) {
			return null;
		}
		boolean killed = entity instanceof EntityLivingBase living && living.getHealth() <= 0.0F;
		return CarriedBridges.constant("net.minecraft.world.entity.Entity$RemovalReason", killed ? "KILLED" : "DISCARDED");
	}

	private static final Own CAN_SPRINT = new Own("canSprint", 0);

	/** 1.21 {@code entity.canSprint()}: players can (the entity's own answer first). */
	public static boolean canSprint(Entity entity) {
		Object own = CAN_SPRINT.call(entity);
		return own instanceof Boolean can ? can : entity instanceof EntityPlayer;
	}

	private static final Own ADDED_TO_LEVEL = new Own("onAddedToLevel", 0);
	private static final Own CLIENT_REMOVAL = new Own("onClientRemoval", 0);

	/** NeoForge {@code entity.onAddedToLevel()}: the entity's own hook, else 1.12.2's onAddedToWorld. */
	public static void onAddedToLevel(Entity entity) {
		if (ADDED_TO_LEVEL.call(entity) == INHERITED) {
			entity.onAddedToWorld();
		}
	}

	/** 1.21 {@code entity.onClientRemoval()}: the entity's own hook (1.21's default does nothing). */
	public static void onClientRemoval(Entity entity) {
		CLIENT_REMOVAL.call(entity);
	}

	// --- Entity: poses and sizes ---------------------------------------------------------------------------------------

	/** 1.21 {@code entity.hasPose(pose)}. */
	public static boolean hasPose(Entity entity, @Nullable Object pose) {
		return pose != null && pose == CarriedBridges.getPose(entity);
	}

	/**
	 * 1.21 {@code entity.getEyeHeight(pose)}: a player's eyes in each pose (sneaking as 1.12.2 lowers them), a sleeper's
	 * at 0.2, else the entity's.
	 */
	public static float getEyeHeight(Entity entity, @Nullable Object pose) {
		String name = poseName(pose);
		if ("SLEEPING".equals(name) && entity instanceof EntityLivingBase) {
			return 0.2F;
		}
		if (entity instanceof EntityPlayer player) {
			return switch (name) {
				case "CROUCHING" -> player.getDefaultEyeHeight() - 0.08F;
				case "SWIMMING", "FALL_FLYING", "SPIN_ATTACK" -> 0.4F;
				case "DYING" -> player.getDefaultEyeHeight();
				default -> player.getDefaultEyeHeight();
			};
		}
		return entity.getEyeHeight();
	}

	/** 1.21 {@code entity.dimensions}: its size now (a carried EntityDimensions). */
	@Nullable
	public static Object dimensions(Entity entity) {
		return CarriedBridges.getDimensions(entity, null);
	}

	private static final Own DEFAULT_DIMENSIONS = new Own("getDefaultDimensions", 1);

	/**
	 * 1.21 {@code entity.getDefaultDimensions(pose)}: the entity's own, else (and for its super call) its type's size
	 * (a slime's times its size), else its size now. Twilight Forest's pinch beetles and rising zombies, Iron's Spells'
	 * ice spiders and roots, Create's packages, Aether's swets ask it in their own sizes.
	 */
	@Nullable
	public static Object getDefaultDimensions(Entity entity, @Nullable Object pose) {
		Object own = DEFAULT_DIMENSIONS.call(entity, pose);
		if (own != INHERITED) {
			return own;
		}
		io.github.xponer2.translator.registry.EntityType<?> type = io.github.xponer2.translator.registry.EntityType.of(entity);
		float width = type != null && type.getWidth() > 0.0F ? type.getWidth() : entity.width;
		float height = type != null && type.getHeight() > 0.0F ? type.getHeight() : entity.height;
		if (entity instanceof net.minecraft.entity.monster.EntitySlime slime && type != null) {
			width *= slime.getSlimeSize();
			height *= slime.getSlimeSize();
		}
		try {
			return Class.forName("net.minecraft.world.entity.EntityDimensions", true, net.minecraft.launchwrapper.Launch.classLoader)
					.getMethod("scalable", float.class, float.class).invoke(null, width, height);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			RuntimeStubs.failed("EntityDimensions.scalable", e);
			return null;
		}
	}

	/** 1.21 {@code living.getLocalBoundsForPose(pose)}: its box in that pose around its feet (a sleeper's is 0.2 wide). */
	public static AxisAlignedBB getLocalBoundsForPose(EntityLivingBase living, @Nullable Object pose) {
		boolean sleeping = "SLEEPING".equals(poseName(pose));
		double width = sleeping ? 0.2D : living.width;
		double height = sleeping ? 0.2D : living.height;
		return new AxisAlignedBB(-width / 2.0D, 0.0D, -width / 2.0D, width / 2.0D, height, width / 2.0D);
	}

	private static final Own DISMOUNT_POSES = new Own("getDismountPoses", 0);

	/** 1.21 {@code living.getDismountPoses()}: standing, for a player also crouching and swimming. */
	public static ImmutableList<?> getDismountPoses(EntityLivingBase living) {
		Object own = DISMOUNT_POSES.call(living);
		if (own instanceof ImmutableList<?> poses) {
			return poses;
		}
		String pose = "net.minecraft.world.entity.Pose";
		Object standing = CarriedBridges.constant(pose, "STANDING");
		if (standing == null) {
			return ImmutableList.of();
		}
		return living instanceof EntityPlayer
				? ImmutableList.of(standing, CarriedBridges.constant(pose, "CROUCHING"), CarriedBridges.constant(pose, "SWIMMING"))
				: ImmutableList.of(standing);
	}

	// --- Entity: fluids ------------------------------------------------------------------------------------------------

	/** The fluids the entity's box is in (their NeoForge fluid types) and how high each stands above its feet. */
	private static Map<Object, Double> fluidHeights(Entity entity) {
		Map<Object, Double> heights = new LinkedHashMap<>();
		AxisAlignedBB box = entity.getEntityBoundingBox().shrink(0.001D);
		World world = entity.world;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = MathHelper.floor(box.minX); x < MathHelper.ceil(box.maxX); x++) {
			for (int y = MathHelper.floor(box.minY); y < MathHelper.ceil(box.maxY); y++) {
				for (int z = MathHelper.floor(box.minZ); z < MathHelper.ceil(box.maxZ); z++) {
					pos.setPos(x, y, z);
					IBlockState state = world.getBlockState(pos);
					net.minecraftforge.fluids.Fluid fluid = net.minecraftforge.fluids.FluidRegistry.lookupFluidForBlock(state.getBlock());
					if (fluid == null) {
						continue;
					}
					double surface = y + filled(world, pos, state);
					if (surface < box.minY) {
						continue;
					}
					Object type = CarriedBridges.getFluidType(fluid);
					if (type != null) {
						heights.merge(type, surface - box.minY, Math::max);
					}
				}
			}
		}
		return heights;
	}

	private static double filled(World world, BlockPos pos, IBlockState state) {
		if (state.getBlock() instanceof net.minecraftforge.fluids.IFluidBlock block) {
			return Math.abs(block.getFilledPercentage(world, pos));
		}
		if (state.getBlock() instanceof BlockLiquid && state.getPropertyKeys().contains(BlockLiquid.LEVEL)) {
			return 1.0D - BlockLiquid.getLiquidHeightPercent(state.getValue(BlockLiquid.LEVEL));
		}
		return 1.0D;
	}

	/**
	 * NeoForge {@code entity.isInFluidType(test)} (a test of each fluid type and its height), {@code isInFluidType(type)}
	 * or {@code isInFluidType(fluidState)}: the fluids the entity is in.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static boolean isInFluidType(Entity entity, @Nullable Object test) {
		Map<Object, Double> heights = fluidHeights(entity);
		if (test instanceof java.util.function.BiPredicate predicate) {
			for (Map.Entry<Object, Double> entry : heights.entrySet()) {
				if (predicate.test(entry.getKey(), entry.getValue())) {
					return true;
				}
			}
			return false;
		}
		if (test instanceof io.github.xponer2.translator.util.FluidState state) {
			test = CarriedBridges.getFluidType(state);
		}
		return test == null ? !heights.isEmpty() : heights.containsKey(test);
	}

	/** NeoForge {@code entity.getFluidTypeHeight(type)}: how high that fluid stands above its feet, 0 when not in it. */
	public static double getFluidTypeHeight(Entity entity, @Nullable Object type) {
		Double height = type == null ? null : fluidHeights(entity).get(type);
		return height == null ? 0.0D : height;
	}

	/** NeoForge {@code entity.getMaxHeightFluidType()}: the fluid that stands highest on it, else NeoForge's empty type. */
	@Nullable
	public static Object getMaxHeightFluidType(Entity entity) {
		Object highest = null;
		double most = Double.NEGATIVE_INFINITY;
		for (Map.Entry<Object, Double> entry : fluidHeights(entity).entrySet()) {
			if (entry.getValue() > most) {
				most = entry.getValue();
				highest = entry.getKey();
			}
		}
		return highest != null ? highest : emptyFluidType();
	}

	@Nullable
	private static Object emptyFluidType() {
		try {
			Object holder = CarriedBridges.carried("net.neoforged.neoforge.common.NeoForgeMod").getField("EMPTY_TYPE").get(null);
			return holder == null ? null : accessor(holder, "value");
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	// --- Entity: drops and slots ---------------------------------------------------------------------------------------

	private static final Map<Entity, Collection<EntityItem>> CAPTURING = Collections.synchronizedMap(new WeakHashMap<>());

	/** NeoForge {@code entity.captureDrops()}: where the entity's drops go now (with what was caught), null when they fall. */
	@Nullable
	public static Collection<EntityItem> captureDrops(Entity entity) {
		Collection<EntityItem> into = CAPTURING.get(entity);
		if (into != null) {
			caught(entity, into);
		}
		return into;
	}

	/**
	 * NeoForge {@code entity.captureDrops(collection)}: the entity's drops go there instead of into the world (1.12.2's
	 * captured drops), null lets them fall again; gives the collection before, with what it caught (Create's deployer
	 * takes what it shears or kills).
	 */
	@Nullable
	public static Collection<EntityItem> captureDrops(Entity entity, @Nullable Collection<EntityItem> value) {
		Collection<EntityItem> previous = CAPTURING.remove(entity);
		if (previous != null) {
			caught(entity, previous);
		}
		if (value != null) {
			CAPTURING.put(entity, value);
			entity.captureDrops = true;
		} else if (previous != null) {
			entity.captureDrops = false;
		}
		return previous;
	}

	private static void caught(Entity entity, Collection<EntityItem> into) {
		if (entity.capturedDrops != null && !entity.capturedDrops.isEmpty()) {
			into.addAll(entity.capturedDrops);
			entity.capturedDrops.clear();
		}
	}

	/**
	 * 1.21 {@code entity.getSlot(index)}: a living entity's hands (98, 99), armor (100-103); a player's inventory (0-35),
	 * ender chest (200+), carried stack (499) and crafting grid (500-503); else no slot.
	 */
	public static io.github.xponer2.translator.menu.SlotAccess getSlot(Entity entity, int index) {
		if (entity instanceof EntityPlayer player) {
			if (index == 499) {
				return io.github.xponer2.translator.menu.SlotAccess.of(player.inventory::getItemStack, player.inventory::setItemStack);
			}
			int craft = index - 500;
			if (craft >= 0 && craft < 4 && player.inventoryContainer instanceof net.minecraft.inventory.ContainerPlayer menu) {
				return io.github.xponer2.translator.menu.SlotAccess.forContainer(menu.craftMatrix, craft);
			}
			if (index >= 0 && index < player.inventory.mainInventory.size()) {
				return io.github.xponer2.translator.menu.SlotAccess.forContainer(player.inventory, index);
			}
			int ender = index - 200;
			if (ender >= 0 && ender < player.getInventoryEnderChest().getSizeInventory()) {
				return io.github.xponer2.translator.menu.SlotAccess.forContainer(player.getInventoryEnderChest(), ender);
			}
		}
		if (entity instanceof EntityLivingBase living) {
			EntityEquipmentSlot slot = switch (index) {
				case 98 -> EntityEquipmentSlot.MAINHAND;
				case 99 -> EntityEquipmentSlot.OFFHAND;
				case 100 -> EntityEquipmentSlot.FEET;
				case 101 -> EntityEquipmentSlot.LEGS;
				case 102 -> EntityEquipmentSlot.CHEST;
				case 103 -> EntityEquipmentSlot.HEAD;
				default -> null;
			};
			if (slot == EntityEquipmentSlot.HEAD || slot == EntityEquipmentSlot.MAINHAND || slot == EntityEquipmentSlot.OFFHAND) {
				return io.github.xponer2.translator.menu.SlotAccess.forEquipmentSlot(living, slot);
			}
			if (slot != null) {
				EntityEquipmentSlot armor = slot;
				return io.github.xponer2.translator.menu.SlotAccess.forEquipmentSlot(living, slot,
						stack -> stack.isEmpty() || EntityLiving.getSlotForItemStack(stack) == armor);
			}
		}
		return io.github.xponer2.translator.menu.SlotAccess.NULL;
	}

	// --- LivingEntity: sleeping ----------------------------------------------------------------------------------------

	/** Where a living entity that isn't a player sleeps (1.12.2 only lets players sleep). */
	private static final Map<EntityLivingBase, BlockPos> SLEEPING = Collections.synchronizedMap(new WeakHashMap<>());

	/** 1.21 {@code living.getSleepingPos()}: the bed it sleeps in. */
	public static Optional<BlockPos> getSleepingPos(EntityLivingBase living) {
		if (living instanceof EntityPlayer player) {
			return player.isPlayerSleeping() && player.bedLocation != null ? Optional.of(player.bedLocation) : Optional.empty();
		}
		return Optional.ofNullable(SLEEPING.get(living));
	}

	/** 1.21 {@code living.setSleepingPos(pos)}: asleep there. */
	public static void setSleepingPos(EntityLivingBase living, BlockPos pos) {
		if (living instanceof EntityPlayer player) {
			player.bedLocation = pos;
			set(SLEEPING_FLAG, player, true);
		} else {
			SLEEPING.put(living, pos);
		}
	}

	/** 1.21 {@code living.clearSleepingPos()}: no longer asleep. */
	public static void clearSleepingPos(EntityLivingBase living) {
		if (living instanceof EntityPlayer player) {
			set(SLEEPING_FLAG, player, false);
		} else {
			SLEEPING.remove(living);
		}
	}

	/**
	 * 1.21 {@code living.startSleeping(pos)}: lies down in the bed there with no checks (a mod's own sleeping place,
	 * Comforts' sleeping bags and hammocks, checked them): off its mount, the bed occupied, small, laid along the bed;
	 * a server player's clients see it in bed.
	 */
	public static void startSleeping(EntityLivingBase living, BlockPos pos) {
		if (living.isRiding()) {
			living.dismountRidingEntity();
		}
		World world = living.world;
		IBlockState state = world.isBlockLoaded(pos) ? world.getBlockState(pos) : null;
		boolean bed = state != null && state.getBlock().isBed(state, world, pos, living);
		living.motionX = 0.0D;
		living.motionY = 0.0D;
		living.motionZ = 0.0D;
		if (!(living instanceof EntityPlayer player)) {
			living.setPosition(pos.getX() + 0.5D, pos.getY() + 0.6875D, pos.getZ() + 0.5D);
			SLEEPING.put(living, pos);
			return;
		}
		if (bed) {
			state.getBlock().setBedOccupied(world, pos, player, true);
		}
		invoke(SPAWN_SHOULDER_ENTITIES, player);
		invoke(SET_SIZE, player, 0.2F, 0.2F);
		if (bed) {
			EnumFacing facing = state.getBlock().getBedDirection(state, world, pos);
			player.renderOffsetX = -1.8F * facing.getXOffset();
			player.renderOffsetZ = -1.8F * facing.getZOffset();
			player.setPosition(pos.getX() + 0.5F + facing.getXOffset() * 0.4F, pos.getY() + 0.6875F, pos.getZ() + 0.5F + facing.getZOffset() * 0.4F);
		} else {
			player.setPosition(pos.getX() + 0.5F, pos.getY() + 0.6875F, pos.getZ() + 0.5F);
		}
		set(SLEEPING_FLAG, player, true);
		set(SLEEP_TIMER, player, 0);
		player.bedLocation = pos;
		if (!world.isRemote) {
			world.updateAllPlayersSleepingFlag();
		}
		if (player instanceof EntityPlayerMP server && server.connection != null) {
			var packet = new net.minecraft.network.play.server.SPacketUseBed(server, pos);
			server.getServerWorld().getEntityTracker().sendToTracking(server, packet);
			server.connection.setPlayerLocation(server.posX, server.posY, server.posZ, server.rotationYaw, server.rotationPitch);
			server.connection.sendPacket(packet);
		}
	}

	/** 1.21 {@code living.getBedOrientation()}: the way the bed it sleeps in faces, up when it sleeps in none. */
	public static EnumFacing getBedOrientation(EntityLivingBase living) {
		BlockPos pos = getSleepingPos(living).orElse(null);
		if (pos == null) {
			return EnumFacing.UP;
		}
		IBlockState state = living.world.getBlockState(pos);
		return state.getBlock().isBed(state, living.world, pos, living) ? state.getBlock().getBedDirection(state, living.world, pos) : EnumFacing.UP;
	}

	// --- LivingEntity: effects, attributes, combat ---------------------------------------------------------------------

	/** 1.21 {@code living.removeAllEffects()}: every effect ends (what Forge's event lets go); whether it had any. */
	public static boolean removeAllEffects(EntityLivingBase living) {
		if (living.world.isRemote) {
			return false;
		}
		boolean had = !living.getActivePotionEffects().isEmpty();
		living.clearActivePotions();
		return had;
	}

	/** 1.21 {@code living.removeEffectNoUpdate(effect)}: taken off without ending it (no attribute or client update). */
	@Nullable
	public static PotionEffect removeEffectNoUpdate(EntityLivingBase living, @Nullable io.github.xponer2.translator.registry.Holder<?> effect) {
		return effect != null && effect.value() instanceof net.minecraft.potion.Potion potion ? living.removeActivePotionEffect(potion) : null;
	}

	/** 1.21 {@code living.sendEffectToPassengers(effect)}: its players riding it see the effect. */
	public static void sendEffectToPassengers(EntityLivingBase living, PotionEffect effect) {
		for (Entity passenger : living.getPassengers()) {
			if (passenger instanceof EntityPlayerMP player && player.connection != null) {
				player.connection.sendPacket(new net.minecraft.network.play.server.SPacketEntityEffect(living.getEntityId(), effect));
			}
		}
	}

	/** 1.21 {@code living.refreshDirtyAttributes()}: 1.12.2 applies attributes at once; health over a lowered maximum drops. */
	public static void refreshDirtyAttributes(EntityLivingBase living) {
		if (living.getHealth() > living.getMaxHealth()) {
			living.setHealth(living.getMaxHealth());
		}
	}

	/** 1.21 {@code living.getJumpBoostPower()}: a tenth of a block per level of jump boost. */
	public static float getJumpBoostPower(EntityLivingBase living) {
		PotionEffect jump = living.getActivePotionEffect(net.minecraft.init.MobEffects.JUMP_BOOST);
		return jump == null ? 0.0F : 0.1F * (jump.getAmplifier() + 1.0F);
	}

	/** 1.21 {@code living.getSwimAmount(partialTick)}: 1.12.2 entities never swim (no swimming pose). */
	public static float getSwimAmount(EntityLivingBase living, float partialTick) {
		return 0.0F;
	}

	/** 1.21 {@code living.getArmorAndBodyArmorSlots()}: its armor (a 1.12.2 horse's armor is in its own inventory). */
	public static Iterable<ItemStack> getArmorAndBodyArmorSlots(EntityLivingBase living) {
		return living.getArmorInventoryList();
	}

	/** 1.21 {@code living.setNoActionTime(ticks)}: how long it has done nothing (mobs despawn and stop looking by it). */
	public static void setNoActionTime(EntityLivingBase living, int ticks) {
		set(IDLE_TIME, living, ticks);
	}

	/** 1.21 {@code living.lerpHeadTo(yaw, steps)}: its head turns there (1.12.2 sets the head at once). */
	public static void lerpHeadTo(EntityLivingBase living, float yaw, int steps) {
		living.setRotationYawHead(yaw);
	}

	/** 1.21 {@code living.setLastHurtByPlayer(player)}: hurt by that player now (null forgets it), for drops and XP. */
	public static void setLastHurtByPlayer(EntityLivingBase living, @Nullable EntityPlayer player) {
		set(ATTACKING_PLAYER, living, player);
		set(RECENTLY_HIT, living, player == null ? 0 : 100);
	}

	/** 1.21 {@code living.onEquippedItemBroken(item, slot)}: the break of the item is heard (and seen on clients). */
	public static void onEquippedItemBroken(EntityLivingBase living, net.minecraft.item.Item item, EntityEquipmentSlot slot) {
		living.renderBrokenItemStack(new ItemStack(item));
	}

	/** 1.21 {@code living.spawnItemParticles(stack, count)}: crumbs of the item in front of its mouth, as when eating. */
	public static void spawnItemParticles(EntityLivingBase living, ItemStack stack, int count) {
		java.util.Random random = living.world.rand;
		float pitch = -living.rotationPitch * 0.017453292F;
		float yaw = -living.rotationYaw * 0.017453292F;
		int[] item = stack.getHasSubtypes() ? new int[] {net.minecraft.item.Item.getIdFromItem(stack.getItem()), stack.getMetadata()}
				: new int[] {net.minecraft.item.Item.getIdFromItem(stack.getItem())};
		for (int i = 0; i < count; i++) {
			Vec3d speed = new Vec3d((random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D).rotatePitch(pitch).rotateYaw(yaw);
			double depth = -random.nextFloat() * 0.6D - 0.3D;
			Vec3d at = new Vec3d((random.nextFloat() - 0.5D) * 0.3D, depth, 0.6D).rotatePitch(pitch).rotateYaw(yaw)
					.add(living.posX, living.posY + living.getEyeHeight(), living.posZ);
			living.world.spawnParticle(net.minecraft.util.EnumParticleTypes.ITEM_CRACK, at.x, at.y, at.z, speed.x, speed.y + 0.05D, speed.z, item);
		}
	}

	private static final Own CAN_ATTACK = new Own("canAttack", 1);

	/** 1.21 {@code living.canAttack(target)}: not a player in peaceful, and the target can be seen as an enemy. */
	public static boolean canAttack(EntityLivingBase living, EntityLivingBase target) {
		Object own = CAN_ATTACK.call(living, target);
		if (own instanceof Boolean can) {
			return can;
		}
		if (target instanceof EntityPlayer && living.world.getDifficulty() == net.minecraft.world.EnumDifficulty.PEACEFUL) {
			return false;
		}
		return EntityGapBridges.canBeSeenAsEnemy(target);
	}

	private static final Own STAND_ON_FLUID = new Own("canStandOnFluid", 1);

	/** 1.21 {@code living.canStandOnFluid(state)}: the entity's own answer (1.21's default: no). */
	public static boolean canStandOnFluid(EntityLivingBase living, @Nullable io.github.xponer2.translator.util.FluidState state) {
		Object own = STAND_ON_FLUID.call(living, state);
		return own instanceof Boolean can && can;
	}

	private static final Own UPDATE_WALK_ANIMATION = new Own("updateWalkAnimation", 1);

	/** 1.21 {@code living.calculateEntityAnimation(includeHeight)}: the walk animation follows how far it moved this tick. */
	public static void calculateEntityAnimation(EntityLivingBase living, boolean includeHeight) {
		double dx = living.posX - living.prevPosX;
		double dy = includeHeight ? living.posY - living.prevPosY : 0.0D;
		double dz = living.posZ - living.prevPosZ;
		float moved = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (UPDATE_WALK_ANIMATION.call(living, moved) == INHERITED) {
			io.github.xponer2.translator.entity.WalkAnimationState.of(living).update(Math.min(moved * 4.0F, 1.0F), 0.4F);
		}
	}

	/** 1.21 {@code living.walkAnimation = state} (Iron's Spells' frozen humanoids keep their own still pose). */
	public static void walkAnimation(EntityLivingBase living, @Nullable io.github.xponer2.translator.entity.WalkAnimationState state) {
		io.github.xponer2.translator.entity.WalkAnimationState.set(living, state);
	}

	/**
	 * 1.21 {@code mob.jumpInFluid(type)}: a mob whose path can't float pushes up hard, any other swims up by its swim
	 * speed.
	 */
	public static void jumpInFluid(EntityLivingBase living, @Nullable Object type) {
		if (living instanceof EntityLiving mob && !(mob.getNavigator() instanceof net.minecraft.pathfinding.PathNavigateGround ground && ground.getCanSwim())) {
			mob.motionY += 0.3D;
			return;
		}
		net.minecraft.entity.ai.attributes.IAttributeInstance swim = living.getEntityAttribute(EntityLivingBase.SWIM_SPEED);
		living.motionY += 0.04F * (swim == null ? 1.0D : swim.getAttributeValue());
	}

	// --- LivingEntity / Mob: loot, experience, equipment ---------------------------------------------------------------

	private static final Own BASE_EXPERIENCE = new Own("getBaseExperienceReward", 0);

	/** 1.21 {@code living.getExperienceReward(level, killer)}: the experience it drops (its own count, else 1.12.2's). */
	public static int getExperienceReward(EntityLivingBase living, @Nullable WorldServer level, @Nullable Entity killer) {
		Object own = BASE_EXPERIENCE.call(living);
		if (own instanceof Integer points) {
			return points;
		}
		return invoke(EXPERIENCE_POINTS, living, killer instanceof EntityPlayer player ? player : null) instanceof Integer points ? points : 0;
	}

	private static final Own DEFAULT_LOOT_TABLE = new Own("getDefaultLootTable", 0);

	/** 1.21 {@code living.getLootTable()}: the table set on the mob, else its own, else its type's (entities/<name>). */
	@SuppressWarnings("unchecked")
	public static ResourceKey<LootTable> getLootTable(EntityLivingBase living) {
		if (living instanceof EntityLiving mob && get(DEATH_LOOT_TABLE, mob) instanceof ResourceLocation set) {
			return ResourceKey.create(LootBridges.LOOT_TABLE, set);
		}
		Object own = DEFAULT_LOOT_TABLE.call(living);
		if (own instanceof ResourceKey<?> key) {
			return (ResourceKey<LootTable>) key;
		}
		if (living instanceof EntityLiving mob && invoke(LOOT_TABLE, mob) instanceof ResourceLocation table) {
			return ResourceKey.create(LootBridges.LOOT_TABLE, table);
		}
		return io.github.xponer2.translator.registry.EntityType.of(living).getDefaultLootTable();
	}

	/** 1.21 {@code mob.getLootTableSeed()}: the seed set with its loot table (0 when none). */
	public static long getLootTableSeed(EntityLivingBase living) {
		return living instanceof EntityLiving mob && get(DEATH_LOOT_TABLE_SEED, mob) instanceof Long seed ? seed : 0L;
	}

	/** 1.21 {@code mob.canReplaceEqualItem(candidate, existing)}: the less damaged, else the one with data. */
	public static boolean canReplaceEqualItem(EntityLiving mob, ItemStack candidate, ItemStack existing) {
		if (candidate.getItemDamage() < existing.getItemDamage()) {
			return true;
		}
		return candidate.hasTagCompound() && !existing.hasTagCompound();
	}

	private static final Own EQUIPMENT_DROP_CHANCE = new Own("getEquipmentDropChance", 1);

	/** 1.21 {@code mob.getEquipmentDropChance(slot)}: the chance the item in that slot drops. */
	public static float getEquipmentDropChance(EntityLiving mob, EntityEquipmentSlot slot) {
		Object own = EQUIPMENT_DROP_CHANCE.call(mob, slot);
		if (own instanceof Float chance) {
			return chance;
		}
		Object chances = get(slot.getSlotType() == EntityEquipmentSlot.Type.HAND ? HAND_DROP_CHANCES : ARMOR_DROP_CHANCES, mob);
		return chances instanceof float[] values && slot.getIndex() < values.length ? values[slot.getIndex()] : 0.085F;
	}

	/** 1.21 {@code mob.equip(equipmentTable)}: the table's loot put on the mob, with its drop chances. */
	public static void equip(EntityLiving mob, @Nullable Object table) {
		if (table == null) {
			return;
		}
		if (accessor(table, "lootTable") instanceof ResourceKey<?> key) {
			equip(mob, key, accessor(table, "slotDropChances") instanceof Map<?, ?> chances ? chances : Collections.emptyMap());
		}
	}

	/**
	 * 1.21 {@code mob.equip(lootTable, dropChances)}: each item the table gives goes to its slot (armor where it is worn,
	 * anything else in the main hand; one item a slot), with that slot's drop chance (Twilight Forest's spawners).
	 */
	public static void equip(EntityLiving mob, @Nullable ResourceKey<?> lootTable, @Nullable Map<?, ?> dropChances) {
		if (lootTable == null || !(mob.world instanceof WorldServer world) || "minecraft:empty".equals(String.valueOf(lootTable.location()))) {
			return;
		}
		LootTable table = new io.github.xponer2.translator.world.ReloadableRegistries(world.getMinecraftServer()).getLootTable(lootTable);
		if (table == null || table == LootTable.EMPTY_LOOT_TABLE) {
			return;
		}
		var context = new net.minecraft.world.storage.loot.LootContext.Builder(world).withLootedEntity(mob).build();
		List<EntityEquipmentSlot> used = new ArrayList<>();
		for (ItemStack stack : table.generateLootForPools(world.rand, context)) {
			if (stack.isEmpty()) {
				continue;
			}
			EntityEquipmentSlot slot = EntityLiving.getSlotForItemStack(stack);
			if (used.contains(slot)) {
				continue;
			}
			mob.setItemStackToSlot(slot, slot.getSlotType() == EntityEquipmentSlot.Type.ARMOR ? stack.splitStack(1) : stack);
			Object chance = dropChances == null ? null : dropChances.get(slot);
			if (chance instanceof Float value) {
				mob.setDropChance(slot, value);
			}
			used.add(slot);
		}
	}

	private static final Own SPAWN_OBSTRUCTION = new Own("checkSpawnObstruction", 1);

	/** 1.21 {@code mob.checkSpawnObstruction(level)}: no liquid in its box and no entity in the way. */
	public static boolean checkSpawnObstruction(EntityLiving mob, @Nullable World level) {
		Object own = SPAWN_OBSTRUCTION.call(mob, level);
		if (own instanceof Boolean free) {
			return free;
		}
		World world = level == null ? mob.world : level;
		AxisAlignedBB box = mob.getEntityBoundingBox();
		return !world.containsAnyLiquid(box) && world.checkNoEntityCollision(box, mob);
	}

	private static final double ATTACK_REACH = Math.sqrt(2.04F) - 0.6F;
	private static final Own WITHIN_MELEE_RANGE = new Own("isWithinMeleeAttackRange", 1);

	/** 1.21 {@code mob.isWithinMeleeAttackRange(target)}: its box (and its mount's), a little wider, touches the target's. */
	public static boolean isWithinMeleeAttackRange(EntityLiving mob, EntityLivingBase target) {
		Object own = WITHIN_MELEE_RANGE.call(mob, target);
		if (own instanceof Boolean within) {
			return within;
		}
		AxisAlignedBB box = mob.getEntityBoundingBox();
		Entity vehicle = mob.getRidingEntity();
		if (vehicle != null) {
			AxisAlignedBB mount = vehicle.getEntityBoundingBox();
			box = new AxisAlignedBB(Math.min(box.minX, mount.minX), box.minY, Math.min(box.minZ, mount.minZ), Math.max(box.maxX, mount.maxX), box.maxY,
					Math.max(box.maxZ, mount.maxZ));
		}
		return box.grow(ATTACK_REACH, 0.0D, ATTACK_REACH).intersects(EntityGapBridges.getHitbox(target));
	}

	private static final Own MAY_BE_LEASHED = new Own("mayBeLeashed", 0);
	private static final Own CAN_BE_LEASHED = new Own("canBeLeashed", 0);

	/** 1.21 {@code mob.mayBeLeashed()}: it can be leashed (no monster can) and isn't now. */
	public static boolean mayBeLeashed(EntityLiving mob) {
		Object own = MAY_BE_LEASHED.call(mob);
		if (own instanceof Boolean may) {
			return may;
		}
		Object can = CAN_BE_LEASHED.call(mob);
		boolean leashable = can instanceof Boolean value ? value : !(mob instanceof net.minecraft.entity.monster.IMob);
		return leashable && !mob.getLeashed();
	}

	// --- LivingEntity: brain -------------------------------------------------------------------------------------------

	private static final Own BRAIN = new Own("getBrain", 0);
	private static final Map<EntityLivingBase, Object> BRAINS = Collections.synchronizedMap(new WeakHashMap<>());

	/**
	 * 1.21 {@code living.getBrain()}: a mod's mob's own brain; a 1.12.2 mob (its AI is goals) gets an empty one, which
	 * holds no memories (Iron's Spells and Supplementaries erase and look up memories of the mobs around).
	 */
	@Nullable
	public static Object getBrain(EntityLivingBase living) {
		Object own = BRAIN.call(living);
		if (own != INHERITED && own != null) {
			return own;
		}
		Object brain = BRAINS.get(living);
		if (brain == null) {
			brain = emptyBrain();
			if (brain != null) {
				BRAINS.put(living, brain);
			}
		}
		return brain;
	}

	@Nullable
	private static Object emptyBrain() {
		try {
			for (var constructor : CarriedBridges.carried("net.minecraft.world.entity.ai.Brain").getConstructors()) {
				if (constructor.getParameterCount() == 4) {
					java.util.function.Supplier<Object> codec = () -> null;
					return constructor.newInstance(Collections.emptyList(), Collections.emptyList(), ImmutableList.of(), codec);
				}
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			RuntimeStubs.failed("an empty brain", e);
		}
		return null;
	}

	// --- Player / ServerPlayer -----------------------------------------------------------------------------------------

	/** 1.21 {@code player.fishing}: its fishing hook out, else null. */
	@Nullable
	public static net.minecraft.entity.projectile.EntityFishHook fishing(EntityPlayer player) {
		return player.fishEntity;
	}

	public static void fishing(EntityPlayer player, @Nullable net.minecraft.entity.projectile.EntityFishHook hook) {
		player.fishEntity = hook;
	}

	/** NeoForge {@code player.mayFly()}: it is allowed to fly. */
	public static boolean mayFly(EntityPlayer player) {
		return player.capabilities.allowFlying;
	}

	private static final Own FAKE_PLAYER = new Own("isFakePlayer", 0);

	/** NeoForge {@code player.isFakePlayer()}: a mod's fake player (NeoForge's or Forge's). */
	public static boolean isFakePlayer(EntityPlayer player) {
		Object own = FAKE_PLAYER.call(player);
		return own instanceof Boolean fake ? fake : player instanceof net.minecraftforge.common.util.FakePlayer;
	}

	/** 1.21 {@code player.isTextFilteringEnabled()}: 1.12.2 filters no chat or sign text. */
	public static boolean isTextFilteringEnabled(EntityPlayer player) {
		return false;
	}

	/** 1.21 {@code player.startFallFlying()}: gliding with the elytra flag. */
	public static void startFallFlying(EntityPlayer player) {
		invoke(SET_FLAG, player, 7, true);
	}

	/** 1.21 {@code entity.playStepSound(pos, state)}: the step sound of that block (1.12.2's, by the block). */
	public static void playStepSound(Entity entity, BlockPos pos, IBlockState state) {
		invoke(PLAY_STEP_SOUND, entity, pos, state.getBlock());
	}

	/** 1.21 {@code player.getLastDeathLocation()}: 1.12.2 doesn't keep where a player died. */
	public static Optional<?> getLastDeathLocation(EntityPlayer player) {
		return Optional.empty();
	}

	/**
	 * 1.21 {@code player.triggerRecipeCrafted(recipe, items)}: a player has no advancement criterion for it here (1.21's
	 * Player does nothing, its ServerPlayer's "recipe_crafted" criterion isn't in 1.12.2).
	 */
	public static void triggerRecipeCrafted(EntityPlayer player, @Nullable Object recipe, @Nullable List<?> items) {
	}

	/**
	 * 1.21 {@code living.onItemPickup(itemEntity)}: the "thrown item picked up" criteria of 1.21 aren't in 1.12.2 (the
	 * pickup itself is done by the caller).
	 */
	public static void onItemPickup(EntityLivingBase living, EntityItem item) {
	}

	/** 1.21 {@code serverPlayer.setGameMode(mode)}: switched (and told), false when it already plays in it. */
	public static boolean setGameMode(EntityPlayerMP player, net.minecraft.world.GameType mode) {
		if (mode == null || player.interactionManager.getGameType() == mode) {
			return false;
		}
		player.setGameType(mode);
		return true;
	}

	/** 1.21 {@code serverPlayer.requestedViewDistance()}: 1.12.2 clients don't send theirs, the server's. */
	public static int requestedViewDistance(EntityPlayerMP player) {
		net.minecraft.server.MinecraftServer server = player.getServer();
		return server == null ? 10 : server.getPlayerList().getViewDistance();
	}

	/** 1.21 {@code serverPlayer.getRespawnDimension()}: the dimension of its spawn point. */
	public static ResourceKey<World> getRespawnDimension(EntityPlayerMP player) {
		return ApiBridges.dimensionKey(player.getSpawnDimension());
	}

	/** 1.21 {@code serverPlayer.getChunkTrackingView()}: the chunks around it within the view distance. */
	@Nullable
	public static Object getChunkTrackingView(EntityPlayerMP player) {
		try {
			return CarriedBridges.carried("net.minecraft.server.level.ChunkTrackingView")
					.getMethod("of", net.minecraft.util.math.ChunkPos.class, int.class)
					.invoke(null, new net.minecraft.util.math.ChunkPos(player.chunkCoordX, player.chunkCoordZ), requestedViewDistance(player));
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			RuntimeStubs.failed("ChunkTrackingView.of", e);
			return null;
		}
	}

	private static final Map<EntityPlayerMP, Object> RECIPE_BOOKS = Collections.synchronizedMap(new WeakHashMap<>());

	/**
	 * 1.21 {@code serverPlayer.getRecipeBook()}: the carried ServerRecipeBook (a 1.12.2 RecipeBook) with what the
	 * player's own book has unlocked, read when asked (Mekanism's and Twilight Forest's crafting windows check it).
	 */
	@Nullable
	public static Object getRecipeBook(EntityPlayerMP player) {
		Object book = RECIPE_BOOKS.get(player);
		if (book == null) {
			try {
				book = CarriedBridges.carried("net.minecraft.stats.ServerRecipeBook").getConstructor().newInstance();
				RECIPE_BOOKS.put(player, book);
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				RuntimeStubs.failed("ServerRecipeBook", e);
				return null;
			}
		}
		if (book instanceof net.minecraft.stats.RecipeBook copy) {
			copy.copyFrom(player.getRecipeBook());
		}
		return book;
	}
}
