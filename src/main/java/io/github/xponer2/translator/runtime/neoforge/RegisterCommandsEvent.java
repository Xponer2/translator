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

import javax.annotation.Nullable;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * NeoForge {@code RegisterCommandsEvent}: the mods add their commands to a brigadier dispatcher (1.21.1's brigadier, a
 * converted library the Translator doesn't compile against: the getters are redirected to the static ones here, which
 * give Objects the caller casts back). {@link ModCommands} makes each root command a 1.12.2 one. It was never posted:
 * no command of any 1.21.1 mod existed (AE2's, Create's, Mekanism's, Twilight Forest's, Patchouli's, ...).
 */
public class RegisterCommandsEvent extends Event {
	private final Object dispatcher;
	@Nullable
	private final Object buildContext;
	@Nullable
	private final Object selection;

	public RegisterCommandsEvent(Object dispatcher, @Nullable Object buildContext, @Nullable Object selection) {
		this.dispatcher = dispatcher;
		this.buildContext = buildContext;
		this.selection = selection;
	}

	/** 1.21 {@code event.getDispatcher()}: the CommandDispatcher. */
	public static Object getDispatcher(RegisterCommandsEvent event) {
		return event.dispatcher;
	}

	/** 1.21 {@code event.getBuildContext()}: a CommandBuildContext over the Translator's registries (null if none could be made). */
	@Nullable
	public static Object getBuildContext(RegisterCommandsEvent event) {
		return event.buildContext;
	}

	/** 1.21 {@code event.getCommandSelection()}: DEDICATED or INTEGRATED. */
	@Nullable
	public static Object getCommandSelection(RegisterCommandsEvent event) {
		return event.selection;
	}

	/** A new event for the server: a fresh dispatcher, the build context and the selection, made through 1.21.1's own classes. */
	static RegisterCommandsEvent create(MinecraftServer server) throws ReflectiveOperationException {
		Object dispatcher = Class.forName("com.mojang.brigadier.CommandDispatcher", true, Launch.classLoader).getConstructor().newInstance();
		return new RegisterCommandsEvent(dispatcher, buildContext(), selection(server));
	}

	@Nullable
	private static Object buildContext() {
		try {
			Class<?> context = Class.forName("net.minecraft.commands.CommandBuildContext", true, Launch.classLoader);
			Class<?> flags = Class.forName("net.minecraft.world.flag.FeatureFlags", true, Launch.classLoader);
			Object vanilla = flags.getField("VANILLA_SET").get(null);
			for (Method method : context.getMethods()) {
				if ("simple".equals(method.getName()) && method.getParameterCount() == 2) {
					return method.invoke(null, io.github.xponer2.translator.registry.RegistryAccess.INSTANCE, vanilla);
				}
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("RegisterCommandsEvent: the command build context", e);
		}
		return null;
	}

	@Nullable
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Object selection(MinecraftServer server) {
		try {
			Class type = Class.forName("net.minecraft.commands.Commands$CommandSelection", true, Launch.classLoader);
			return Enum.valueOf(type, server.isDedicatedServer() ? "DEDICATED" : "INTEGRATED");
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}
}
