/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

/**
 * The 1.21 GUI sprite atlas ({@code minecraft.getGuiSprites()}): JEI's buttons and backgrounds, Jade's tooltip box,
 * GuideME's pages take their sprites from it. Each sprite is its own texture here (see {@link GuiSprite}). Client only.
 */
public final class GuiSpriteManager {
	public static final GuiSpriteManager INSTANCE = new GuiSpriteManager();

	private GuiSpriteManager() {
	}

	public TextureAtlasSprite getSprite(ResourceLocation sprite) {
		return GuiSprite.of(sprite);
	}

	/** The carried GuiSpriteScaling of a sprite (stretched for one that isn't a GUI sprite). */
	@SuppressWarnings("unchecked")
	public <R> R getSpriteScaling(TextureAtlasSprite sprite) {
		GuiSprite gui = sprite instanceof GuiSprite own ? own : GuiSprite.of(new ResourceLocation(sprite.getIconName()));
		return (R) gui.carriedScaling();
	}
}
