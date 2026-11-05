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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * Calls whose 1.21 types come with the carried 1.21.1 API (the Translator has no compile-time class for them): the
 * helpers take and give them as Object (the converter casts the result back) and reach them by reflection.
 */
public final class CarriedBridges {
	private static final Map<Fluid, Object> FLUID_TYPES = new ConcurrentHashMap<>();

	private CarriedBridges() {
	}

	// --- Fluid / FluidStack: NeoForge FluidType ---------------------------------------------------------------------

	/**
	 * 1.21 {@code fluid.getFluidType()}: a 1.21.1 mod's fluid gives its own type (BaseFlowingFluid has it); water, lava
	 * and Forge fluids get a FluidType made once with their Forge properties (name, density, temperature, viscosity,
	 * light).
	 */
	@Nullable
	public static Object getFluidType(Fluid fluid) {
		if (fluid == null) {
			return null;
		}
		Method own = ownMethod(fluid.getClass(), "getFluidType");
		if (own != null) {
			try {
				return own.invoke(fluid);
			} catch (ReflectiveOperationException e) {
				io.github.xponer2.translator.runtime.RuntimeStubs.failed("the fluid type of " + fluid.getName(), e);
			}
		}
		return FLUID_TYPES.computeIfAbsent(fluid, CarriedBridges::fluidType);
	}

	@Nullable
	public static Object getFluidType(FluidStack stack) {
		return stack == null || stack.getFluid() == null ? null : getFluidType(stack.getFluid());
	}

	@Nullable
	private static Object fluidType(Fluid fluid) {
		try {
			Class<?> properties = carried("net.neoforged.neoforge.fluids.FluidType$Properties");
			Object p = properties.getMethod("create").invoke(null);
			p = properties.getMethod("descriptionId", String.class).invoke(p, fluid.getUnlocalizedName());
			p = properties.getMethod("density", int.class).invoke(p, fluid.getDensity());
			p = properties.getMethod("temperature", int.class).invoke(p, fluid.getTemperature());
			p = properties.getMethod("viscosity", int.class).invoke(p, fluid.getViscosity());
			p = properties.getMethod("lightLevel", int.class).invoke(p, fluid.getLuminosity());
			return carried("net.neoforged.neoforge.fluids.FluidType").getConstructor(properties).newInstance(p);
		} catch (ReflectiveOperationException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("making a fluid type", e);
			return null;
		}
	}

	/** 1.21 {@code fluidState.getFluidType()}: the type of the state's fluid. */
	@Nullable
	public static Object getFluidType(io.github.xponer2.translator.util.FluidState state) {
		return state == null || state.isEmpty() ? null : getFluidType(state.getType());
	}

	// --- Holder: Holder.Kind --------------------------------------------------------------------------------------------

	/** 1.21 {@code holder.kind()}: a registered object's holder is a reference, any other is direct. */
	@Nullable
	public static Object kind(io.github.xponer2.translator.registry.Holder<?> holder) {
		return constant("net.minecraft.core.Holder$Kind", holder.unwrapKey().isPresent() ? "REFERENCE" : "DIRECT");
	}

	/** 1.21 {@code holder.canSerializeIn(owner)}: one registry set on 1.12.2, every holder fits. */
	public static boolean canSerializeIn(io.github.xponer2.translator.registry.Holder<?> holder, Object owner) {
		return true;
	}

	/** 1.21 {@code holder.getData(dataMap)}: NeoForge data maps aren't loaded on 1.12.2, no value. */
	@Nullable
	public static Object getData(io.github.xponer2.translator.registry.Holder<?> holder, Object dataMap) {
		return null;
	}

	// --- Entity: Pose ---------------------------------------------------------------------------------------------------

	/** 1.21 {@code entity.getPose()}: what 1.12.2 knows of it (sleeping, gliding, sneaking, dying, else standing). */
	@Nullable
	public static Object getPose(Entity entity) {
		String pose = "STANDING";
		if (entity instanceof net.minecraft.entity.EntityLivingBase living && living.getHealth() <= 0) {
			pose = "DYING";
		} else if (entity instanceof net.minecraft.entity.player.EntityPlayer player && player.isPlayerSleeping()) {
			pose = "SLEEPING";
		} else if (entity instanceof net.minecraft.entity.EntityLivingBase living && living.isElytraFlying()) {
			pose = "FALL_FLYING";
		} else if (entity.isSneaking()) {
			pose = "CROUCHING";
		}
		return constant("net.minecraft.world.entity.Pose", pose);
	}

	/** 1.21 {@code entity.setPose(pose)}: 1.12.2 entities have no poses (sneaking is its own flag). */
	public static void setPose(Entity entity, Object pose) {
		if (pose instanceof Enum<?> value && "CROUCHING".equals(value.name())) {
			entity.setSneaking(true);
		} else if (pose instanceof Enum<?> value && "STANDING".equals(value.name())) {
			entity.setSneaking(false);
		}
	}

	/** 1.21 {@code entity.getDimensions(pose)}: the entity's width and height (1.12.2 has one size per entity). */
	@Nullable
	public static Object getDimensions(Entity entity, Object pose) {
		try {
			return carried("net.minecraft.world.entity.EntityDimensions").getMethod("scalable", float.class, float.class).invoke(null, entity.width,
					entity.height);
		} catch (ReflectiveOperationException | LinkageError e) {
			return null;
		}
	}

