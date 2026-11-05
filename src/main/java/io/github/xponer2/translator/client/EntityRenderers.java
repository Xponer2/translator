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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.EntityType;
import io.github.xponer2.translator.runtime.RuntimeStubs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The renderers of the mods' entity types ({@code event.registerEntityRenderer(type, Renderer::new)}): the mod's
 * provider makes its renderer from a 1.21 renderer context (built from the 1.12.2 render managers it names), and the
 * renderer is the type's 1.12.2 renderer. A provider that fails gives a renderer that draws nothing, so the entity
 * doesn't stop the game.
 */
@SideOnly(Side.CLIENT)
public final class EntityRenderers {
	private EntityRenderers() {
	}

	public static void register(EntityType<?> type, Object provider) {
		Class<? extends Entity> entityClass = type.getEntityClass();
		if (entityClass == null || entityClass == Entity.class) {
			return;
		}
		RenderManager manager = Minecraft.getMinecraft().getRenderManager();
		// a mod's boat: 1.12.2's boat renderer with the wood's texture (the 1.21 boat renderer can't be made here: the
		// boats were invisible); the boat model and its texture layout are the same in both versions
		Render<? extends Entity> renderer = io.github.xponer2.translator.runtime.mc.Boat.class.isAssignableFrom(entityClass)
				? new ModBoatRenderer(manager)
				: create(provider, manager, type);
		if (manager != null) {
			manager.entityRenderMap.put(entityClass, renderer);
		}
	}

	private static Render<? extends Entity> create(Object provider, RenderManager manager, EntityType<?> type) {
		try {
			Method create = null;
			for (Method method : provider.getClass().getMethods()) {
				if ("create".equals(method.getName()) && method.getParameterCount() == 1) {
					create = method;
					break;
				}
			}
			if (create != null) {
				create.setAccessible(true);
				Object renderer = create.invoke(provider, context(create.getParameterTypes()[0]));
				if (renderer instanceof Render<?> render) {
					return render;
				}
			}
		} catch (Throwable t) {
			RuntimeStubs.failed("the renderer of " + type, t instanceof java.lang.reflect.InvocationTargetException ? t.getCause() : t);
		}
		return new Nothing(manager);
	}

	/** A 1.21 EntityRendererProvider.Context: each parameter the 1.12.2 object of its type (null where 1.12.2 has none). */
	@Nullable
	private static Object context(Class<?> type) throws ReflectiveOperationException {
		if (type == io.github.xponer2.translator.client.entity.RendererContext.class) {
			return new io.github.xponer2.translator.client.entity.RendererContext();
		}
		if (type == Object.class || type.isInterface()) {
			return null;
		}
		Minecraft mc = Minecraft.getMinecraft();
		Constructor<?> constructor = type.getDeclaredConstructors()[0];
		Class<?>[] parameters = constructor.getParameterTypes();
		Object[] args = new Object[parameters.length];
		Object[] known = {mc.getRenderManager(), mc.getRenderItem(), mc.getBlockRendererDispatcher(), mc.getItemRenderer(), mc.fontRenderer,
				mc.getResourceManager(), mc.getTextureManager(), io.github.xponer2.translator.client.model.EntityModelSet.INSTANCE};
		for (int i = 0; i < parameters.length; i++) {
			for (Object value : known) {
				if (value != null && parameters[i].isInstance(value)) {
					args[i] = value;
					break;
				}
			}
		}
		constructor.setAccessible(true);
		return constructor.newInstance(args);
	}

	/** Draws nothing (an entity whose 1.21 renderer can't be made). */
	private static final class Nothing extends Render<Entity> {
		Nothing(RenderManager manager) {
			super(manager);
		}

		@Override
		public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
		}

		@Nullable
		@Override
		protected ResourceLocation getEntityTexture(Entity entity) {
			return null;
		}
	}
}
