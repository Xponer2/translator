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
package io.github.xponer2.translator.runtime.neoforge;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import io.github.xponer2.translator.recipe.ICondition;
import io.github.xponer2.translator.resources.ResourceManager;
import io.github.xponer2.translator.resources.ServerResourceManager;
import io.github.xponer2.translator.runtime.RuntimeStubs;

import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * NeoForge {@code AddReloadListenerEvent}: the mods add their server data loaders (1.21 PreparableReloadListeners, a
 * carried interface: {@link #addListener(AddReloadListenerEvent, Object)} takes them), which then load the server's data
 * ({@link ServerResourceManager}: data/ of the mods' jars and of Minecraft 1.21.1). Posted when the server is about to
 * start. It was never posted: the server data of 15 mods (Iron's Spells' spells, Curios' slots, Create's, Mekanism's,
 * the Aether's, Twilight Forest's, Ars Nouveau's ...) was never loaded.
 */
public class AddReloadListenerEvent extends Event {
	private final List<Object> listeners = new ArrayList<>();

	/** 1.21 {@code event.addListener(listener)} (redirected here: the listener's type is carried). */
	public static void addListener(AddReloadListenerEvent event, @Nullable Object listener) {
		if (listener != null) {
			event.listeners.add(listener);
		}
	}

	/** The same under the table's general name for addListener. */
	public static void register(AddReloadListenerEvent event, @Nullable Object listener) {
		addListener(event, listener);
	}

	public List<Object> getListeners() {
		return Collections.unmodifiableList(this.listeners);
	}

	public io.github.xponer2.translator.registry.RegistryAccess getRegistryAccess() {
		return io.github.xponer2.translator.registry.RegistryAccess.INSTANCE;
	}

	public ServerResources getServerResources() {
		return ServerResources.INSTANCE;
	}

	public ICondition.IContext getConditionContext() {
		return ICondition.IContext.EMPTY;
	}

	/** Posts the event and loads the server's data with the listeners the mods added, one by one. */
	public static void fire() {
		AddReloadListenerEvent event = new AddReloadListenerEvent();
		ServerEvents.post(event);
		ResourceManager manager = ServerResourceManager.get();
		for (Object listener : event.listeners) {
			reload(listener, manager);
		}
	}

	/**
	 * Runs a 1.21 reload listener at once: NeoForge's context first (a ContextAwareReloadListener), then its
	 * {@code reload(barrier, manager, profiler, profiler, background, game)} with a barrier that waits for nothing and
	 * both executors this thread, joined; or a ResourceManagerReloadListener's onResourceManagerReload(manager).
	 */
	static void reload(Object listener, ResourceManager manager) {
		try {
			for (Method method : listener.getClass().getMethods()) {
				if ("injectContext".equals(method.getName()) && method.getParameterCount() == 2) {
					method.setAccessible(true);
					method.invoke(listener, ICondition.IContext.EMPTY, io.github.xponer2.translator.registry.RegistryAccess.INSTANCE);
					break;
				}
			}
			Method reload = null;
			Method plain = null;
			for (Method method : listener.getClass().getMethods()) {
				if ("reload".equals(method.getName()) && method.getParameterCount() == 6 && method.getParameterTypes()[0].isInterface()) {
					reload = method;
				} else if ("onResourceManagerReload".equals(method.getName()) && method.getParameterCount() == 1) {
					plain = method;
				}
			}
			if (reload != null) {
				reload.setAccessible(true);
				Class<?> barrierType = reload.getParameterTypes()[0];
				Object barrier = Proxy.newProxyInstance(barrierType.getClassLoader(), new Class<?>[] {barrierType}, (proxy, called, args) -> {
					switch (called.getName()) {
						case "wait":
							return CompletableFuture.completedFuture(args == null || args.length == 0 ? null : args[0]);
						case "hashCode":
							return System.identityHashCode(proxy);
						case "equals":
							return proxy == args[0];
						case "toString":
							return "Translator's reload barrier";
						default:
							return null;
					}
				});
				Executor here = Runnable::run;
				Object profiler = new net.minecraft.profiler.Profiler();
				Object done = reload.invoke(listener, barrier, manager, profiler, profiler, here, here);
				if (done instanceof CompletableFuture<?>) {
					((CompletableFuture<?>) done).join();
				}
			} else if (plain != null) {
				plain.setAccessible(true);
				plain.invoke(listener, manager);
			} else {
				RuntimeStubs.missing("AddReloadListenerEvent: " + listener.getClass().getName() + " has no reload");
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e;
			RuntimeStubs.failed(listener.getClass().getName() + ".reload (server data)", cause);
		}
	}
}