	/** 1.21 {@code level.getNearestEntity(type, conditions, from, x, y, z, box)}: the nearest one in the box the conditions allow. */
	@Nullable
	public static <T extends net.minecraft.entity.EntityLivingBase> T getNearestEntity(net.minecraft.world.World world, Class<? extends T> type,
			Object conditions, @Nullable net.minecraft.entity.EntityLivingBase from, double x, double y, double z,
			net.minecraft.util.math.AxisAlignedBB box) {
		Method test = null;
		for (Method method : conditions == null ? new Method[0] : conditions.getClass().getMethods()) {
			if ("test".equals(method.getName()) && method.getParameterCount() == 2) {
				test = method;
			}
		}
		T nearest = null;
		double best = Double.MAX_VALUE;
		for (T entity : world.getEntitiesWithinAABB(type, box)) {
			if (entity == from) {
				continue;
			}
			if (test != null) {
				try {
					if (!(Boolean) test.invoke(conditions, from, entity)) {
						continue;
					}
				} catch (ReflectiveOperationException | RuntimeException e) {
					test = null;
				}
			}
			double distance = entity.getDistanceSq(x, y, z);
			if (distance < best) {
				best = distance;
				nearest = entity;
			}
		}
		return nearest;
	}

	/**
	 * A {@code map.put} of a carried class whose map takes each key once (BlockFamilies): the first value stays, and
	 * "nothing was there" is answered, so its check for a second one passes (1.21 blocks that are one 1.12.2 block).
	 */
	@Nullable
	public static Object putFirst(java.util.Map<Object, Object> map, Object key, Object value) {
		map.putIfAbsent(key, value);
		return null;
	}

	/** 1.21 {@code level.getFreeMapId()}: the next 1.12.2 map number as the carried MapId record. */
	@Nullable
	public static Object getFreeMapId(net.minecraft.world.World world) {
		try {
			return carried("net.minecraft.world.level.saveddata.maps.MapId").getConstructor(int.class).newInstance(world.getUniqueDataId("map"));
		} catch (ReflectiveOperationException | LinkageError e) {
			return null;
		}
	}

	/** 1.21 {@code mapData.toggleBanner(level, pos)}: 1.12.2 maps have no banner markers. */
	public static boolean toggleBanner(net.minecraft.world.storage.MapData data, Object level, net.minecraft.util.math.BlockPos pos) {
		return false;
	}

	// --- helpers --------------------------------------------------------------------------------------------------------

	/** A public method the object's own class (not Object) declares or inherits, else null. */
	@Nullable
	private static Method ownMethod(Class<?> type, String name) {
		try {
			Method method = type.getMethod(name);
			return method.getDeclaringClass() == Object.class || Modifier.isStatic(method.getModifiers()) ? null : method;
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	/**
	 * NeoForge's {@code CommonHooks.overridesEqualsAndHashCode(type)}: whether a data component class has its own
	 * equals and hashCode. CommonHooks marks BlockState, FluidState and ResourceKey valid while it loads (1.21 compares
	 * them by identity); here BlockState is the interface IBlockState, which Class.getMethod finds no equals on, and the
	 * Translator's ResourceKey has its own: CommonHooks failed to load (BOP's spanish moss, every hook of it). An
	 * interface or the ResourceKey has none of its own, as in 1.21.
	 */
	public static boolean overridesEqualsAndHashCode(Class<?> type) {
		if (type.isInterface() || type == io.github.xponer2.translator.registry.ResourceKey.class) {
			return false;
		}
		try {
			return type.getMethod("equals", Object.class).getDeclaringClass() != Object.class && type.getMethod("hashCode").getDeclaringClass() != Object.class;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	static Class<?> carried(String name) throws ClassNotFoundException {
		return Class.forName(name, true, net.minecraft.launchwrapper.Launch.classLoader);
	}

	/** A constant of a carried 1.21 enum, null when the class isn't there. */
	@Nullable
	@SuppressWarnings({"unchecked", "rawtypes"})
	static Object constant(String enumClass, String name) {
		try {
			return Enum.valueOf((Class) carried(enumClass), name);
		} catch (ClassNotFoundException | IllegalArgumentException e) {
			return null;
		}
	}

	private static final Map<Object, java.util.Set<Object>> BUILT_KEYS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

	/** {@code builder.put(key, value)} the first time a key comes (see CarriedPatches: 1.21 blocks that are one here). */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static com.google.common.collect.ImmutableMap.Builder putOnce(com.google.common.collect.ImmutableMap.Builder builder, Object key, Object value) {
		if (BUILT_KEYS.computeIfAbsent(builder, b -> new java.util.HashSet<>()).add(key)) {
			builder.put(key, value);
		}
		return builder;
	}

	/** The object when it is of that class, else null (a 1.21 block 1.12.2 hasn't, cast to its 1.21 class). */
	public static Object castOrNull(Object value, Class<?> type) {
		return type.isInstance(value) ? value : null;
	}
}
