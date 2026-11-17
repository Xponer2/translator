/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client;

import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code TextureAtlasHolder} (S row): a mod's own atlas (Twilight Forest's magic paintings).
 * 1.12.2 has one atlas: the sprites of every atlas source file of the 1.21.1 mods are stitched into the blocks atlas
 * ({@link AtlasTextures}), and the holder's atlas is that one, bound under the holder's location too. The 1.21
 * constructor made a TextureAtlas 1.12.2 has no constructor for: Twilight Forest's client setup failed
 * ("Registering texture").
 */
@SideOnly(Side.CLIENT)
public abstract class TextureAtlasHolder implements AutoCloseable {
	/** 1.21 {@code textureAtlas}: the blocks atlas. */
	protected final TextureMap textureAtlas;

	public TextureAtlasHolder(TextureManager textureManager, ResourceLocation textureAtlasLocation, ResourceLocation atlasInfoLocation) {
		this.textureAtlas = Minecraft.getMinecraft().getTextureMapBlocks();
		bindAs(textureManager, textureAtlasLocation, this.textureAtlas);
	}

	public TextureAtlasHolder(TextureManager textureManager, ResourceLocation textureAtlasLocation, ResourceLocation atlasInfoLocation,
			Set<?> metadataSections) {
		this(textureManager, textureAtlasLocation, atlasInfoLocation);
	}

	/**
	 * The atlas under another location of the texture manager, as it is (no load: that would stitch the blocks atlas
	 * again): a renderer binding the mod's atlas location binds the blocks atlas, where the holder's sprites are.
	 */
	static void bindAs(TextureManager textures, ResourceLocation location, ITextureObject atlas) {
		if (textures == null || location == null || location.equals(TextureMap.LOCATION_BLOCKS_TEXTURE)) {
			return;
		}
		try {
			Map<ResourceLocation, ITextureObject> objects = ObfuscationReflectionHelper.getPrivateValue(TextureManager.class, textures,
					"field_110585_a");
			objects.put(location, atlas);
		} catch (RuntimeException e) {
			io.github.xponer2.translator.Translator.LOGGER.warn("Couldn't bind the atlas {} as the blocks atlas: {}", location, e.toString());
		}
	}

	/** 1.21 {@code getSprite(name)}: the sprite of the blocks atlas by its 1.21 name. */
	protected TextureAtlasSprite getSprite(ResourceLocation location) {
		return io.github.xponer2.translator.runtime.redirect.ScreenBridges.getSprite(this.textureAtlas, location);
	}

	/** Its reload: the sprites are stitched with the blocks atlas, nothing else to load. */
	public void onResourceManagerReload(Object resourceManager) {
	}

	@Override
	public void close() {
	}
}
