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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import io.github.xponer2.translator.client.Camera;
import io.github.xponer2.translator.client.VertexConsumer;

/**
 * The parent of the 1.21 particles (S row of Particle: the carried vanilla ones and the mods' own). 1.12.2 draws a
 * particle with its renderParticle into the batch of its layer; a 1.21 particle draws itself with
 * {@code render(consumer, camera, partialTick)}: called here with a consumer writing into that batch and a camera at
 * 1.12.2's particle origin. Its sprites are in the blocks atlas (layer 1); a custom one draws on its own (layer 3).
 */
@SideOnly(Side.CLIENT)
public abstract class TranslatedParticle extends Particle {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator Runtime");

	/** The particle classes whose render failed (logged once each). */
	private static final java.util.Set<Class<?>> FAILED_CLASSES = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

	/** The 1.21 render of a particle class (render, or renderParticle when the table renamed it). */
	private static final ClassValue<MethodHandle> RENDER = new ClassValue<>() {
		@Override
		protected MethodHandle computeValue(Class<?> type) {
			MethodType wanted = MethodType.methodType(void.class, VertexConsumer.class, Camera.class, float.class);
			for (String name : new String[] {"render", "renderParticle"}) {
				try {
					Method method = find(type, name, VertexConsumer.class, Camera.class, float.class);
					if (method != null) {
						method.setAccessible(true);
						return MethodHandles.lookup().unreflect(method).asType(wanted.insertParameterTypes(0, TranslatedParticle.class));
					}
				} catch (IllegalAccessException | RuntimeException e) {
					// the other name
				}
			}
			return null;
		}
	};

	/** The name of its 1.21 render type (TERRAIN_SHEET, PARTICLE_SHEET_OPAQUE, CUSTOM, NO_RENDER, a mod's own). */
	private static final ClassValue<Method> RENDER_TYPE = new ClassValue<>() {
		@Override
		protected Method computeValue(Class<?> type) {
			Method method = find(type, "getRenderType");
			if (method != null) {
				method.setAccessible(true);
			}
			return method;
		}
	};

	@Nullable
	private static Method find(Class<?> type, String name, Class<?>... parameters) {
		for (Class<?> c = type; c != null && c != Particle.class; c = c.getSuperclass()) {
			try {
				Method method = c.getDeclaredMethod(name, parameters);
				if (!java.lang.reflect.Modifier.isAbstract(method.getModifiers())) {
					return method;
				}
			} catch (NoSuchMethodException e) {
				// its parent's
			}
		}
		return null;
	}

	private boolean failed;
	/** Its render type's name, asked once (the layer it is filed under can't change after it is added). */
	@Nullable
	private String renderType;

	protected TranslatedParticle(World level, double x, double y, double z) {
		super(level, x, y, z);
	}

	protected TranslatedParticle(WorldClient level, double x, double y, double z) {
		super(level, x, y, z);
	}

	public TranslatedParticle(World level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		super(level, x, y, z, xSpeed, ySpeed, zSpeed);
	}

	public TranslatedParticle(WorldClient level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		super(level, x, y, z, xSpeed, ySpeed, zSpeed);
	}

	private String renderTypeName() {
		if (this.renderType == null) {
			this.renderType = this.askRenderType();
		}
		return this.renderType;
	}

	private String askRenderType() {
		Method method = RENDER_TYPE.get(this.getClass());
		if (method == null) {
			return "";
		}
		try {
			Object type = method.invoke(this);
			return type == null ? "NO_RENDER" : type.toString();
		} catch (ReflectiveOperationException | RuntimeException e) {
			return "";
		}
	}

	/** Its sheet in the blocks atlas (1.12.2's terrain layer), a custom one on its own. */
	@Override
	public int getFXLayer() {
		String type = this.renderTypeName();
		return "CUSTOM".equals(type) || "NO_RENDER".equals(type) ? 3 : 1;
	}

	@Override
	public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rotationX, float rotationZ, float rotationYZ,
			float rotationXY, float rotationXZ) {
		MethodHandle render = RENDER.get(this.getClass());
		if (render == null) {
			super.renderParticle(buffer, entity, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
			return;
		}
		if (this.failed || "NO_RENDER".equals(this.renderTypeName())) {
			return;
		}
		// 1.12.2 draws particles relative to the view entity's position (interpPos): the camera of the render is there
		Camera camera = Camera.at(interpPosX, interpPosY, interpPosZ);
		boolean own = this.getFXLayer() == 3;
		BufferBuilder target = buffer;
		if (own) {
			Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			target = Tessellator.getInstance().getBuffer();
			target.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
		}
		io.github.xponer2.translator.client.BufferVertexConsumer consumer = VertexConsumer.raw(target);
		try {
			// the handle's exact type: the consumer as the 1.21 VertexConsumer (as BufferVertexConsumer every particle of
			// the mods failed with WrongMethodTypeException, and logged it each frame: the game froze on its log)
			render.invokeExact(this, (VertexConsumer) consumer, camera, partialTicks);
		} catch (Throwable e) {
			this.failed = true;
			// once per particle class: a failing particle is made again and again
			if (FAILED_CLASSES.add(this.getClass())) {
				LOGGER.warn("A particle of {} isn't drawn", this.getClass().getName(), e);
			}
		} finally {
			consumer.endVertices();
			if (own) {
				Tessellator.getInstance().draw();
			}
		}
	}
}
