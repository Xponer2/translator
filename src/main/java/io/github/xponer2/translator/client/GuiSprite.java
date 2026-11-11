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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

/**
 * A 1.21 GUI sprite ({@code textures/gui/sprites/<path>.png}, in 1.21 a part of the GUI atlas): here its own texture,
 * the whole of it from u 0 to 1. Its scaling (stretch, tile or nine-slice) comes from the "gui" section of its
 * .mcmeta, as in 1.21. Client only.
 */
public final class GuiSprite extends TextureAtlasSprite {
	private static final Map<ResourceLocation, GuiSprite> SPRITES = new ConcurrentHashMap<>();

	private final ResourceLocation texture;
	/** "stretch", "tile" or "nine_slice", with the size and border the .mcmeta gives. */
	final String scaling;
	final int scalingWidth;
	final int scalingHeight;
	final int left;
	final int top;
	final int right;
	final int bottom;

	private GuiSprite(ResourceLocation sprite, ResourceLocation texture, int width, int height, @Nullable JsonObject scaling) {
		super(sprite.toString());
		this.texture = texture;
		this.setIconWidth(width);
		this.setIconHeight(height);
		this.initSprite(width, height, 0, 0, false);
		String type = scaling != null && scaling.has("type") ? scaling.get("type").getAsString() : "stretch";
		this.scaling = "tile".equals(type) || "nine_slice".equals(type) ? type : "stretch";
		this.scalingWidth = scaling != null && scaling.has("width") ? scaling.get("width").getAsInt() : width;
		this.scalingHeight = scaling != null && scaling.has("height") ? scaling.get("height").getAsInt() : height;
		JsonElement border = scaling == null ? null : scaling.get("border");
		if (border != null && border.isJsonObject()) {
			JsonObject sides = border.getAsJsonObject();
			this.left = sides.has("left") ? sides.get("left").getAsInt() : 0;
			this.top = sides.has("top") ? sides.get("top").getAsInt() : 0;
			this.right = sides.has("right") ? sides.get("right").getAsInt() : 0;
			this.bottom = sides.has("bottom") ? sides.get("bottom").getAsInt() : 0;
		} else {
			int all = border != null && border.isJsonPrimitive() ? border.getAsInt() : 0;
			this.left = all;
			this.top = all;
			this.right = all;
			this.bottom = all;
		}
	}

	/** The texture the sprite is drawn from. */
	public ResourceLocation texture() {
		return this.texture;
	}

	/** The sprite of a 1.21 GUI sprite name ("namespace:path" under textures/gui/sprites), read once. */
	public static GuiSprite of(ResourceLocation sprite) {
		return SPRITES.computeIfAbsent(sprite, GuiSprite::load);
	}

	private static GuiSprite load(ResourceLocation sprite) {
		var texture = new ResourceLocation(sprite.getNamespace(), "textures/gui/sprites/" + sprite.getPath() + ".png");
		int width = 16;
		int height = 16;
		JsonObject scaling = null;
		var resources = Minecraft.getMinecraft().getResourceManager();
		try (var resource = resources.getResource(texture); InputStream in = resource.getInputStream()) {
			// the PNG header: the size is in the IHDR chunk at bytes 16 to 23
			byte[] header = new byte[24];
			if (in.read(header) == 24) {
				width = readInt(header, 16);
				height = readInt(header, 20);
			}
		} catch (IOException | RuntimeException e) {
			// no such sprite: drawn as the missing texture
		}
		try (var resource = resources.getResource(new ResourceLocation(texture.getNamespace(), texture.getPath() + ".mcmeta"));
				var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
			JsonElement meta = new JsonParser().parse(reader);
			if (meta.isJsonObject() && meta.getAsJsonObject().has("gui")) {
				JsonObject gui = meta.getAsJsonObject().getAsJsonObject("gui");
				scaling = gui.has("scaling") ? gui.getAsJsonObject("scaling") : null;
			}
		} catch (IOException | RuntimeException e) {
			// no .mcmeta: stretched, as 1.21
		}
		return new GuiSprite(sprite, texture, Math.max(1, width), Math.max(1, height), scaling);
	}

	private static int readInt(byte[] bytes, int at) {
		return (bytes[at] & 0xFF) << 24 | (bytes[at + 1] & 0xFF) << 16 | (bytes[at + 2] & 0xFF) << 8 | bytes[at + 3] & 0xFF;
	}

	/** Sprites read again after a resource reload (a resource pack may change them). */
	public static void clear() {
		SPRITES.clear();
	}

	/**
	 * The carried 1.21 {@code ResourceMetadata} of a sprite ({@code sprite.contents().metadata()}): its "gui" section
	 * for {@code GuiMetadataSection.TYPE}, nothing for any other section (JEI reads the nine-slice of its backgrounds
	 * so). Null if the carried classes can't be loaded.
	 */
	@Nullable
	public static Object carriedMetadata(TextureAtlasSprite sprite) {
		GuiSprite gui = sprite instanceof GuiSprite own ? own : null;
		try {
			ClassLoader loader = net.minecraft.launchwrapper.Launch.classLoader;
			Class<?> metadata = Class.forName("net.minecraft.server.packs.resources.ResourceMetadata", true, loader);
			Class<?> section = Class.forName("net.minecraft.client.resources.metadata.gui.GuiMetadataSection", true, loader);
			Object type = section.getField("TYPE").get(null);
			Object scaling = gui == null ? null : gui.carriedScaling();
			Object guiSection = scaling == null ? null
					: section.getConstructor(Class.forName("net.minecraft.client.resources.metadata.gui.GuiSpriteScaling", true, loader)).newInstance(scaling);
			return java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[] {metadata}, (proxy, method, args) -> {
				switch (method.getName()) {
				case "getSection":
					return args != null && args.length == 1 && args[0] == type && guiSection != null ? java.util.Optional.of(guiSection)
							: java.util.Optional.empty();
				case "hashCode":
					return System.identityHashCode(proxy);
				case "equals":
					return proxy == args[0];
				case "toString":
					return "GuiSprite metadata of " + sprite.getIconName();
				default:
					return method.getReturnType() == java.util.Optional.class ? java.util.Optional.empty() : null;
				}
			});
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	/** The carried 1.21 {@code GuiSpriteScaling} of the sprite (Stretch, Tile or NineSlice), null if it can't be made. */
	@Nullable
	Object carriedScaling() {
		try {
			ClassLoader loader = net.minecraft.launchwrapper.Launch.classLoader;
			String base = "net.minecraft.client.resources.metadata.gui.GuiSpriteScaling$";
			switch (this.scaling) {
			case "tile":
				return Class.forName(base + "Tile", true, loader).getConstructor(int.class, int.class).newInstance(this.scalingWidth, this.scalingHeight);
			case "nine_slice":
				Class<?> borderClass = Class.forName(base + "NineSlice$Border", true, loader);
				Object border = borderClass.getConstructor(int.class, int.class, int.class, int.class).newInstance(this.left, this.top, this.right,
						this.bottom);
				return Class.forName(base + "NineSlice", true, loader).getConstructor(int.class, int.class, borderClass).newInstance(this.scalingWidth,
						this.scalingHeight, border);
			default:
				return Class.forName(base + "Stretch", true, loader).getConstructor().newInstance();
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}
}
