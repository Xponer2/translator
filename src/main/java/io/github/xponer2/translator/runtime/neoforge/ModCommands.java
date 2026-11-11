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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import io.github.xponer2.translator.runtime.RuntimeStubs;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * The commands of the 1.21.1 mods: {@link RegisterCommandsEvent} is posted when the server starts, and each root
 * command the mods put in its brigadier dispatcher becomes a 1.12.2 command that runs the typed line through that
 * dispatcher with a 1.21 CommandSourceStack of the sender (its position, world, entity and permission level; its
 * messages go to the sender). Tab completion asks the dispatcher too. Brigadier and CommandSourceStack are 1.21.1's
 * own classes (converted, carried), reached by reflection.
 */
public final class ModCommands {
	private ModCommands() {
	}

	/** Posts RegisterCommandsEvent and registers the mods' root commands (a vanilla command's name is left to vanilla). */
	public static void register(FMLServerStartingEvent fml) {
		MinecraftServer server = fml.getServer();
		RegisterCommandsEvent event;
		try {
			event = RegisterCommandsEvent.create(server);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			RuntimeStubs.failed("RegisterCommandsEvent: brigadier's dispatcher", e);
			return;
		}
		ServerEvents.post(event);
		Object dispatcher = RegisterCommandsEvent.getDispatcher(event);
		Collection<?> roots;
		try {
			roots = new ArrayList<>((Collection<?>) call(call(dispatcher, "getRoot"), "getChildren"));
		} catch (ReflectiveOperationException | RuntimeException e) {
			RuntimeStubs.failed("RegisterCommandsEvent: the commands of the mods", e);
			return;
		}
		java.util.Set<String> taken = server.getCommandManager().getCommands().keySet();
		for (Object node : roots) {
			try {
				String name = (String) call(node, "getName");
				if (!taken.contains(name)) {
					fml.registerServerCommand(new Bridge(dispatcher, node, name));
				}
			} catch (ReflectiveOperationException | RuntimeException e) {
				RuntimeStubs.failed("RegisterCommandsEvent: a command of a mod", e);
			}
		}
	}

	/** The public method of that name whose parameters take the arguments (brigadier has execute(String, S) and execute(StringReader, S)). */
	private static Object call(Object target, String name, Object... args) throws ReflectiveOperationException {
		search: for (Method method : target.getClass().getMethods()) {
			if (!method.getName().equals(name) || method.getParameterCount() != args.length) {
				continue;
			}
			Class<?>[] types = method.getParameterTypes();
			for (int i = 0; i < args.length; i++) {
				if (args[i] != null && !(types[i].isPrimitive() || types[i].isInstance(args[i]))) {
					continue search;
				}
			}
			method.setAccessible(true);
			return method.invoke(target, args);
		}
		throw new NoSuchMethodException(target.getClass().getName() + "." + name);
	}

	/** One root command of a mod as a 1.12.2 command. */
	static final class Bridge extends CommandBase {
		private final Object dispatcher;
		private final Object node;
		private final String name;

		Bridge(Object dispatcher, Object node, String name) {
			this.dispatcher = dispatcher;
			this.node = node;
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public String getUsage(ICommandSender sender) {
			return "/" + this.name;
		}

		/** Brigadier's own requirement (the node's requires(source -> source.hasPermission(level))) decides. */
		@Override
		public int getRequiredPermissionLevel() {
			return 0;
		}

		@Override
		public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
			try {
				return Boolean.TRUE.equals(call(this.node, "canUse", source(server, sender)));
			} catch (ReflectiveOperationException | RuntimeException e) {
				return sender.canUseCommand(2, this.name);
			}
		}

		@Override
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			String line = args.length == 0 ? this.name : this.name + " " + String.join(" ", args);
			try {
				call(this.dispatcher, "execute", line, source(server, sender));
			} catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause != null && "com.mojang.brigadier.exceptions.CommandSyntaxException".equals(cause.getClass().getName())) {
					throw new CommandException(String.valueOf(cause.getMessage()));
				}
				RuntimeStubs.failed("the command /" + line, cause == null ? e : cause);
				throw new CommandException("commands.generic.exception");
			} catch (ReflectiveOperationException | RuntimeException e) {
				RuntimeStubs.failed("the command /" + line, e);
				throw new CommandException("commands.generic.exception");
			}
		}

		@Override
		public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos target) {
			String line = this.name + " " + String.join(" ", args);
			try {
				Object parse = call(this.dispatcher, "parse", line, source(server, sender));
				Object suggestions = ((CompletableFuture<?>) call(this.dispatcher, "getCompletionSuggestions", parse)).join();
				List<String> texts = new ArrayList<>();
				for (Object suggestion : (List<?>) call(suggestions, "getList")) {
					texts.add((String) call(suggestion, "getText"));
				}
				return texts;
			} catch (ReflectiveOperationException | RuntimeException e) {
				return Collections.emptyList();
			}
		}
	}

	/** The 1.21 CommandSourceStack of a 1.12.2 command sender. */
	static Object source(MinecraftServer server, ICommandSender sender) throws ReflectiveOperationException {
		Class<?> stackType = Class.forName("net.minecraft.commands.CommandSourceStack", true, Launch.classLoader);
		Class<?> sourceType = Class.forName("net.minecraft.commands.CommandSource", true, Launch.classLoader);
		Object output = Proxy.newProxyInstance(sourceType.getClassLoader(), new Class<?>[] {sourceType}, (proxy, method, args) -> {
			switch (method.getName()) {
				case "sendSystemMessage":
					if (args != null && args.length == 1 && args[0] instanceof ITextComponent) {
						sender.sendMessage((ITextComponent) args[0]);
					}
					return null;
				case "acceptsSuccess":
				case "acceptsFailure":
					return true;
				case "shouldInformAdmins":
					return sender instanceof EntityPlayerMP;
				case "alwaysAccepts":
					return false;
				case "hashCode":
					return System.identityHashCode(proxy);
				case "equals":
					return proxy == args[0];
				case "toString":
					return "CommandSource of " + sender.getName();
				default:
					return method.getReturnType() == boolean.class ? Boolean.FALSE : null;
			}
		});
		Entity entity = sender.getCommandSenderEntity();
		WorldServer world = sender.getEntityWorld() instanceof WorldServer ? (WorldServer) sender.getEntityWorld() : server.getWorld(0);
		Vec2f rotation = entity == null ? new Vec2f(0.0F, 0.0F) : new Vec2f(entity.rotationPitch, entity.rotationYaw);
		for (Constructor<?> constructor : stackType.getConstructors()) {
			if (constructor.getParameterCount() == 9) {
				return constructor.newInstance(output, sender.getPositionVector(), rotation, world, permission(server, sender), sender.getName(),
						sender.getDisplayName(), server, entity);
			}
		}
		throw new NoSuchMethodException("CommandSourceStack's constructor");
	}

	/** The sender's permission level: the highest 1.12.2 allows it (the server console 4). */
	private static int permission(MinecraftServer server, ICommandSender sender) {
		if (sender == server) {
			return 4;
		}
		for (int level = 4; level > 0; level--) {
			if (sender.canUseCommand(level, "")) {
				return level;
			}
		}
		return 0;
	}
}
