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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * NeoForge's own model loaders on 1.12.2: {@code neoforge:composite} (children baked together),
 * {@code neoforge:separate_transforms} (another model per display context) and {@code neoforge:obj} (Forge's OBJ
 * loader). Mekanism, Create, Twilight Forest, Artifacts, Iron's Spellbooks and Supplementaries make ~190 models so.
 * Models of the other NeoForge loaders (item_layers, fluid_container) read as plain json models, as before.
 */
@SideOnly(Side.CLIENT)
final class NeoForgeModels {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator");

	private NeoForgeModels() {
	}

	static boolean handles(ResourceLocation loader) {
		if (!"neoforge".equals(loader.getNamespace()) && !"forge".equals(loader.getNamespace())) {
			return false;
		}
		switch (loader.getPath()) {
			case "composite":
			case "separate_transforms":
			case "separate-perspective":
			case "obj":
				return true;
			default:
				return false;
		}
	}

	static final class Model implements IModel {
		private final ResourceLocation location;
		private final ResourceLocation loader;
		private final JsonObject json;
		private final Map<String, String> textures;
		private final JsonObject display;

		Model(ResourceLocation location, ResourceLocation loader, JsonObject json, Map<String, String> textures, JsonObject display) {
			this.location = location;
			this.loader = loader;
			this.json = json;
			this.textures = textures;
			this.display = display;
		}

		@Override
		public Collection<ResourceLocation> getDependencies() {
			return Collections.emptyList();
		}

		@Override
		public Collection<ResourceLocation> getTextures() {
			var out = new HashSet<ResourceLocation>();
			for (String name : this.textures.keySet()) {
				String texture = GeometryLoaders.texture(this.textures, name);
				if (texture != null) {
					out.add(new ResourceLocation(texture));
				}
			}
			return out;
		}

		@Override
		public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> sprites) {
			var before = GeometryLoaders.BAKING.get();
			GeometryLoaders.BAKING.set(new GeometryLoaders.Baking(GeometryLoaders.rotation(state), sprites));
			try {
				switch (this.loader.getPath()) {
					case "composite":
						return this.composite(sprites);
					case "obj":
						return this.obj(state, format, sprites);
					default:
						return this.separate();
				}
			} catch (Exception | LinkageError e) {
				LOGGER.error("Couldn't bake the 1.21.1 model {} ({})", this.location, this.loader, e);
				return ModelLoaderRegistry.getMissingModel().bake(state, format, sprites);
			} finally {
				if (before == null) {
					GeometryLoaders.BAKING.remove();
				} else {
					GeometryLoaders.BAKING.set(before);
				}
			}
		}

		private TextureAtlasSprite particle(Function<ResourceLocation, TextureAtlasSprite> sprites, @Nullable IBakedModel fallback) {
			String particle = GeometryLoaders.texture(this.textures, "particle");
			if (particle != null) {
				return sprites.apply(new ResourceLocation(particle));
			}
			return fallback != null ? fallback.getParticleTexture() : sprites.apply(new ResourceLocation("minecraft:missingno"));
		}

		private boolean flag(String name, boolean fallback) {
			return this.json.has(name) ? this.json.get(name).getAsBoolean() : fallback;
		}

		/** {@code "children": {name: model}}: every child baked with the composite's textures, quads together. */
		private IBakedModel composite(Function<ResourceLocation, TextureAtlasSprite> sprites) {
			var children = new ArrayList<IBakedModel>();
			if (this.json.has("children") && this.json.get("children").isJsonObject()) {
				for (var entry : this.json.getAsJsonObject("children").entrySet()) {
					if (entry.getValue().isJsonObject()) {
						children.add(GeometryLoaders.bakeJson(entry.getValue().getAsJsonObject(), this.location + "#" + entry.getKey(), this.textures));
					}
				}
			}
			IBakedModel first = children.isEmpty() ? null : children.get(0);
			return new Composite(children, GeometryLoaders.transforms(this.display), this.particle(sprites, first), this.flag("ambientocclusion", true),
					this.flag("gui_light", true));
		}

		/** {@code "base"} and {@code "perspectives": {context: model}}. */
		private IBakedModel separate() {
			IBakedModel base = this.json.has("base") && this.json.get("base").isJsonObject()
					? GeometryLoaders.bakeJson(this.json.getAsJsonObject("base"), this.location + "#base", this.textures)
					: ModelLoaderRegistry.getMissingModel().bake(net.minecraft.client.renderer.block.model.ModelRotation.X0_Y0,
							net.minecraft.client.renderer.vertex.DefaultVertexFormats.ITEM, net.minecraftforge.client.model.ModelLoader.defaultTextureGetter());
			var perspectives = new EnumMap<TransformType, IBakedModel>(TransformType.class);
			if (this.json.has("perspectives") && this.json.get("perspectives").isJsonObject()) {
				for (var entry : this.json.getAsJsonObject("perspectives").entrySet()) {
					TransformType type = context(entry.getKey());
					if (type != null && entry.getValue().isJsonObject()) {
						perspectives.put(type, GeometryLoaders.bakeJson(entry.getValue().getAsJsonObject(), this.location + "#" + entry.getKey(), this.textures));
					}
				}
			}
			return new Separate(base, perspectives);
		}

