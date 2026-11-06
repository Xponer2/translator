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
package io.github.xponer2.translator.client.model;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.ITransformation;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * NeoForge's geometry loaders on 1.12.2's model loading: a model whose json (or a parent's) names a {@code "loader"} a
 * mod registered (ModelEvent.RegisterGeometryLoaders) is read by that loader and baked by its geometry, with a baking
 * context, a model baker and sprites of the 1.12.2 atlas. Its 1.21 BlockModels (the parts mods assemble their model
 * from) bake here too, from their json elements. Without it every such model was missing (Sophisticated Backpacks'
 * backpacks, placed and held, were invisible).
 */
@SideOnly(Side.CLIENT)
public final class GeometryLoaders implements ICustomModelLoader {
	public static final GeometryLoaders INSTANCE = new GeometryLoaders();
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator");

	private final Map<ResourceLocation, Object> loaders = new ConcurrentHashMap<>();
	private final Map<ResourceLocation, Optional<Resolved>> resolved = new ConcurrentHashMap<>();
	private volatile boolean registered;

	private GeometryLoaders() {
	}

	/** {@code event.register(id, loader)}: a mod's loader, by the id its models name. */
	public void register(ResourceLocation id, Object loader) {
		if (id != null && loader != null) {
			this.loaders.put(id, loader);
			if (!this.registered) {
				this.registered = true;
				ModelLoaderRegistry.registerLoader(this);
			}
		}
	}

	@Override
	public void onResourceManagerReload(IResourceManager manager) {
		this.resolved.clear();
	}

	/** From the start: NeoForge's own loaders (composite, separate transforms, OBJ) need no mod to register them. */
	public void install() {
		if (!this.registered) {
			this.registered = true;
			ModelLoaderRegistry.registerLoader(this);
		}
	}

	@Override
	public boolean accepts(ResourceLocation location) {
		return !(location instanceof ModelResourceLocation) && this.resolve(model(location)) != null;
	}

	/** Forge asks with the file's path ("ns:models/block/x"), the json names models without "models/". */
	private static ResourceLocation model(ResourceLocation location) {
		String path = location.getPath();
		return path.startsWith("models/") ? new ResourceLocation(location.getNamespace(), path.substring("models/".length())) : location;
	}

	@Override
	public IModel loadModel(ResourceLocation actual) throws Exception {
		ResourceLocation location = model(actual);
		Resolved model = this.resolve(location);
		if (!this.loaders.containsKey(model.loader)) {
			if ("neoforge:fluid_container".equals(model.loader.toString())) {
				IModel bucket = fluidContainer(model.json);
				if (bucket != null) {
					return bucket;
				}
			}
			return new NeoForgeModels.Model(location, model.loader, model.json, model.textures, model.display);
		}
		Object loader = this.loaders.get(model.loader);
		Method read = method(loader.getClass(), "read", 2);
		Object geometry = read.invoke(loader, model.json, CONTEXT);
		return new GeometryModel(location, model, geometry);
	}

	/**
	 * NeoForge's {@code fluid_container} model (a mod's fluid bucket: its fluid drawn into the bucket) as 1.12.2 Forge's
	 * dynamic bucket, with Forge's own bucket textures (base, fluid mask, cover) and the fluid's still texture; the
	 * plain json model it was read as had no texture of its own (purple and black).
	 */
	@Nullable
	private static IModel fluidContainer(JsonObject json) {
		if (!json.has("fluid")) {
			return null;
		}
		ResourceLocation id = new ResourceLocation(json.get("fluid").getAsString());
		net.minecraftforge.fluids.Fluid fluid = io.github.xponer2.translator.fluids.FluidBase.byId(id);
		if (fluid == null) {
			fluid = net.minecraftforge.fluids.FluidRegistry.getFluid(id.getPath());
		}
		if (fluid == null) {
			return null;
		}
		boolean flipGas = json.has("flip_gas") && json.get("flip_gas").getAsBoolean();
		boolean tint = !json.has("apply_tint") || json.get("apply_tint").getAsBoolean();
		return new net.minecraftforge.client.model.ModelDynBucket(new ResourceLocation("forge", "items/bucket_base"),
				new ResourceLocation("forge", "items/bucket_fluid"), new ResourceLocation("forge", "items/bucket_cover"), fluid, flipGas, tint);
	}

	// --- the json, with its parents' textures and display ---------------------------------------------------------------

	private static final class Resolved {
		ResourceLocation loader;
		JsonObject json;
		final Map<String, String> textures = new LinkedHashMap<>();
		JsonObject display = new JsonObject();
		boolean ambientOcclusion = true;
		@Nullable
		ResourceLocation renderType;
	}

	@Nullable
	private Resolved resolve(ResourceLocation location) {
		return this.resolved.computeIfAbsent(location, key -> Optional.ofNullable(this.read(key))).orElse(null);
	}

	@Nullable
	private Resolved read(ResourceLocation location) {
		var result = new Resolved();
		var seen = new HashSet<ResourceLocation>();
		ResourceLocation current = location;
		while (current != null && seen.add(current) && seen.size() < 32) {
			JsonObject json = json(current);
			if (json == null) {
				return null;
			}
			// the child's textures and display win over the parent's
			if (json.has("textures") && json.get("textures").isJsonObject()) {
				for (var entry : json.getAsJsonObject("textures").entrySet()) {
					if (entry.getValue().isJsonPrimitive()) {
						result.textures.putIfAbsent(entry.getKey(), entry.getValue().getAsString());
					}
				}
			}
			if (json.has("display") && json.get("display").isJsonObject()) {
				for (var entry : json.getAsJsonObject("display").entrySet()) {
					if (!result.display.has(entry.getKey())) {
						result.display.add(entry.getKey(), entry.getValue());
					}
				}
			}
			if (json.has("ambientocclusion") && result.loader == null) {
				result.ambientOcclusion = json.get("ambientocclusion").getAsBoolean();
			}
			if (json.has("render_type") && result.renderType == null) {
				result.renderType = new ResourceLocation(json.get("render_type").getAsString());
			}
			if (json.has("loader")) {
				ResourceLocation loader = new ResourceLocation(json.get("loader").getAsString());
				if (!this.loaders.containsKey(loader) && !NeoForgeModels.handles(loader) && !"neoforge:fluid_container".equals(loader.toString())) {
					return null;
				}
				result.loader = loader;
				result.json = json;
				return result;
			}
			current = json.has("parent") ? new ResourceLocation(json.get("parent").getAsString()) : null;
		}
		return null;
	}

	@Nullable
	static JsonObject json(ResourceLocation model) {
		if ("builtin".equals(model.getPath()) || model.getPath().startsWith("builtin/")) {
			return null;
		}
		var file = new ResourceLocation(model.getNamespace(), "models/" + model.getPath() + ".json");
		try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(file);
				Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
			JsonElement element = new JsonParser().parse(reader);
			return element.isJsonObject() ? element.getAsJsonObject() : null;
		} catch (Exception e) {
			return null;
		}
	}

	/** A plain deserialization context (the loaders read their own json; nested objects by Gson's defaults). */
	private static final JsonDeserializationContext CONTEXT = context();

	private static final class Marker {
	}

	private static JsonDeserializationContext context() {
		var holder = new JsonDeserializationContext[1];
		new GsonBuilder().registerTypeAdapter(Marker.class, (JsonDeserializer<Marker>) (json, type, context) -> {
			holder[0] = context;
			return new Marker();
		}).create().fromJson("{}", Marker.class);
		return holder[0];
	}

	// --- baking --------------------------------------------------------------------------------------------------------

	/** The rotation and sprites of the model being baked (the parts a mod bakes inside take them). */
	static final ThreadLocal<Baking> BAKING = new ThreadLocal<>();

	static final class Baking {
		final ITransformation rotation;
		final Function<ResourceLocation, TextureAtlasSprite> sprites;

		Baking(ITransformation rotation, Function<ResourceLocation, TextureAtlasSprite> sprites) {
			this.rotation = rotation;
			this.sprites = sprites;
		}
	}

	static ITransformation rotation(IModelState state) {
		if (state instanceof ITransformation transformation) {
			return transformation;
		}
		return state == null ? ModelRotation.X0_Y0 : state.apply(Optional.empty()).map(t -> (ITransformation) t).orElse(TRSRTransformation.identity());
	}

	private static final class GeometryModel implements IModel {
		private final ResourceLocation location;
		private final Resolved model;
		private final Object geometry;

		GeometryModel(ResourceLocation location, Resolved model, Object geometry) {
			this.location = location;
			this.model = model;
			this.geometry = geometry;
		}

		@Override
		public Collection<ResourceLocation> getDependencies() {
			return Collections.emptyList();
		}

		@Override
		public Collection<ResourceLocation> getTextures() {
			var textures = new HashSet<ResourceLocation>();
			for (String name : this.model.textures.keySet()) {
				String texture = texture(this.model.textures, name);
				if (texture != null) {
					textures.add(new ResourceLocation(texture));
				}
			}
			return textures;
		}

		@Override
		public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> sprites) {
			Baking before = BAKING.get();
			BAKING.set(new Baking(rotation(state), sprites));
			try {
				Object context = proxy("net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext", (proxy, method, args) -> this.context(method, args));
				Object baker = proxy("net.minecraft.client.resources.model.ModelBaker", (proxy, method, args) -> baker(method, args));
				Object modelState = proxy("net.minecraft.client.resources.model.ModelState", (proxy, method, args) -> modelState(method));
				Function<Object, TextureAtlasSprite> materials = material -> sprite(material);
				Method bake = method(this.geometry.getClass(), "bake", 5);
				Object baked = bake.invoke(this.geometry, context, baker, materials, modelState, ItemOverrideList.NONE);
				return baked instanceof IBakedModel model ? model : ModelLoaderRegistry.getMissingModel().bake(state, format, sprites);
			} catch (Exception | LinkageError e) {
				Throwable cause = e instanceof java.lang.reflect.InvocationTargetException invocation && invocation.getCause() != null ? invocation.getCause() : e;
				LOGGER.error("Couldn't bake the 1.21.1 model {} ({}), drawn as a plain json model", this.location, this.model.loader, cause);
				// what 1.12.2 made of the json before: its elements and parents, without the loader's extras
				try {
					return bakeJson(this.model.json, this.location.toString(), this.model.textures);
				} catch (RuntimeException | LinkageError plain) {
					return ModelLoaderRegistry.getMissingModel().bake(state, format, sprites);
				}
			} finally {
				if (before == null) {
					BAKING.remove();
				} else {
					BAKING.set(before);
				}
			}
		}

		/** IGeometryBakingContext's methods, from the json. */
		@Nullable
		private Object context(Method method, Object[] args) {
			switch (method.getName()) {
				case "getModelName":
					return this.location.toString();
				case "hasMaterial":
					return texture(this.model.textures, (String) args[0]) != null;
				case "getMaterial": {
					String texture = texture(this.model.textures, (String) args[0]);
					return material(texture == null ? "minecraft:missingno" : texture);
				}
				case "isGui3d":
				case "useBlockLight":
					return true;
				case "useAmbientOcclusion":
					return this.model.ambientOcclusion;
				case "getTransforms":
					return transforms(this.model.display);
				case "getRootTransform":
					return identity();
				case "getRenderTypeHint":
					return this.model.renderType;
				case "isComponentVisible":
					return args[1];
				default:
					return objectMethod(this, method, args);
			}
		}
	}

	/** ModelBaker's methods: other models by location, baked with the current rotation and sprites. */
	@Nullable
	private static Object baker(Method method, Object[] args) {
		switch (method.getName()) {
			case "bake":
			case "bakeUncached":
				if (args != null && args.length > 0) {
					if (args[0] instanceof ResourceLocation location) {
						return bakeLocation(location);
					}
					if (args[0] instanceof ModelBlock model) {
						return bakeModel(model);
					}
				}
				return null;
			case "getModelTextureGetter":
				return (Function<Object, TextureAtlasSprite>) GeometryLoaders::sprite;
			case "getModel":
				// an UnbakedModel of 1.21 (the mods resolve their parts' parents through it: the Translator does)
				return null;
			default:
				return objectMethod(GeometryLoaders.class, method, args);
		}
	}

	@Nullable
	private static Object modelState(Method method) {
		switch (method.getName()) {
			case "getRotation":
				return identity();
			case "isUvLocked":
				return false;
			default:
				return objectMethod(GeometryLoaders.class, method, null);
		}
	}

	private static IBakedModel bakeLocation(ResourceLocation location) {
		Baking baking = BAKING.get();
		var model = ModelLoaderRegistry.getModelOrMissing(location);
		var state = baking != null && baking.rotation instanceof IModelState modelState ? modelState : model.getDefaultState();
		return model.bake(state, DefaultVertexFormats.ITEM, baking != null ? baking.sprites : ModelLoader.defaultTextureGetter());
	}

	// --- 1.21 BlockModels on 1.12.2 ModelBlocks ---------------------------------------------------------------------------

	/** A model json of that location with its parents (null when there is none). */
	@Nullable
	public static ModelBlock load(ResourceLocation location) {
		return load(location, 0);
	}

	@Nullable
	private static ModelBlock load(ResourceLocation location, int depth) {
		JsonObject json = depth > 32 ? null : json(location);
		if (json == null) {
			return null;
		}
		try {
			ModelBlock model = ModelBlock.deserialize(json.toString());
			model.name = location.toString();
			parents(model, depth);
			return model;
		} catch (RuntimeException e) {
			LOGGER.warn("Couldn't read the model {}: {}", location, e.toString());
			return null;
		}
	}

	/** The model's parents, read (1.21 {@code resolveParents}). */
	public static void parents(ModelBlock model) {
		parents(model, 0);
	}

	private static void parents(ModelBlock model, int depth) {
		if (model.parent == null && model.getParentLocation() != null) {
			model.parent = load(model.getParentLocation(), depth + 1);
		}
	}

	/**
	 * A model of json (inline, with parents; a nested loader too): an item/generated one by 1.12.2's item model
	 * generator, the others by their elements.
	 */
	public static IBakedModel bakeJson(JsonObject json, String name, Map<String, String> inherited) {
		if (json.has("loader")) {
			ResourceLocation loader = new ResourceLocation(json.get("loader").getAsString());
			if (NeoForgeModels.handles(loader)) {
				var textures = new LinkedHashMap<String, String>();
				if (json.has("textures") && json.get("textures").isJsonObject()) {
					for (var entry : json.getAsJsonObject("textures").entrySet()) {
						textures.put(entry.getKey(), entry.getValue().getAsString());
					}
				}
				inherited.forEach(textures::putIfAbsent);
				JsonObject display = json.has("display") && json.get("display").isJsonObject() ? json.getAsJsonObject("display") : new JsonObject();
				Baking baking = BAKING.get();
				return new NeoForgeModels.Model(new ResourceLocation(name), loader, json, textures, display).bake(
						baking != null && baking.rotation instanceof IModelState state ? state : ModelRotation.X0_Y0, DefaultVertexFormats.ITEM,
						baking != null ? baking.sprites : ModelLoader.defaultTextureGetter());
			}
		}
		var copy = new JsonParser().parse(json.toString()).getAsJsonObject();
		copy.remove("loader");
		if (!inherited.isEmpty()) {
			JsonObject textures = copy.has("textures") && copy.get("textures").isJsonObject() ? copy.getAsJsonObject("textures") : new JsonObject();
			inherited.forEach((key, value) -> {
				if (!textures.has(key)) {
					textures.addProperty(key, value);
				}
			});
			copy.add("textures", textures);
		}
		ModelBlock model;
		try {
			model = ModelBlock.deserialize(copy.toString());
		} catch (RuntimeException e) {
			LOGGER.warn("Couldn't read the model part {}: {}", name, e.toString());
			return ModelLoaderRegistry.getMissingModel().bake(ModelRotation.X0_Y0, DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
		}
		model.name = name;
		parents(model);
		if (generated(model)) {
			ModelBlock items = new net.minecraft.client.renderer.block.model.ItemModelGenerator().makeItemModel(Minecraft.getMinecraft().getTextureMapBlocks(),
					model);
			if (items != null) {
				model = items;
			}
		}
		return bakeModel(model);
	}

	/** Whether the model's root is builtin/generated (an item made from its layer textures). */
	private static boolean generated(ModelBlock model) {
		for (ModelBlock current = model; current != null; current = current.parent) {
			ResourceLocation parent = current.getParentLocation();
			if (parent != null && "builtin/generated".equals(parent.getPath())) {
				return true;
			}
		}
		return false;
	}

	/** The model's elements as quads (1.12.2 ModelBakery's baking, with the current rotation and sprites). */
	public static IBakedModel bakeModel(ModelBlock model) {
		Baking baking = BAKING.get();
		ITransformation rotation = baking != null ? baking.rotation : ModelRotation.X0_Y0;
		Function<ResourceLocation, TextureAtlasSprite> sprites = baking != null ? baking.sprites : ModelLoader.defaultTextureGetter();
		parents(model);
		var builder = new SimpleBakedModel.Builder(model, ItemOverrideList.NONE);
		builder.setTexture(sprites.apply(new ResourceLocation(model.resolveTextureName("particle"))));
		var bakery = new FaceBakery();
		for (BlockPart part : model.getElements()) {
			for (Map.Entry<EnumFacing, BlockPartFace> entry : part.mapFaces.entrySet()) {
				BlockPartFace face = entry.getValue();
				TextureAtlasSprite sprite = sprites.apply(new ResourceLocation(model.resolveTextureName(face.texture)));
				BakedQuad quad = bakery.makeBakedQuad(part.positionFrom, part.positionTo, face, sprite, entry.getKey(), rotation, part.partRotation, false,
						part.shade);
				if (face.cullFace == null) {
					builder.addGeneralQuad(quad);
				} else {
					builder.addFaceQuad(rotation.rotate(face.cullFace), quad);
				}
			}
		}
		return builder.makeBakedModel();
	}

	// --- the carried 1.21 types ---------------------------------------------------------------------------------------

	private static Object proxy(String carried, java.lang.reflect.InvocationHandler handler) throws ClassNotFoundException {
		Class<?> type = Class.forName(carried, true, net.minecraft.launchwrapper.Launch.classLoader);
		return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
	}

	@Nullable
	private static Object objectMethod(Object self, Method method, @Nullable Object[] args) {
		switch (method.getName()) {
			case "toString":
				return "Translator " + method.getDeclaringClass().getSimpleName();
			case "hashCode":
				return System.identityHashCode(self);
			case "equals":
				return args != null && args.length == 1 && args[0] == self;
			default:
				Class<?> type = method.getReturnType();
				if (type == boolean.class) {
					return false;
				}
				if (type == int.class || type == long.class || type == float.class || type == double.class || type == short.class || type == byte.class
						|| type == char.class) {
					return 0;
				}
				return null;
		}
	}

	/** A texture of the model's map: "#name" references followed, null when it has none. */
	@Nullable
	static String texture(Map<String, String> textures, String name) {
		String value = textures.get(name.startsWith("#") ? name.substring(1) : name);
		for (int i = 0; value != null && value.startsWith("#") && i < 16; i++) {
			value = textures.get(value.substring(1));
		}
		return value;
	}

	/** A carried 1.21 Material of the blocks atlas. */
	@Nullable
	static Object material(String texture) {
		try {
			Class<?> type = Class.forName("net.minecraft.client.resources.model.Material", true, net.minecraft.launchwrapper.Launch.classLoader);
			return type.getConstructor(ResourceLocation.class, ResourceLocation.class).newInstance(TextureMap.LOCATION_BLOCKS_TEXTURE,
					new ResourceLocation(texture));
		} catch (ReflectiveOperationException | LinkageError e) {
			return null;
		}
	}

	/** The texture of a carried Material (or of a location), as its atlas sprite. */
	static TextureAtlasSprite sprite(@Nullable Object material) {
		ResourceLocation texture = null;
		if (material instanceof ResourceLocation location) {
			texture = location;
		} else if (material != null) {
			try {
				Object value = material.getClass().getMethod("texture").invoke(material);
				texture = value instanceof ResourceLocation location ? location : null;
			} catch (ReflectiveOperationException | LinkageError e) {
				texture = null;
			}
		}
		Baking baking = BAKING.get();
		if (texture == null) {
			return Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
		}
		return baking != null ? baking.sprites.apply(texture) : Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(texture.toString());
	}

	/** The texture name of a 1.21 {@code Either<Material, String>} texture entry ("#name" for a reference). */
	@Nullable
	public static String textureName(@Nullable Object entry) {
		if (entry instanceof com.mojang.datafixers.util.Either<?, ?> either) {
			var left = either.left();
			if (left.isPresent()) {
				Object material = left.get();
				try {
					return String.valueOf(material.getClass().getMethod("texture").invoke(material));
				} catch (ReflectiveOperationException | LinkageError e) {
					return null;
				}
			}
			return either.right().map(reference -> "#" + reference).orElse(null);
		}
		return entry == null ? null : String.valueOf(entry);
	}

	static ItemCameraTransforms transforms(JsonObject display) {
		if (display.size() == 0) {
			return ItemCameraTransforms.DEFAULT;
		}
		var json = new JsonObject();
		json.add("display", display);
		try {
			return ModelBlock.deserialize(json.toString()).getAllTransforms();
		} catch (RuntimeException e) {
			return ItemCameraTransforms.DEFAULT;
		}
	}

	@Nullable
	private static Object identity() {
		try {
			Class<?> type = Class.forName("com.mojang.math.Transformation", true, net.minecraft.launchwrapper.Launch.classLoader);
			return type.getMethod("identity").invoke(null);
		} catch (ReflectiveOperationException | LinkageError e) {
			return null;
		}
	}

	private static Method method(Class<?> type, String name, int parameters) throws NoSuchMethodException {
		for (Class<?> current = type; current != null; current = current.getSuperclass()) {
			for (Method method : current.getDeclaredMethods()) {
				if (method.getName().equals(name) && method.getParameterCount() == parameters && !method.isBridge()) {
					method.setAccessible(true);
					return method;
				}
			}
		}
		for (Method method : type.getMethods()) {
			if (method.getName().equals(name) && method.getParameterCount() == parameters) {
				method.setAccessible(true);
				return method;
			}
		}
		throw new NoSuchMethodException(type.getName() + "." + name);
	}

	private static final Set<Class<?>> REPORTED = ConcurrentHashMap.newKeySet();

	/** Whether this model class's failure is the first one (the rest aren't logged). */
	static boolean reportOnce(Class<?> type) {
		return REPORTED.add(type);
	}

	/** The loaders by id (for the self-test). */
	public Set<ResourceLocation> ids() {
		return Collections.unmodifiableSet(this.loaders.keySet());
	}
}
