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
package io.github.xponer2.translator.entity;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.xponer2.translator.registry.EntityType;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The spawn rules of the mods' mobs (NeoForge {@code RegisterSpawnPlacementsEvent}). The event was never posted, so a
 * mod's mob spawned wherever 1.12.2 lets any creature: Iron's Spellbooks' necromancers (not monsters to 1.12.2, so no
 * darkness check) walked about in daylight everywhere. Posted once after the mods are set up; a natural spawn of a
 * mob with a rule goes ahead only when the rule allows it (1.12.2's own checks come after, as before).
 */
public final class SpawnRules {
	private static final Logger LOGGER = LogManager.getLogger("Translator");
	private static final Map<Object, Object> PREDICATES = new IdentityHashMap<>();
	private static Method test;
	private static boolean posted;
	private static boolean reported;

	private SpawnRules() {
	}

	public static synchronized void register() {
		if (posted) {
			return;
		}
		posted = true;
		ClassLoader loader = net.minecraft.launchwrapper.Launch.classLoader;
		try {
			Class<?> eventType = Class.forName("net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent", true, loader);
			Map<Object, Object> merged = new HashMap<>();
			MinecraftForge.EVENT_BUS.post((Event) eventType.getConstructor(Map.class).newInstance(merged));
			Method build = Class.forName("net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent$MergedSpawnPredicate", true, loader)
					.getMethod("build");
			test = Class.forName("net.minecraft.world.entity.SpawnPlacements$SpawnPredicate", true, loader).getMethod("test", EntityType.class,
					World.class, MobSpawnType.class, BlockPos.class, java.util.Random.class);
			for (Map.Entry<Object, Object> entry : merged.entrySet()) {
				Object predicate = build.invoke(entry.getValue());
				if (entry.getKey() != null && predicate != null) {
					PREDICATES.put(entry.getKey(), predicate);
				}
			}
			if (!PREDICATES.isEmpty()) {
				MinecraftForge.EVENT_BUS.register(new SpawnRules());
				LOGGER.info("Spawn rules of {} mobs of the 1.21.1 mods", PREDICATES.size());
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			Throwable cause = e instanceof java.lang.reflect.InvocationTargetException invocation && invocation.getCause() != null ? invocation.getCause() : e;
			LOGGER.warn("The spawn rules of the 1.21.1 mods weren't registered: {}", cause.toString());
		}
	}

	/** A natural spawn (not a spawner's) of a mob with a rule: denied when the rule says no. */
	@SubscribeEvent
	public void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
		if (event.isSpawner() || event.getWorld() == null || event.getWorld().isRemote) {
			return;
		}
		EntityType<?> type = EntityType.of(event.getEntity());
		Object predicate = type == null ? null : PREDICATES.get(type);
		if (predicate == null) {
			return;
		}
		World world = event.getWorld();
		try {
			Object allowed = test.invoke(predicate, type, world, MobSpawnType.NATURAL, new BlockPos(event.getX(), event.getY(), event.getZ()), world.rand);
			if (Boolean.FALSE.equals(allowed)) {
				event.setResult(Event.Result.DENY);
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			// a rule that can't run here says nothing: 1.12.2's own checks decide, as before
			PREDICATES.remove(type);
			if (!reported) {
				reported = true;
				Throwable cause = e instanceof java.lang.reflect.InvocationTargetException invocation && invocation.getCause() != null ? invocation.getCause()
						: e;
				LOGGER.warn("The spawn rule of {} failed, 1.12.2's own checks decide: {}", type, cause.toString());
			}
		}
	}
}
