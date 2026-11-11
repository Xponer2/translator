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
package io.github.xponer2.translator.runtime.redirect;

import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import io.github.xponer2.translator.client.PlayerSkin;

import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;

/**
 * The 1.21 player skin lookups ({@code PlayerSkin}) on 1.12.2's skin manager (neoforge-1.21.1-redirects.tsv): skulls
 * and statues with a player's face (Supplementaries, Twilight Forest, Pipez), maps (Xaero's). Client only.
 */
public final class SkinBridges {
	private SkinBridges() {
	}

	/** 1.21 {@code DefaultPlayerSkin.get(uuid)}: the default skin of that player (Steve or Alex by the UUID). */
	public static PlayerSkin get(UUID uuid) {
		return new PlayerSkin(DefaultPlayerSkin.getDefaultSkin(uuid), null, null, null, PlayerSkin.Model.byName(DefaultPlayerSkin.getSkinType(uuid)),
				true);
	}

	/** 1.21 {@code DefaultPlayerSkin.getDefaultTexture()}: Steve's skin. */
	public static ResourceLocation getDefaultTexture() {
		return DefaultPlayerSkin.getDefaultSkinLegacy();
	}

	/**
	 * 1.21 {@code skinManager.getInsecureSkin(profile)}: the skin the profile's textures name (loaded, or being
	 * downloaded), the player's default skin while there is none.
	 */
	public static PlayerSkin getInsecureSkin(SkinManager skins, @Nullable GameProfile profile) {
		if (profile == null) {
			return get(new UUID(0L, 0L));
		}
		UUID uuid = profile.getId() != null ? profile.getId() : new UUID(0L, 0L);
		Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = skins.loadSkinFromCache(profile);
		MinecraftProfileTexture skin = textures.get(MinecraftProfileTexture.Type.SKIN);
		if (skin == null) {
			return get(uuid);
		}
		ResourceLocation texture = skins.loadSkin(skin, MinecraftProfileTexture.Type.SKIN);
		MinecraftProfileTexture cape = textures.get(MinecraftProfileTexture.Type.CAPE);
		ResourceLocation capeTexture = cape == null ? null : skins.loadSkin(cape, MinecraftProfileTexture.Type.CAPE);
		String model = skin.getMetadata("model");
		return new PlayerSkin(texture, skin.getUrl(), capeTexture, null, PlayerSkin.Model.byName(model == null ? "default" : model), false);
	}
}
