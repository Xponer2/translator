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

import javax.annotation.Nullable;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code EntityRenderer} methods called on any renderer, also 1.12.2's own (the dispatcher gives the vanilla
 * renderer of a vanilla entity, which is no facade: JourneyMap's mob icons cast it and failed for every animal).
 */
@SideOnly(Side.CLIENT)
public final class RenderBridges {
	@Nullable
	private static Method entityTexture;

	private RenderBridges() {
	}

	/** {@code renderer.getTextureLocation(entity)}: a facade's own, 1.12.2's getEntityTexture for the game's renderers. */
	@Nullable
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static ResourceLocation getTextureLocation(Render render, Entity entity) {
		if (render instanceof EntityRenderer facade) {
			return facade.getTextureLocation(entity);
		}
		try {
			if (entityTexture == null) {
				entityTexture = ReflectionHelper.findMethod(Render.class, "getEntityTexture", "func_110775_a", Entity.class);
			}
			return (ResourceLocation) entityTexture.invoke(render, entity);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
