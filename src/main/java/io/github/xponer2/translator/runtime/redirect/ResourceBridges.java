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

import io.github.xponer2.translator.resources.ResourceManager;
import io.github.xponer2.translator.runtime.RuntimeStubs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;

/**
 * The client's 1.21 {@code ReloadableResourceManager} (= the Translator's client resource manager, R line):
 * {@code registerReloadListener} hooks a 1.21 listener into the 1.12.2 resource reloads. A listener of the
 * registration event runs once right away, as the 1.21 game runs them at its first resource load (map mods build their
 * HUD there, before a world exists); one a mod adds itself later runs at the next reload only, as in 1.21 (1.12.2 ran it
 * at once: JourneyMap adds its mob icon listener after loading the icons, and the listener threw them away again, so
 * no mob had an icon). Client only.
 */
public final class ResourceBridges {
	private ResourceBridges() {
	}

	/**
	 * The listener as a 1.12.2 one: a 1.21 ResourceManagerReloadListener (carried) by its
	 * onResourceManagerReload(ResourceManager); any other PreparableReloadListener (GeckoLib's model cache, the
	 * SimpleJsonResourceReloadListeners of the mods) by its reload(...) with every stage run at once on this thread.
	 */
	public static void registerReloadListener(ResourceManager.Client manager, Object listener) {
		register(listener, false);
	}

	/** A listener of the registration event: run once now too (the 1.21 game's first load). */
	public static void registerAndRun(Object listener) {
		register(listener, true);
	}

	private static void register(Object listener, boolean runNow) {
		Method reload = find(listener.getClass());
		Method preparable = reload == null ? findPreparable(listener.getClass()) : null;
		if (reload == null && preparable == null) {
			RuntimeStubs.missing("ReloadableResourceManager.registerReloadListener (" + listener.getClass().getName() + ": no reload method)");
			return;
		}
		java.util.concurrent.atomic.AtomicBoolean ran = new java.util.concurrent.atomic.AtomicBoolean();
		IResourceManagerReloadListener hook = resources -> {
			ran.set(true);
			try {
				if (reload != null) {
					reload.invoke(listener, ResourceManager.client());
				} else {
					runPreparable(listener, preparable);
				}
			} catch (ReflectiveOperationException | RuntimeException e) {
				Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ite ? ite.getCause() : e;
				if (cause instanceof java.util.concurrent.CompletionException completion && completion.getCause() != null) {
					cause = completion.getCause();
				}
				RuntimeStubs.failed(listener.getClass().getName() + (reload != null ? ".onResourceManagerReload" : ".reload"), cause);
			}
		};
		Object resources = Minecraft.getMinecraft().getResourceManager();
		if (!runNow && resources instanceof net.minecraft.client.resources.SimpleReloadableResourceManager simple) {
			try {
				java.util.List<IResourceManagerReloadListener> listeners = net.minecraftforge.fml.common.ObfuscationReflectionHelper
						.getPrivateValue(net.minecraft.client.resources.SimpleReloadableResourceManager.class, simple, "field_110546_b");
				listeners.add(hook);
				if (!Minecraft.getMinecraft().isCallingFromMinecraftThread() || Minecraft.getMinecraft().currentScreen == null
						&& Minecraft.getMinecraft().world == null) {
					// added while the game still loads (GeckoLib's model cache, from its mod's constructor): 1.21 runs
					// it at its first resource load, which on 1.12.2 may be over by then; run once the game has
					// loaded unless a reload ran it (every GeckoLib model was missing: "Unable to find model")
					Pending.add(() -> {
						if (!ran.get()) {
							hook.onResourceManagerReload(Minecraft.getMinecraft().getResourceManager());
						}
					});
				}
				return;
			} catch (RuntimeException e) {
				// as 1.12.2 adds it, run once now
			}
		}
		((IReloadableResourceManager) resources).registerReloadListener(hook);
	}

	/** The listeners added while the game loaded, run (if no reload did) on the first client tick after it loaded. */
	public static final class Pending {
		private static final java.util.List<Runnable> TASKS = new java.util.ArrayList<>();
		private static boolean listening;

		private Pending() {
		}

		static synchronized void add(Runnable task) {
			TASKS.add(task);
			if (!listening) {
				listening = true;
				net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Pending());
			}
		}

