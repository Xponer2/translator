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
package io.github.xponer2.translator.client.particle;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The mods' own particles (NeoForge's RegisterParticleProvidersEvent, 1.21's ParticleEngine): their providers by
 * particle type, the sprite sets of their particles/*.json (stitched into the blocks atlas, the sheet 1.12.2's
 * particle layer 1 draws from), and a particle made by its provider when a mod adds one to the client's world. Before,
 * the providers were dropped and a mod's particle was never made: no particle of any mod was seen.
 */
@SideOnly(Side.CLIENT)
public final class ParticleProviders {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator Runtime");

	/** What makes a particle of a type: the mod's provider, called by its createParticle. */
	private static final Map<Object, Object> PROVIDERS = new IdentityHashMap<>();
	/** Types whose provider picks the sprite of its particle itself from the set (registerSprite). */
	private static final Map<Object, Object> SPRITE_PROVIDERS = new IdentityHashMap<>();
	/** 1.21 {@code ParticleEngine.spriteSets}: the sprite set of each type with one, by the type's id. */
	public static final Map<ResourceLocation, Object> SPRITE_SETS = new ConcurrentHashMap<>();
	private static final Map<Object, Object> SETS_BY_TYPE = new IdentityHashMap<>();
	/** The textures of each set (from its particles json), read at the atlas stitch. */
	private static final Map<ResourceLocation, List<ResourceLocation>> TEXTURES = new HashMap<>();
	private static final Map<Class<?>, Method> CREATE = new ConcurrentHashMap<>();
	private static final java.util.Set<Object> FAILED = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

	/** Subscribed in pre-init: the stitch handlers below. */
	public ParticleProviders() {
	}