		/** {@code "model": "ns:models/x.obj"}: Forge's OBJ loader, its materials given the json's textures. */
		private IBakedModel obj(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> sprites) throws Exception {
			var file = new ResourceLocation(this.json.get("model").getAsString());
			// Forge's OBJ loader reads the file by its path ("ns:models/block/x.obj")
			var location = file.getPath().startsWith("models/") ? file : new ResourceLocation(file.getNamespace(), "models/" + file.getPath());
			OBJLoader.INSTANCE.addDomain(file.getNamespace());
			IModel model = OBJLoader.INSTANCE.loadModel(location);
			model = model.process(ImmutableMap.of("flip-v", String.valueOf(this.flag("flip_v", false))));
			var retexture = ImmutableMap.<String, String>builder();
			for (String name : this.textures.keySet()) {
				String texture = GeometryLoaders.texture(this.textures, name);
				if (texture != null) {
					retexture.put("#" + name, texture);
				}
			}
			model = model.retexture(retexture.build());
			IBakedModel baked = model.bake(state, format, sprites);
			if (this.display.size() == 0) {
				return baked;
			}
			// the json's display (OBJ files have none)
			return new Separate(new Composite(Collections.singletonList(baked), GeometryLoaders.transforms(this.display), baked.getParticleTexture(),
					baked.isAmbientOcclusion(), baked.isGui3d()), new EnumMap<>(TransformType.class));
		}
	}

	@Nullable
	static TransformType context(String name) {
		switch (name) {
			case "none":
				return TransformType.NONE;
			case "thirdperson_lefthand":
				return TransformType.THIRD_PERSON_LEFT_HAND;
			case "thirdperson_righthand":
			case "thirdperson":
				return TransformType.THIRD_PERSON_RIGHT_HAND;
			case "firstperson_lefthand":
				return TransformType.FIRST_PERSON_LEFT_HAND;
			case "firstperson_righthand":
			case "firstperson":
				return TransformType.FIRST_PERSON_RIGHT_HAND;
			case "head":
				return TransformType.HEAD;
			case "gui":
				return TransformType.GUI;
			case "ground":
				return TransformType.GROUND;
			case "fixed":
				return TransformType.FIXED;
			default:
				return null;
		}
	}

	/** Children drawn together. */
	static final class Composite implements IBakedModel {
		private final List<IBakedModel> children;
		private final ItemCameraTransforms transforms;
		private final TextureAtlasSprite particle;
		private final boolean ambientOcclusion;
		private final boolean gui3d;

		Composite(List<IBakedModel> children, ItemCameraTransforms transforms, TextureAtlasSprite particle, boolean ambientOcclusion, boolean gui3d) {
			this.children = children;
			this.transforms = transforms;
			this.particle = particle;
			this.ambientOcclusion = ambientOcclusion;
			this.gui3d = gui3d;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
			if (this.children.size() == 1) {
				return this.children.get(0).getQuads(state, side, rand);
			}
			var quads = new ArrayList<BakedQuad>();
			for (IBakedModel child : this.children) {
				quads.addAll(child.getQuads(state, side, rand));
			}
			return quads;
		}

		@Override
		public boolean isAmbientOcclusion() {
			return this.ambientOcclusion;
		}

		@Override
		public boolean isGui3d() {
			return this.gui3d;
		}

		@Override
		public boolean isBuiltInRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return this.particle;
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return this.transforms;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return ItemOverrideList.NONE;
		}
	}

	/** The base model, another one in some display contexts (a 3D item in the hand, a flat icon in the inventory). */
	static final class Separate implements IBakedModel {
		private final IBakedModel base;
		private final Map<TransformType, IBakedModel> perspectives;

		Separate(IBakedModel base, Map<TransformType, IBakedModel> perspectives) {
			this.base = base;
			this.perspectives = perspectives;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
			return this.base.getQuads(state, side, rand);
		}

		@Override
		public boolean isAmbientOcclusion() {
			return this.base.isAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return this.base.isGui3d();
		}

		@Override
		public boolean isBuiltInRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return this.base.getParticleTexture();
		}

		@Override
		public ItemCameraTransforms getItemCameraTransforms() {
			return this.base.getItemCameraTransforms();
		}

		@Override
		public ItemOverrideList getOverrides() {
			return this.base.getOverrides();
		}

		@Override
		public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type) {
			IBakedModel model = this.perspectives.get(type);
			return model != null ? model.handlePerspective(type) : this.base.handlePerspective(type);
		}
	}
}
