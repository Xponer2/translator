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
package io.github.xponer2.translator.runtime;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/**
 * The 1.12.2 fluid handler calls on a 1.21.1 mod's fluid handler (see InterfaceBridges): its 1.21 methods (getTanks,
 * getFluidInTank, getTankCapacity, isFluidValid, fill and drain with a FluidAction) found by name.
 */
public final class FluidHandlers {
	private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();

	private FluidHandlers() {
	}

	@Nullable
	private static Method find(Class<?> type, String name, int parameters, @Nullable Class<?> first, boolean action) {
		return METHODS.computeIfAbsent(type.getName() + "." + name + parameters + (first == null ? "" : first.getName()) + action, k -> {
			for (Class<?> c = type; c != null; c = c.getSuperclass()) {
				for (Method method : c.getDeclaredMethods()) {
					if (method.getName().equals(name) && method.getParameterCount() == parameters && !method.isSynthetic()
							&& (first == null || method.getParameterTypes()[0] == first)
							&& (!action || method.getParameterTypes()[1].isEnum())) {
						method.setAccessible(true);
						return method;
					}
				}
			}
			return null;
		});
	}

	private static Object call(Object handler, String name, @Nullable Class<?> first, Object fallback, Object... args) {
		boolean action = args.length == 2 && args[1] instanceof Boolean;
		Method method = find(handler.getClass(), name, args.length, first, action);
		if (method == null) {
			return fallback;
		}
		try {
			Object[] actual = args.clone();
			if (action) {
				actual[1] = action(method.getParameterTypes()[1], (Boolean) args[1]);
			}
			Object result = method.invoke(handler, actual);
			return result == null ? fallback : result;
		} catch (ReflectiveOperationException | RuntimeException e) {
			RuntimeStubs.failed("the fluid handler " + handler.getClass().getName() + "." + name, e);
			return fallback;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object action(Class<?> type, boolean execute) {
		return Enum.valueOf((Class) type, execute ? "EXECUTE" : "SIMULATE");
	}

	public static IFluidTankProperties[] properties(Object handler) {
		int tanks = (int) call(handler, "getTanks", null, 0);
		var properties = new IFluidTankProperties[tanks];
		for (int i = 0; i < tanks; i++) {
			int tank = i;
			properties[i] = new IFluidTankProperties() {
				@Nullable
				@Override
				public FluidStack getContents() {
					Object stack = call(handler, "getFluidInTank", int.class, null, tank);
					return stack instanceof FluidStack fluid && fluid.amount > 0 ? fluid.copy() : null;
				}

				@Override
				public int getCapacity() {
					return (int) call(handler, "getTankCapacity", int.class, 0, tank);
				}

				@Override
				public boolean canFill() {
					return true;
				}

				@Override
				public boolean canDrain() {
					return true;
				}

				@Override
				public boolean canFillFluidType(FluidStack stack) {
					return (boolean) call(handler, "isFluidValid", int.class, true, tank, stack);
				}

				@Override
				public boolean canDrainFluidType(FluidStack stack) {
					return true;
				}
			};
		}
		return properties;
	}

	public static int fill(Object handler, FluidStack stack, boolean execute) {
		return (int) call(handler, "fill", FluidStack.class, 0, stack, execute);
	}

	@Nullable
	public static FluidStack drain(Object handler, FluidStack stack, boolean execute) {
		Object result = call(handler, "drain", FluidStack.class, null, stack, execute);
		return result instanceof FluidStack fluid && fluid.amount > 0 ? fluid : null;
	}

	@Nullable
	public static FluidStack drain(Object handler, int amount, boolean execute) {
		Object result = call(handler, "drain", int.class, null, amount, execute);
		return result instanceof FluidStack fluid && fluid.amount > 0 ? fluid : null;
	}
}
