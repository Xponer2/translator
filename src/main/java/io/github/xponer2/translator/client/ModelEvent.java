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
package io.github.xponer2.translator.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.github.xponer2.translator.Translator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * NeoForge model events on the 1.12.2 ones: {@link RegisterAdditional} (standalone models, baked by the Translator
 * under {@code <id>#standalone}) and {@link ModifyBakingResult} (the baked model registry, after 1.12.2 baked it).
 * Posted by {@link Forwarder}, registered on the client.
 */
@SideOnly(Side.CLIENT)
public class ModelEvent extends Event {
	public static final String STANDALONE = "standalone";

	public static ModelResourceLocation standalone(ResourceLocation id) {
		return new ModelResourceLocation(id, STANDALONE);
	}

	public static ModelResourceLocation inventory(ResourceLocation id) {
		return new ModelResourceLocation(id, "inventory");
	}

	public static class RegisterAdditional extends ModelEvent {
		private final List<ResourceLocation> models;

		RegisterAdditional(List<ResourceLocation> models) {
			this.models = models;
		}

		public void register(ModelResourceLocation location) {
			this.models.add(new ResourceLocation(location.getNamespace(), location.getPath()));
		}

		public void register(ResourceLocation location) {
			this.models.add(location);
		}
	}

	public static class ModifyBakingResult extends ModelEvent {
		private final Models models;

		ModifyBakingResult(IRegistry<ModelResourceLocation, IBakedModel> registry) {
			this.models = new Models(registry);
		}

		public Models getModels() {
			return this.models;
		}
	}

	/** The baked models by location (1.21 Map<ModelResourceLocation, BakedModel>), backed by the 1.12.2 registry. */
	public static final class Models extends java.util.AbstractMap<ModelResourceLocation, IBakedModel> {
		private final IRegistry<ModelResourceLocation, IBakedModel> registry;

		Models(IRegistry<ModelResourceLocation, IBakedModel> registry) {
			this.registry = registry;
		}

		@Override
		@Nullable
		public IBakedModel get(Object location) {
			return location instanceof ModelResourceLocation mrl ? this.registry.getObject(mrl) : null;
		}

		@Override
		public IBakedModel put(ModelResourceLocation location, IBakedModel model) {
			var old = this.registry.getObject(location);
			this.registry.putObject(location, model);
			return old;
		}

		@Override
		public boolean containsKey(Object location) {
			return this.get(location) != null;
		}

		@Override
		public java.util.Set<Entry<ModelResourceLocation, IBakedModel>> entrySet() {
			var entries = new java.util.LinkedHashSet<Entry<ModelResourceLocation, IBakedModel>>();
			for (var key : this.registry.getKeys()) {
				entries.add(new java.util.AbstractMap.SimpleEntry<>(key, this.registry.getObject(key)));
			}
			return entries;
		}
	}

	public static final class Forwarder {
		private final List<ResourceLocation> standalone = new ArrayList<>();

		@SubscribeEvent(priority = EventPriority.LOW)
		public void onModelRegistry(ModelRegistryEvent event) {
			this.standalone.clear();
			MinecraftForge.EVENT_BUS.post(new RegisterAdditional(this.standalone));
		}

		@SubscribeEvent(priority = EventPriority.HIGH)
		public void onModelBake(ModelBakeEvent event) {
			var registry = event.getModelRegistry();
			for (var id : this.standalone) {
				try {
					var model = ModelLoaderRegistry.getModel(id);
					var baked = model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
					registry.putObject(standalone(id), baked);
				} catch (Exception e) {
					Translator.LOGGER.error("Couldn't bake standalone model {}", id, e);
				}
			}
			MinecraftForge.EVENT_BUS.post(new ModifyBakingResult(registry));
		}
	}
}
