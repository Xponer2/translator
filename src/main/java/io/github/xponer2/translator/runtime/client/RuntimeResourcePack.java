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
package io.github.xponer2.translator.runtime.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The assets of an original 1.21.1 mod jar as a 1.12.2 resource pack, converted when read: language files from JSON
 * to .lang, the "" variant of blockstates to "normal", vanilla texture folders of models (block/ -> blocks/, item/ ->
 * items/). What the resource converter of the ports does ahead of time.
 */
@SideOnly(Side.CLIENT)
public final class RuntimeResourcePack implements IResourcePack {
	private final ZipFile zip;
	private final String name;
	private final Set<String> domains = new HashSet<>();

	private RuntimeResourcePack(File jar, String name) throws IOException {
		this.zip = new ZipFile(jar);
		this.name = name;
		for (ZipEntry entry : Collections.list(this.zip.entries())) {
			String path = entry.getName();
			if (path.startsWith("assets/")) {
				int slash = path.indexOf('/', 7);
				if (slash > 7) {
					this.domains.add(path.substring(7, slash));
				}
			}
		}
	}

	private static final List<RuntimeResourcePack> PACKS = new java.util.concurrent.CopyOnWriteArrayList<>();

	/** The ids of the assets under a folder (assets/<namespace>/<folder>/...) of every 1.21.1 mod jar. */
	public static List<ResourceLocation> list(String folder) {
		var ids = new java.util.ArrayList<ResourceLocation>();
		for (RuntimeResourcePack pack : PACKS) {
			for (ZipEntry entry : Collections.list(pack.zip.entries())) {
				String path = entry.getName();
				if (entry.isDirectory() || !path.startsWith("assets/")) {
					continue;
				}
				int slash = path.indexOf('/', 7);
				if (slash > 7 && path.startsWith(folder + "/", slash + 1)) {
					ids.add(new ResourceLocation(path.substring(7, slash), path.substring(slash + 1)));
				}
			}
		}
		return ids;
	}

	/** Adds the jar's assets to the game's default resource packs (before the first resource load). */
	@SuppressWarnings("unchecked")
	public static void add(File jar, String name) throws IOException, ReflectiveOperationException {
		var pack = new RuntimeResourcePack(jar, name);
		PACKS.add(pack);
		// for every later reload, and into the resource manager now (the first load ran before pre-init)
		Field field = FMLClientHandler.class.getDeclaredField("resourcePackList");
		field.setAccessible(true);
		((List<IResourcePack>) field.get(FMLClientHandler.instance())).add(pack);
		var minecraft = net.minecraft.client.Minecraft.getMinecraft();
		if (minecraft.getResourceManager() instanceof net.minecraft.client.resources.SimpleReloadableResourceManager manager) {
			manager.reloadResourcePack(pack);
			// the languages were read in that first load: again, with this pack's
			minecraft.getLanguageManager().onResourceManagerReload(manager);
		}
	}

	private static String path(ResourceLocation location) {
		return "assets/" + location.getNamespace() + "/" + location.getPath();
	}

	@Override
	public InputStream getInputStream(ResourceLocation location) throws IOException {
		String path = path(location);
		ZipEntry entry = this.zip.getEntry(path);
		if (entry == null && path.endsWith(".lang")) {
			ZipEntry json = this.zip.getEntry(path.substring(0, path.length() - 5) + ".json");
			if (json != null) {
				return text(lang(this.read(json)));
			}
		}
		if (entry == null) {
			throw new FileNotFoundException(location.toString());
		}
		if (path.endsWith(".json") && path.contains("/blockstates/")) {
			return text(blockstate(this.read(entry)).toString());
		}
		if (path.endsWith(".json") && path.contains("/models/")) {
			return text(model(this.read(entry)).toString());
		}
		return this.zip.getInputStream(entry);
	}

	private JsonElement read(ZipEntry entry) throws IOException {
		try (var reader = new InputStreamReader(this.zip.getInputStream(entry), StandardCharsets.UTF_8)) {
			return new JsonParser().parse(reader);
		}
	}

	private static InputStream text(String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}

	private static String lang(JsonElement json) {
		var out = new StringBuilder();
		for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
			if (entry.getValue().isJsonPrimitive()) {
				String value = entry.getValue().getAsString().replace('\n', ' ');
				out.append(entry.getKey()).append('=').append(value).append('\n');
				// 1.12.2 names blocks (and their items) tile.<id> and looks up <key>.name
				if (entry.getKey().startsWith("block.")) {
					String tile = "tile." + entry.getKey().substring(6);
					out.append(tile).append('=').append(value).append('\n');
					out.append(tile).append(".name=").append(value).append('\n');
				}
			}
		}
		return out.toString();
	}

	private static JsonElement blockstate(JsonElement json) {
		if (json.isJsonObject() && json.getAsJsonObject().has("variants")) {
			JsonObject variants = json.getAsJsonObject().getAsJsonObject("variants");
			if (variants.has("")) {
				variants.add("normal", variants.remove(""));
			}
		}
		blockstateModels(json);
		return json;
	}

	/** 1.21 blockstates name the model with its folder ("ns:block/x"); 1.12.2 adds "block/" itself. */
	private static void blockstateModels(JsonElement json) {
		if (json.isJsonArray()) {
			json.getAsJsonArray().forEach(RuntimeResourcePack::blockstateModels);
		} else if (json.isJsonObject()) {
			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
				if ("model".equals(entry.getKey()) && entry.getValue().isJsonPrimitive()) {
					String model = entry.getValue().getAsString();
					int colon = model.indexOf(':');
					String path = model.substring(colon + 1);
					if (path.startsWith("block/")) {
						entry.setValue(new JsonPrimitive((colon >= 0 ? model.substring(0, colon + 1) : "") + path.substring(6)));
					}
				} else {
					blockstateModels(entry.getValue());
				}
			}
		}
	}

	private static JsonElement model(JsonElement json) {
		if (json.isJsonObject() && json.getAsJsonObject().has("textures") && json.getAsJsonObject().get("textures").isJsonObject()) {
			JsonObject textures = json.getAsJsonObject().getAsJsonObject("textures");
			for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
				if (entry.getValue().isJsonPrimitive()) {
					entry.setValue(new JsonPrimitive(texture(entry.getValue().getAsString())));
				}
			}
		}
		return json;
	}

	/** 1.21 vanilla texture folders are block/ and item/, 1.12.2's blocks/ and items/. */
	private static String texture(String id) {
		if (id.startsWith("#")) {
			return id;
		}
		boolean vanilla = !id.contains(":") || id.startsWith("minecraft:");
		String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
		if (!vanilla) {
			return id;
		}
		if (path.startsWith("block/")) {
			return "minecraft:blocks/" + path.substring(6);
		}
		if (path.startsWith("item/")) {
			return "minecraft:items/" + path.substring(5);
		}
		return id;
	}

	@Override
	public boolean resourceExists(ResourceLocation location) {
		String path = path(location);
		return this.zip.getEntry(path) != null
				|| path.endsWith(".lang") && this.zip.getEntry(path.substring(0, path.length() - 5) + ".json") != null;
	}

	@Override
	public Set<String> getResourceDomains() {
		return this.domains;
	}

	@Nullable
	@Override
	public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) {
		return null;
	}

	@Nullable
	@Override
	public BufferedImage getPackImage() {
		return null;
	}

	@Override
	public String getPackName() {
		return "Translator: " + this.name;
	}
}
