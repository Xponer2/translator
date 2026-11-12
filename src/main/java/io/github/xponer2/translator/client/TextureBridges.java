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

import javax.annotation.Nullable;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 texture and model data calls 1.12.2's classes lack. {@code texture.bind()}: a mod binds its own texture before it
 * draws with it (JourneyMap's map tiles: the call was left out and every tile drew with whatever was bound last).
 */
@SideOnly(Side.CLIENT)
public final class TextureBridges {
	private TextureBridges() {
	}

	/**
	 * 1.21 {@code AbstractTexture.bind()}: the texture's own bind when its class has one, else its id bound. 1.12.2's
	 * AbstractTexture has no bind, so a call of it is this static method, and a mod's texture that uploads in its own
	 * bind (JourneyMap's LOD map tiles) was never uploaded: the zoomed out map and the minimap drew white or nothing.
	 * Within that own bind (its super.bind(), also this method) the id is bound.
	 */
	public static void bind(AbstractTexture texture) {
		java.lang.reflect.Method own = OWN_BIND.get(texture.getClass());
		if (own != null) {
			var active = BINDING.get();
			if (active.add(texture)) {
				try {
					own.invoke(texture);
					return;
				} catch (java.lang.reflect.InvocationTargetException e) {
					io.github.xponer2.translator.runtime.RuntimeStubs.failed(texture.getClass().getName() + ".bind", e.getCause());
					return;
				} catch (ReflectiveOperationException | RuntimeException e) {
					// not callable: the id below
				} finally {
					active.remove(texture);
				}
			}
		}
		GlStateManager.bindTexture(texture.getGlTextureId());
	}

	private static final ThreadLocal<java.util.Set<AbstractTexture>> BINDING = ThreadLocal
			.withInitial(() -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

	/** A texture class's own public bind(), declared by a mod's class (not 1.12.2's or the Translator's). */
	private static final ClassValue<java.lang.reflect.Method> OWN_BIND = new ClassValue<java.lang.reflect.Method>() {
		@Override
		@Nullable
		protected java.lang.reflect.Method computeValue(Class<?> type) {
			try {
				java.lang.reflect.Method method = type.getMethod("bind");
				String owner = method.getDeclaringClass().getName();
				if (method.getReturnType() != void.class || owner.startsWith("net.minecraft.") || owner.startsWith("io.github.xponer2.translator.")) {
					return null;
				}
				method.setAccessible(true);
				return method;
			} catch (NoSuchMethodException | RuntimeException e) {
				return null;
			}
		}
	};

	/** 1.21 {@code AbstractTexture.getId()}. */
	public static int getId(AbstractTexture texture) {
		return texture.getGlTextureId();
	}

	/** 1.21 {@code AbstractTexture.releaseId()}. */
	public static void releaseId(AbstractTexture texture) {
		texture.deleteGlTexture();
	}

	/**
	 * NeoForge {@code level.getModelDataManager().getAt(pos)}: what the block entity there tells its block's model, empty
	 * when there is none. 1.12.2 has no manager (the receiver is whatever the call before gave, also null).
	 */
	public static ModelData getAt(@Nullable Object manager, @Nullable BlockPos pos) {
		var world = net.minecraft.client.Minecraft.getMinecraft().world;
		if (world == null || pos == null || !world.isBlockLoaded(pos)) {
			return ModelData.EMPTY;
		}
		try {
			TileEntity tile = world.getTileEntity(pos);
			if (tile instanceof io.github.xponer2.translator.block.BlockEntity entity) {
				ModelData data = entity.getModelData();
				return data == null ? ModelData.EMPTY : data;
			}
		} catch (RuntimeException e) {
			// asked off the client thread while the chunk changes: nothing known
		}
		return ModelData.EMPTY;
	}
}
