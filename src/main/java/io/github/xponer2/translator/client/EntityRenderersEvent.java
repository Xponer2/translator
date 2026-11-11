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

import io.github.xponer2.translator.registry.BlockEntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * NeoForge renderer registration, posted by the Translator on the client in init (the block entity types exist then).
 */
@SideOnly(Side.CLIENT)
public class EntityRenderersEvent extends Event {
	/** NeoForge: the mods' entity model layers (posted before the renderers, which bake them). */
	public static class RegisterLayerDefinitions extends EntityRenderersEvent {
		public void registerLayerDefinition(Object location, java.util.function.Supplier<?> definition) {
			io.github.xponer2.translator.client.model.EntityModelSet.register(location, definition);
		}
	}

	/** NeoForge: layers added to other mods' and vanilla renderers; 1.12.2's renderers take no 1.21 layers. */
	public static class AddLayers extends EntityRenderersEvent {
		public java.util.Set<Object> getSkins() {
			return java.util.Collections.emptySet();
		}

		public java.util.Set<Object> getEntityTypes() {
			return java.util.Collections.emptySet();
		}

		/**
		 * 1.21 {@code event.getRenderer(type)}: the renderer of an entity type, which mods add their layers to (armor
		 * and items on mobs: Mekanism and 6 more mods); null when 1.12.2 has none for its class.
		 */
		@javax.annotation.Nullable
		public <R> R getRenderer(Object type) {
			// a type variable result: the converter casts it to the renderer class the mod expects (an Object result
			// wasn't linked, so the layers weren't added at all)
			if (!(type instanceof io.github.xponer2.translator.registry.EntityType<?> entityType)) {
				return null;
			}
			Class<?> entityClass = entityType.getEntityClass();
			if (entityClass == null || entityClass == net.minecraft.entity.Entity.class) {
				return null;
			}
			var renderers = net.minecraft.client.Minecraft.getMinecraft().getRenderManager().entityRenderMap;
			for (Class<?> c = entityClass; c != null && c != net.minecraft.entity.Entity.class; c = c.getSuperclass()) {
				var renderer = renderers.get(c);
				if (renderer != null) {
					return io.github.xponer2.translator.client.entity.VanillaRendererView.of(renderer);
				}
			}
			return null;
		}

		/** 1.21 {@code event.getContext()}: what renderers are made with (Mekanism builds its layers' models from it). */
		public io.github.xponer2.translator.client.entity.RendererContext getContext() {
			return new io.github.xponer2.translator.client.entity.RendererContext();
		}

		/**
		 * 1.21 {@code event.getSkin(model)}: the player renderer of that skin model ("default" or "slim" in 1.12.2),
		 * which mods add their layers to (8 mods' capes, wings, armor parts).
		 */
		@javax.annotation.Nullable
		public <R> R getSkin(Object model) {
			// the 1.12.2 player renderer as a 1.21 one: mods cast it to LivingEntityRenderer (a 1.12.2 one threw)
			String name = model instanceof Enum<?> constant && "SLIM".equals(constant.name()) || "slim".equals(String.valueOf(model)) ? "slim" : "default";
			return io.github.xponer2.translator.client.entity.VanillaRendererView.of(
					net.minecraft.client.Minecraft.getMinecraft().getRenderManager().getSkinMap().get(name));
		}

		public io.github.xponer2.translator.client.model.EntityModelSet getEntityModels() {
			return io.github.xponer2.translator.client.model.EntityModelSet.INSTANCE;
		}
	}

	/** NeoForge: skull models of the mods' skull types (1.12.2's skulls are its own). */
	public static class CreateSkullModels extends EntityRenderersEvent {
		public io.github.xponer2.translator.client.model.EntityModelSet getEntityModelSet() {
			return io.github.xponer2.translator.client.model.EntityModelSet.INSTANCE;
		}

		public void registerSkullModel(Object type, Object model) {
		}
	}

	public static class RegisterRenderers extends EntityRenderersEvent {
		public <T extends TileEntity> void registerBlockEntityRenderer(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> provider) {
			TesrAdapter.register(type, provider);
		}

		/** An entity type's renderer from the mod's provider (see EntityRenderers). */
		public void registerEntityRenderer(io.github.xponer2.translator.registry.EntityType<?> type, Object provider) {
			try {
				EntityRenderers.register(type, provider);
			} catch (Throwable t) {
				io.github.xponer2.translator.runtime.RuntimeStubs.failed("the renderer of " + type, t);
			}
		}
	}
}
