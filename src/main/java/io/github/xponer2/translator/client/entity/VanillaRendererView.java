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
package io.github.xponer2.translator.client.entity;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import io.github.xponer2.translator.client.MultiBufferSource;
import io.github.xponer2.translator.client.PoseStack;
import io.github.xponer2.translator.client.model.EntityModel;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A 1.12.2 living entity renderer (the player's, a vanilla mob's) as the 1.21 {@code LivingEntityRenderer} mods add
 * their layers to ({@code EntityRenderersEvent.AddLayers}: Mekanism's jetpack, worn curios, wings and capes). The
 * layers are drawn by one 1.12.2 layer added to the renderer, in the model space 1.12.2 draws its own layers in (the
 * same as 1.21's). The 1.12.2 model is no 1.21 one: a layer that poses itself after it fails on its first frame and is
 * left out (logged once).
 */
@SideOnly(Side.CLIENT)
public final class VanillaRendererView<T extends EntityLivingBase, M extends EntityModel<T>> extends LivingEntityRenderer<T, M> {
	private static final Map<Render<?>, VanillaRendererView<?, ?>> VIEWS = Collections.synchronizedMap(new IdentityHashMap<>());

	private final RenderLivingBase<T> vanilla;
	private final Set<RenderLayer<T, M>> failed = Collections.newSetFromMap(new IdentityHashMap<>());

	private VanillaRendererView(RenderLivingBase<T> vanilla) {
		super(new RendererContext(), null, 0.0F);
		this.vanilla = vanilla;
		vanilla.addLayer(new Layers());
	}

	/**
	 * The 1.21 renderer mods add layers to: a mod's own renderer as it is, a 1.12.2 living one as a view of it; null
	 * for anything else (a mod checks for null, as 1.21 gives it for an entity type with no living renderer).
	 */
	@Nullable
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <R> R of(@Nullable Render<?> renderer) {
		if (renderer instanceof LivingEntityRenderer) {
			return (R) renderer;
		}
		if (!(renderer instanceof RenderLivingBase<?> living)) {
			return null;
		}
		return (R) VIEWS.computeIfAbsent(renderer, key -> new VanillaRendererView(living));
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		if (ENTITY_TEXTURE != null) {
			try {
				return (ResourceLocation) ENTITY_TEXTURE.invoke(this.vanilla, entity);
			} catch (ReflectiveOperationException | RuntimeException e) {
				// no texture: the layer draws its own
			}
		}
		return null;
	}

	@Nullable
	private static final Method ENTITY_TEXTURE = entityTexture();

	@Nullable
	private static Method entityTexture() {
		for (String name : new String[] {"func_110775_a", "getEntityTexture"}) {
			try {
				Method method = Render.class.getDeclaredMethod(name, net.minecraft.entity.Entity.class);
				method.setAccessible(true);
				return method;
			} catch (NoSuchMethodException | RuntimeException e) {
				// the other name (the game's or the development one)
			}
		}
		return null;
	}

	/** The 1.12.2 layer that draws the mods' layers of this renderer. */
	private final class Layers implements LayerRenderer<T> {
		@Override
		public void doRenderLayer(T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
				float headPitch, float scale) {
			if (VanillaRendererView.this.layers.isEmpty()) {
				return;
			}
			int light = entity.getBrightnessForRender();
			for (RenderLayer<T, M> layer : VanillaRendererView.this.layers) {
				if (VanillaRendererView.this.failed.contains(layer)) {
					continue;
				}
				int depth = PoseStack.depth();
				try {
					layer.render(PoseStack.INSTANCE, MultiBufferSource.INSTANCE, light, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks,
							netHeadYaw, headPitch);
					MultiBufferSource.INSTANCE.endBatch();
				} catch (RuntimeException | LinkageError e) {
					VanillaRendererView.this.failed.add(layer);
					PoseStack.restore(depth);
					io.github.xponer2.translator.runtime.RuntimeStubs.failed(layer.getClass().getName() + ".render on " + VanillaRendererView.this.vanilla
							.getClass().getSimpleName(), e);
				}
			}
			net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		}

		@Override
		public boolean shouldCombineTextures() {
			return false;
		}
	}
}