		@net.minecraftforge.fml.common.eventhandler.SubscribeEvent
		public void onTick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
			Minecraft minecraft = Minecraft.getMinecraft();
			if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END
					|| !(minecraft.currentScreen instanceof net.minecraft.client.gui.GuiMainMenu) && minecraft.world == null) {
				return;
			}
			java.util.List<Runnable> tasks;
			synchronized (Pending.class) {
				tasks = new java.util.ArrayList<>(TASKS);
				TASKS.clear();
				listening = false;
			}
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
			for (Runnable task : tasks) {
				task.run();
			}
		}
	}

	/** 1.21 {@code PreparableReloadListener.reload(barrier, manager, prepareProfiler, applyProfiler, background, game)}. */
	private static Method findPreparable(Class<?> type) {
		for (Method method : type.getMethods()) {
			if ("reload".equals(method.getName()) && method.getParameterCount() == 6
					&& java.util.concurrent.CompletableFuture.class.isAssignableFrom(method.getReturnType())) {
				method.setAccessible(true);
				return method;
			}
		}
		return null;
	}

	/**
	 * A preparable listener's stages run at once on this thread: its executors run each task right away, its barrier
	 * lets every result through, and the future is waited for (1.12.2 reloads the resources on the client thread).
	 */
	private static void runPreparable(Object listener, Method reload) throws ReflectiveOperationException {
		Class<?>[] params = reload.getParameterTypes();
		Object barrier = java.lang.reflect.Proxy.newProxyInstance(params[0].getClassLoader(), new Class<?>[] {params[0]}, (proxy, method, args) -> {
			switch (method.getName()) {
			case "hashCode":
				return System.identityHashCode(proxy);
			case "equals":
				return proxy == args[0];
			case "toString":
				return "reload barrier";
			default:
				// wait(result): the result at once
				return java.util.concurrent.CompletableFuture.completedFuture(args != null && args.length > 0 ? args[0] : null);
			}
		});
		java.util.concurrent.Executor now = Runnable::run;
		Object profiler = profiler(params[2]);
		long start = System.nanoTime();
		Object future = reload.invoke(listener, barrier, ResourceManager.client(), profiler, profiler, now, now);
		if (future instanceof java.util.concurrent.CompletableFuture<?> stages) {
			stages.join();
		}
		LOGGER.info("Resource listener {} ran in {} ms{}", listener.getClass().getName(), (System.nanoTime() - start) / 1_000_000, loaded(listener));
	}

	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator Runtime");

	/**
	 * What a model cache loaded, for the log: the sizes its class gives by getBakedModels / getBakedAnimations
	 * (GeckoLib's: "N models"), nothing for the others.
	 */
	private static String loaded(Object listener) {
		String name = listener.getClass().getName();
		int lambda = name.indexOf("$$Lambda");
		var text = new StringBuilder();
		try {
			Class<?> owner = lambda > 0 ? Class.forName(name.substring(0, lambda), false, listener.getClass().getClassLoader()) : listener.getClass();
			for (String getter : new String[] {"getBakedModels", "getBakedAnimations"}) {
				try {
					Object value = owner.getMethod(getter).invoke(null);
					if (value instanceof java.util.Map<?, ?> map) {
						text.append(", ").append(map.size()).append(getter.equals("getBakedModels") ? " models" : " animations");
					}
				} catch (ReflectiveOperationException | RuntimeException e) {
					// not a model cache
				}
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			// no owner class
		}
		return text.toString();
	}

	/** A profiler of the type the listener takes: 1.12.2's (the tables' ProfilerFiller), else one that records nothing. */
	private static Object profiler(Class<?> type) {
		if (type.isAssignableFrom(net.minecraft.profiler.Profiler.class)) {
			return new net.minecraft.profiler.Profiler();
		}
		if (type.isInterface()) {
			return java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
				Class<?> returned = method.getReturnType();
				if (returned == boolean.class) {
					return false;
				}
				if (returned.isPrimitive() && returned != void.class) {
					return returned == long.class ? (Object) 0L : returned == int.class ? (Object) 0 : returned == float.class ? (Object) 0.0F
							: returned == double.class ? (Object) 0.0D : null;
				}
				return returned.isInstance(proxy) ? proxy : null;
			});
		}
		return null;
	}

	private static Method find(Class<?> type) {
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			for (Method method : c.getMethods()) {
				if (method.getName().equals("onResourceManagerReload") && method.getParameterCount() == 1
						&& method.getParameterTypes()[0].isAssignableFrom(ResourceManager.Client.class)) {
					method.setAccessible(true);
					return method;
				}
			}
		}
		return null;
	}
}
