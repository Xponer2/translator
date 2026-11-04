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

		@javax.annotation.Nullable
		public Object getRenderer(Object type) {
			return null;
		}

		@javax.annotation.Nullable
		public Object getSkin(Object model) {
			return null;
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
