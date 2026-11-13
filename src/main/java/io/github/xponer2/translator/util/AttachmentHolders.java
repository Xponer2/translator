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
package io.github.xponer2.translator.util;

import java.lang.reflect.Method;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * NeoForge's {@code IAttachmentHolder} for the 1.21.1 mods (MethodGuard sends its instanceof and its calls here, the
 * holder first). 1.12.2's entities, block entities, worlds and chunks hold attachments in {@link Attachments}; any
 * other object that is a holder (a class of a mod implementing the interface) is called by name.
 */
public final class AttachmentHolders {
	private AttachmentHolders() {
	}

	private static boolean vanilla(@Nullable Object holder) {
		return holder instanceof Entity || holder instanceof TileEntity || holder instanceof World || holder instanceof Chunk;
	}

	/** A mod's own holder: it implements the carried interface. */
	private static boolean own(@Nullable Object holder) {
		if (holder == null) {
			return false;
		}
		for (Class<?> type = holder.getClass(); type != null; type = type.getSuperclass()) {
			for (Class<?> implemented : type.getInterfaces()) {
				if ("net.neoforged.neoforge.attachment.IAttachmentHolder".equals(implemented.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isHolder(@Nullable Object holder) {
		return vanilla(holder) || own(holder);
	}

	@Nullable
	private static Object call(Object holder, String name, Object... args) {
		for (Method method : holder.getClass().getMethods()) {
			if (method.getName().equals(name) && method.getParameterCount() == args.length
					&& (args.length == 0 || method.getParameterTypes()[0].isInstance(args[0]))) {
				try {
					return method.invoke(holder, args);
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException("Attachment call " + name + " failed on " + holder, e);
				}
			}
		}
		throw new IllegalArgumentException("Object " + holder + " is not an attachment holder");
	}

	public static boolean hasAttachments(Object holder) {
		if (vanilla(holder)) {
			return Attachments.any(holder);
		}
		return Boolean.TRUE.equals(call(holder, "hasAttachments"));
	}

	public static boolean hasData(Object holder, Object type) {
		return vanilla(holder) ? Attachments.has(holder, type) : Boolean.TRUE.equals(call(holder, "hasData", type));
	}

	@Nullable
	public static Object getData(Object holder, Object type) {
		return vanilla(holder) ? Attachments.get(holder, type) : call(holder, "getData", type);
	}

	public static Optional<Object> getExistingData(Object holder, Object type) {
		return Optional.ofNullable(getExistingDataOrNull(holder, type));
	}

	@Nullable
	public static Object getExistingDataOrNull(Object holder, Object type) {
		if (vanilla(holder)) {
			return Attachments.has(holder, type) ? Attachments.get(holder, type) : null;
		}
		return call(holder, "getExistingDataOrNull", type);
	}

	@Nullable
	public static Object setData(Object holder, Object type, Object value) {
		return vanilla(holder) ? Attachments.set(holder, type, value) : call(holder, "setData", type, value);
	}

	@Nullable
	public static Object removeData(Object holder, Object type) {
		return vanilla(holder) ? Attachments.remove(holder, type) : call(holder, "removeData", type);
	}

	/** Attachments aren't sent to the client: both sides of a single player game share them. */
	public static void syncData(Object holder, Object type) {
	}
}