	/** The providers before the first stitch (the mods registered their types and listeners in pre-init). */
	@SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.HIGHEST)
	public void beforeStitch(TextureStitchEvent.Pre event) {
		io.github.xponer2.translator.runtime.neoforge.ClientEvents.postParticleProviders();
	}

	/** 1.21 {@code level.addParticle} from common code (Particles): a mod's particle on the client's world. */
	public static boolean addFromCommon(Object level, Object options, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		return level instanceof WorldClient client && add(client, options, x, y, z, xSpeed, ySpeed, zSpeed);
	}

	// --- registration ------------------------------------------------------------------------------------------------

	/** NeoForge {@code registerSpecial(type, provider)}: a provider with no sprites. */
	public static synchronized void registerSpecial(Object type, Object provider) {
		PROVIDERS.put(type, provider);
	}

	/** NeoForge {@code registerSpriteSet(type, registration)}: the provider made with the type's sprite set. */
	public static synchronized void registerSpriteSet(Object type, Object registration) {
		Object set = spriteSet(type);
		try {
			ClassLoader loader = ParticleProviders.class.getClassLoader();
			Object provider = Class.forName("net.minecraft.client.particle.ParticleEngine$SpriteParticleRegistration", false, loader)
					.getMethod("create", Class.forName("net.minecraft.client.particle.SpriteSet", false, loader)).invoke(registration, set);
			if (provider != null) {
				PROVIDERS.put(type, provider);
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			LOGGER.warn("The particle provider of {} wasn't made", id(type), e instanceof java.lang.reflect.InvocationTargetException ite ? ite.getCause() : e);
		}
	}

	/** NeoForge {@code registerSprite(type, sprite)}: a particle of one sprite, picked from the type's set. */
	public static synchronized void registerSprite(Object type, Object spriteProvider) {
		spriteSet(type);
		SPRITE_PROVIDERS.put(type, spriteProvider);
		PROVIDERS.put(type, spriteProvider);
	}

	/** The (carried) MutableSpriteSet of a type, made once and filled at the atlas stitch. */
	private static Object spriteSet(Object type) {
		Object set = SETS_BY_TYPE.get(type);
		if (set == null) {
			try {
				set = Class.forName("net.minecraft.client.particle.ParticleEngine$MutableSpriteSet", true, ParticleProviders.class.getClassLoader())
						.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("no 1.21 sprite set", e);
			}
			SETS_BY_TYPE.put(type, set);
			ResourceLocation id = id(type);
			if (id != null) {
				SPRITE_SETS.put(id, set);
			}
		}
		return set;
	}

	@Nullable
	private static ResourceLocation id(Object type) {
		try {
			return io.github.xponer2.translator.util.BuiltInRegistries.byKey(io.github.xponer2.translator.util.Registries.PARTICLE_TYPE).getKey(type);
		} catch (RuntimeException e) {
			return null;
		}
	}

	// --- sprites -----------------------------------------------------------------------------------------------------

	/**
	 * The textures of every sprite set (assets/<ns>/particles/<name>.json, "textures": ["ns:name", ...] in
	 * textures/particle/) go into the blocks atlas.
	 */
	@SubscribeEvent
	public void onStitch(TextureStitchEvent.Pre event) {
		TEXTURES.clear();
		// the ids of the types (one registered after its provider gets it now)
		synchronized (ParticleProviders.class) {
			for (Map.Entry<Object, Object> entry : SETS_BY_TYPE.entrySet()) {
				ResourceLocation id = id(entry.getKey());
				if (id != null) {
					SPRITE_SETS.putIfAbsent(id, entry.getValue());
				}
			}
		}
		int added = 0;
		for (Map.Entry<ResourceLocation, Object> entry : SPRITE_SETS.entrySet()) {
			ResourceLocation id = entry.getKey();
			List<ResourceLocation> textures = readTextures(id);
			TEXTURES.put(id, textures);
			for (ResourceLocation texture : textures) {
				event.getMap().registerSprite(texture);
				added++;
			}
		}
		if (added > 0) {
			LOGGER.info("Blocks atlas: {} particle textures of {} mod particle types added", added, SPRITE_SETS.size());
		}
	}

	/** Each set gets its sprites once the atlas is made. */
	@SubscribeEvent
	public void onStitched(TextureStitchEvent.Post event) {
		if (TEXTURES.isEmpty()) {
			return;
		}
		for (Map.Entry<ResourceLocation, Object> entry : SPRITE_SETS.entrySet()) {
			List<TextureAtlasSprite> sprites = new ArrayList<>();
			for (ResourceLocation texture : TEXTURES.getOrDefault(entry.getKey(), java.util.Collections.emptyList())) {
				sprites.add(event.getMap().getAtlasSprite(texture.toString()));
			}
			if (sprites.isEmpty()) {
				sprites.add(event.getMap().getMissingSprite());
			}
			try {
				entry.getValue().getClass().getMethod("rebind", List.class).invoke(entry.getValue(), sprites);
			} catch (ReflectiveOperationException | RuntimeException e) {
				LOGGER.warn("The sprites of particle {} weren't set", entry.getKey(), e);
			}
		}
	}

	/** The atlas names (ns:particle/name) of the textures of a particle's json; none when it has no json. */
	private static List<ResourceLocation> readTextures(ResourceLocation id) {
		List<ResourceLocation> textures = new ArrayList<>();
		ResourceLocation file = new ResourceLocation(id.getNamespace(), "particles/" + id.getPath() + ".json");
		try {
			var resource = io.github.xponer2.translator.resources.ResourceManager.client().getResource(file);
			if (!resource.isPresent()) {
				return textures;
			}
			try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
				var json = new JsonParser().parse(reader).getAsJsonObject();
				if (json.has("textures")) {
					for (JsonElement element : json.getAsJsonArray("textures")) {
						ResourceLocation texture = new ResourceLocation(element.getAsString());
						textures.add(new ResourceLocation(texture.getNamespace(), "particle/" + texture.getPath()));
					}
				}
			}
		} catch (java.io.IOException | RuntimeException e) {
			LOGGER.warn("Couldn't read the particle {}", file, e);
		}
		return textures;
	}

	// --- making a particle -------------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code level.addParticle(options, ...)} of a mod's particle on the client: made by its type's provider and
	 * added to the game's particles. False when its type has no provider.
	 */
	public static boolean add(WorldClient level, Object options, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		Particle particle = create(level, options, x, y, z, xSpeed, ySpeed, zSpeed);
		if (particle == null) {
			return false;
		}
		Minecraft.getMinecraft().effectRenderer.addEffect(particle);
		return true;
	}

	/** 1.21 {@code particleEngine.createParticle(options, ...)}: the particle of the options' provider, null without one. */
	@Nullable
	public static Particle create(WorldClient level, Object options, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		Object type = type(options);
		Object provider;
		Object spriteProvider;
		synchronized (ParticleProviders.class) {
			provider = PROVIDERS.get(type);
			spriteProvider = SPRITE_PROVIDERS.get(type);
		}
		if (provider == null || FAILED.contains(provider)) {
			return null;
		}
		try {
			Method create = CREATE.computeIfAbsent(provider.getClass(), ParticleProviders::createMethod);
			Object made = create.invoke(provider, options, level, x, y, z, xSpeed, ySpeed, zSpeed);
			if (made != null && spriteProvider != null) {
				Object set = SETS_BY_TYPE.get(type);
				made.getClass().getMethod("pickSprite", Class.forName("net.minecraft.client.particle.SpriteSet", false, ParticleProviders.class.getClassLoader()))
						.invoke(made, set);
			}
			return made instanceof Particle particle ? particle : null;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			FAILED.add(provider);
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the particle provider of " + id(type),
					e instanceof java.lang.reflect.InvocationTargetException ite ? ite.getCause() : e);
			return null;
		}
	}

	private static Method createMethod(Class<?> provider) {
		for (Class<?> c = provider; c != null; c = c.getSuperclass()) {
			for (Method method : c.getDeclaredMethods()) {
				if ("createParticle".equals(method.getName()) && method.getParameterCount() == 8 && !method.isBridge()) {
					method.setAccessible(true);
					return method;
				}
			}
		}
		for (Method method : provider.getMethods()) {
			if ("createParticle".equals(method.getName()) && method.getParameterCount() == 8) {
				method.setAccessible(true);
				return method;
			}
		}
		throw new IllegalStateException("no createParticle in " + provider.getName());
	}

	/** The options' type: 1.21's {@code options.getType()} (a simple type is its own). */
	private static Object type(Object options) {
		try {
			Method getType = options.getClass().getMethod("getType");
			Object type = getType.invoke(options);
			return type != null ? type : options;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return options;
		}
	}
}
