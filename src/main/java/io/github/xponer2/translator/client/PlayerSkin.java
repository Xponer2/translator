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

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * 1.21 {@code PlayerSkin} (neoforge-1.21.1-runtime.tsv): the textures of a player, which 1.12.2's
 * AbstractClientPlayer gives one by one ({@code getLocationSkin}, ...). {@link #getSkin} is 1.21
 * {@code AbstractClientPlayer.getSkin()} (Mining Gadgets draws the player's arm with it).
 */
public final class PlayerSkin {
	private final ResourceLocation texture;
	@Nullable
	private final ResourceLocation capeTexture;
	@Nullable
	private final ResourceLocation elytraTexture;
	private final Model model;

	public PlayerSkin(ResourceLocation texture, @Nullable String textureUrl, @Nullable ResourceLocation capeTexture,
			@Nullable ResourceLocation elytraTexture, Model model, boolean secure) {
		this.texture = texture;
		this.capeTexture = capeTexture;
		this.elytraTexture = elytraTexture;
		this.model = model;
	}

	public static PlayerSkin getSkin(AbstractClientPlayer player) {
		return new PlayerSkin(player.getLocationSkin(), null, player.getLocationCape(), player.getLocationElytra(),
				Model.byName(player.getSkinType()), false);
	}

	public ResourceLocation texture() {
		return this.texture;
	}

	@Nullable
	public String textureUrl() {
		return null;
	}

	@Nullable
	public ResourceLocation capeTexture() {
		return this.capeTexture;
	}

	@Nullable
	public ResourceLocation elytraTexture() {
		return this.elytraTexture;
	}

	public Model model() {
		return this.model;
	}

	public boolean secure() {
		return false;
	}

	public enum Model {
		SLIM("slim"),
		WIDE("default");

		private final String id;

		Model(String id) {
			this.id = id;
		}

		public String id() {
			return this.id;
		}

		public static Model byName(@Nullable String name) {
			return "slim".equals(name) ? SLIM : WIDE;
		}
	}
}
