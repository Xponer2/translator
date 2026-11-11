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
