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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * A server world's queue of chunks to unload as a concurrent set: 1.12.2 iterates it while it unloads them, and a
 * 1.21.1 mod's code run by an unload (a block entity's onChunkUnloaded, a chunk unload event) can queue another chunk,
 * which crashed the server (ConcurrentModificationException in ChunkProviderServer.tick). Such a chunk now unloads in
 * the same or the next tick. The first one is logged with the code that queued it.
 */
public final class ChunkUnloadGuard {
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {
		if (!(event.getWorld() instanceof WorldServer world)) {
			return;
		}
		ChunkProviderServer provider = world.getChunkProvider();
		try {
			var field = ReflectionHelper.findField(ChunkProviderServer.class, "droppedChunks", "field_73248_b");
			@SuppressWarnings("unchecked")
			Set<Long> queue = (Set<Long>) field.get(provider);
			if (queue instanceof Guarded) {
				return;
			}
			var modifiers = java.lang.reflect.Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
			modifiers.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
			field.set(provider, new Guarded(queue));
		} catch (ReflectiveOperationException | RuntimeException e) {
			RuntimeMods.LOGGER.warn("Can't guard the chunk unload queue of dimension {}: {}", world.provider.getDimension(), e.toString());
		}
	}

	/** A concurrent set that notes the first chunk queued while the server unloads chunks (who did it). */
	private static final class Guarded extends java.util.AbstractSet<Long> {
		private static volatile boolean reported;
		private final Set<Long> set = ConcurrentHashMap.newKeySet();
		/** Between iterator() and its end: only then is a queued chunk looked at (a stack walk is slow). */
		private volatile boolean iterating;

		Guarded(Collection<Long> initial) {
			this.set.addAll(initial);
		}

		@Override
		public boolean add(Long chunk) {
			if (!reported && this.iterating && unloading()) {
				reported = true;
				RuntimeMods.LOGGER.warn("A chunk was queued for unloading while chunks unload (crashed 1.12.2 before)", new Throwable("queued here"));
			}
			return this.set.add(chunk);
		}

		private static boolean unloading() {
			for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
				if (frame.getClassName().equals(ChunkProviderServer.class.getName()) && ("tick".equals(frame.getMethodName())
						|| "func_73156_b".equals(frame.getMethodName()))) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean remove(Object chunk) {
			return this.set.remove(chunk);
		}

		@Override
		public boolean contains(Object chunk) {
			return this.set.contains(chunk);
		}

		@Override
		public Iterator<Long> iterator() {
			this.iterating = true;
			Iterator<Long> iterator = this.set.iterator();
			return new Iterator<Long>() {
				@Override
				public boolean hasNext() {
					boolean more = iterator.hasNext();
					if (!more) {
						Guarded.this.iterating = false;
					}
					return more;
				}

				@Override
				public Long next() {
					return iterator.next();
				}

				@Override
				public void remove() {
					iterator.remove();
				}
			};
		}

		@Override
		public int size() {
			return this.set.size();
		}

		@Override
		public void clear() {
			this.set.clear();
		}
	}
}
