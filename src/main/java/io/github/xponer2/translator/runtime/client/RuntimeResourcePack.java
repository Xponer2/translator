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
	/** The player's Minecraft 1.21.1 jar: only the vanilla models 1.12.2 lacks are taken from it. */
	private final boolean vanilla;

	private RuntimeResourcePack(File jar, String name) throws IOException {
		this(jar, name, false);
	}

	private RuntimeResourcePack(File jar, String name, boolean vanilla) throws IOException {
		this.zip = new ZipFile(jar);
		this.name = name;
		this.vanilla = vanilla;
		if (vanilla) {
			this.domains.add("minecraft");
			return;
		}
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
	public static void add(File jar, String name) throws IOException, ReflectiveOperationException {
		var pack = new RuntimeResourcePack(jar, name);
		PACKS.add(pack);
		install(pack);
	}

	/**
	 * The vanilla 1.21 models the mods' models are made from (template_wall_post, slab, slab_top: walls and slabs of
	 * AE2, the Aether, Create were missing), from the player's own Minecraft 1.21.1 jar. A model 1.12.2 has stays 1.12.2's.
	 */
	public static void addVanilla(File jar) throws IOException, ReflectiveOperationException {
		install(new RuntimeResourcePack(jar, "Minecraft 1.21.1 models and textures", true));
	}

	/** Whether the resource is one the 1.21.1 jar gives: a model of Minecraft that 1.12.2's own assets don't have. */
	private boolean vanillaGives(ResourceLocation location) {
		String file = location.getPath();
		boolean model = file.startsWith("models/") && file.endsWith(".json");
		boolean texture = file.startsWith("textures/") && (file.endsWith(".png") || file.endsWith(".png.mcmeta"));
		if (!"minecraft".equals(location.getNamespace()) || !model && !texture || this.zip.getEntry(vanillaPath(location)) == null) {
			return false;
		}
		var game = net.minecraft.client.Minecraft.getMinecraft().getResourcePackRepository().rprDefaultResourcePack;
		return !game.resourceExists(location);
	}

	/**
	 * Where the 1.21.1 jar has it: the textures 1.12.2 asks for in blocks/ and items/ are in block/ and item/ there
	 * (the 755 vanilla textures the tested mods' models use that 1.12.2 lacks: deepslate, copper, cherry, bamboo...).
	 */
	private static String vanillaPath(ResourceLocation location) {
		String file = location.getPath();
		if (file.startsWith("textures/blocks/")) {
			file = "textures/block/" + file.substring("textures/blocks/".length());
		} else if (file.startsWith("textures/items/")) {
			file = "textures/item/" + file.substring("textures/items/".length());
		}
		return "assets/minecraft/" + file;
	}

	@SuppressWarnings("unchecked")
	private static void install(RuntimeResourcePack pack) throws ReflectiveOperationException {
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
		if (this.vanilla && !this.vanillaGives(location)) {
			throw new FileNotFoundException(location.toString());
		}
		String path = this.vanilla ? vanillaPath(location) : path(location);
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
			JsonElement json = blockstate(this.read(entry), location);
			fullVariants(json, location);
			return text(json.toString());
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
				// and entities entity.<namespace>.<id>.name (the boss bars of Iron's Spells showed the raw key)
				if (entry.getKey().startsWith("entity.") && entry.getKey().chars().filter(c -> c == '.').count() == 2) {
					out.append(entry.getKey()).append(".name=").append(value).append('\n');
				}
			}
		}
		return out.toString();
	}

	private static JsonElement blockstate(JsonElement json, ResourceLocation location) {
		if (json.isJsonObject() && json.getAsJsonObject().has("variants")) {
			JsonObject variants = json.getAsJsonObject().getAsJsonObject("variants");
			if (variants.has("")) {
				variants.add("normal", variants.remove(""));
			}
		}
		blockstateModels(json);
		if (!hasWallSideProperty(location)) {
			wallSides(json);
		}
		attachFaces(json);
		return json;
	}

	/**
	 * A 1.21 button's variants are by face (floor, wall, ceiling) and facing; 1.12.2's button has one facing, up and down
	 * included: floor becomes up, ceiling down, a wall face its facing (the first variant of a 1.12.2 state is kept). The
	 * buttons of every mod had no model ("missing variant facing=up,powered=false": Biomes O' Plenty's 165 errors).
	 */
	private static void attachFaces(JsonElement json) {
		if (!json.isJsonObject() || !json.getAsJsonObject().has("variants") || !json.getAsJsonObject().get("variants").isJsonObject()) {
			return;
		}
		JsonObject variants = json.getAsJsonObject().getAsJsonObject("variants");
		boolean faces = false;
		for (var entry : variants.entrySet()) {
			String key = entry.getKey();
			faces |= key.contains("face=floor") || key.contains("face=wall") || key.contains("face=ceiling");
		}
		if (!faces) {
			return;
		}
		var converted = new java.util.LinkedHashMap<String, JsonElement>();
		for (var entry : variants.entrySet()) {
			var properties = new java.util.TreeMap<String, String>();
			String face = null;
			for (String part : entry.getKey().split(",")) {
				int equals = part.indexOf('=');
				if (equals < 0) {
					continue;
				}
				String name = part.substring(0, equals);
				String value = part.substring(equals + 1);
				if ("face".equals(name)) {
					face = value;
				} else {
					properties.put(name, value);
				}
			}
			// on the floor and the ceiling 1.12.2 has one button, lying along x (its box): the 1.21 variant of that
			// orientation (floor facing north: no turn; ceiling facing south: x 180 only), not the first one (turned 90
			// degrees: the box and the button crossed)
			String along = properties.get("facing");
			boolean preferred = "floor".equals(face) && "north".equals(along) || "ceiling".equals(face) && "south".equals(along);
			if ("floor".equals(face)) {
				properties.put("facing", "up");
			} else if ("ceiling".equals(face)) {
				properties.put("facing", "down");
			}
			var key = new StringBuilder();
			for (var property : properties.entrySet()) {
				if (key.length() > 0) {
					key.append(',');
				}
				key.append(property.getKey()).append('=').append(property.getValue());
			}
			if (preferred) {
				converted.put(key.toString(), entry.getValue());
			} else {
				converted.putIfAbsent(key.toString(), entry.getValue());
			}
		}
		var old = new java.util.ArrayList<String>();
		for (var entry : variants.entrySet()) {
			old.add(entry.getKey());
		}
		for (String key : old) {
			variants.remove(key);
		}
		converted.forEach(variants::add);
	}

	/**
	 * 1.21 matches a variant key against a block's state partly ("mossy=false" for every state with mossy false, the
	 * waterlogged ones too); 1.12.2 needs the whole state string. For a block that exists, every state gets the variant
	 * whose key fits it best (a property of the key the state lacks counts when its value is false or none).
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void fullVariants(JsonElement json, ResourceLocation location) {
		if (!json.isJsonObject() || !json.getAsJsonObject().has("variants") || !json.getAsJsonObject().get("variants").isJsonObject()) {
			return;
		}
		String path = location.getPath();
		String name = path.substring(path.lastIndexOf('/') + 1).replace(".json", "");
		var block = net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS.getValue(new ResourceLocation(location.getNamespace(), name));
		if (block == null || block == net.minecraft.init.Blocks.AIR) {
			return;
		}
		JsonObject variants = json.getAsJsonObject().getAsJsonObject("variants");
		var keys = new java.util.ArrayList<java.util.Map.Entry<java.util.Map<String, String>, JsonElement>>();
		for (var entry : variants.entrySet()) {
			var properties = new java.util.HashMap<String, String>();
			if (!"normal".equals(entry.getKey()) && !entry.getKey().isEmpty()) {
				for (String part : entry.getKey().split(",")) {
					int equals = part.indexOf('=');
					if (equals > 0) {
						properties.put(part.substring(0, equals), part.substring(equals + 1));
					}
				}
			}
			keys.add(new java.util.AbstractMap.SimpleEntry<>(properties, entry.getValue()));
		}
		var additions = new java.util.LinkedHashMap<String, JsonElement>();
		java.util.List<net.minecraft.block.state.IBlockState> states;
		try {
			states = block.getBlockState().getValidStates();
		} catch (RuntimeException e) {
			return;
		}
		if (states.size() > 4096) {
			return;
		}
		for (var state : states) {
			var actual = new java.util.TreeMap<String, String>();
			for (var property : state.getProperties().entrySet()) {
				net.minecraft.block.properties.IProperty key = property.getKey();
				actual.put(key.getName(), key.getName((Comparable) property.getValue()));
			}
			var full = new StringBuilder();
			for (var property : actual.entrySet()) {
				if (full.length() > 0) {
					full.append(',');
				}
				full.append(property.getKey()).append('=').append(property.getValue());
			}
			String fullKey = actual.isEmpty() ? "normal" : full.toString();
			if (variants.has(fullKey) || additions.containsKey(fullKey)) {
				continue;
			}
			JsonElement best = null;
			int bestScore = -1;
			for (var key : keys) {
				int score = 0;
				boolean fits = true;
				for (var wanted : key.getKey().entrySet()) {
					String value = actual.get(wanted.getKey());
					if (value == null) {
						if (!"false".equals(wanted.getValue()) && !"none".equals(wanted.getValue())) {
							fits = false;
							break;
						}
					} else if (value.equals(wanted.getValue())) {
						score++;
					} else {
						fits = false;
						break;
					}
				}
				if (fits && score > bestScore) {
					bestScore = score;
					best = key.getValue();
				}
			}
			if (best != null) {
				additions.put(fullKey, best);
			}
		}
		additions.forEach(variants::add);
	}

	/**
	 * Whether the block of the blockstate file has 1.21's three-valued wall sides itself (none, low, tall: a mod's own
	 * block with BlockStateProperties.NORTH_WALL, Supplementaries' wicker fence) rather than the true / false sides of a
	 * wall built on 1.12.2's: its file stays as it is (made true / false, no part of its model matched and it was the
	 * missing model).
	 */
	private static boolean hasWallSideProperty(ResourceLocation location) {
		String path = location.getPath();
		int start = path.lastIndexOf("blockstates/");
		if (start < 0 || !path.endsWith(".json")) {
			return false;
		}
		var block = net.minecraft.block.Block.REGISTRY.getObject(new ResourceLocation(location.getNamespace(),
				path.substring(start + "blockstates/".length(), path.length() - ".json".length())));
		if (block == null || block == net.minecraft.init.Blocks.AIR) {
			return false;
		}
		for (var property : block.getBlockState().getProperties()) {
			if (SIDES.contains(property.getName()) && property.getValueClass() != Boolean.class && hasLow(property)) {
				return true;
			}
		}
		return false;
	}

	private static <T extends Comparable<T>> boolean hasLow(net.minecraft.block.properties.IProperty<T> property) {
		for (T value : property.getAllowedValues()) {
			if ("low".equals(property.getName(value))) {
				return true;
			}
		}
		return false;
	}

	private static final Set<String> SIDES = new HashSet<>(java.util.Arrays.asList("north", "east", "south", "west"));
	private static final Set<String> WALL_SIDES = new HashSet<>(java.util.Arrays.asList("none", "low", "tall"));

	/**
	 * A 1.21 wall's multipart sides are none, low or tall; the Translator's walls have 1.12.2's sides, connected or
	 * not: low becomes true, none false, and the tall parts go (a 1.12.2 wall side has one height). Only for a file
	 * whose sides are all wall sides (redstone wire's are side, up, none).
	 */
	private static void wallSides(JsonElement json) {
		if (!json.isJsonObject() || !json.getAsJsonObject().has("multipart") || !json.getAsJsonObject().get("multipart").isJsonArray()) {
			return;
		}
		var parts = json.getAsJsonObject().getAsJsonArray("multipart");
		boolean wall = false;
		for (JsonElement part : parts) {
			JsonObject when = part.isJsonObject() && part.getAsJsonObject().has("when") && part.getAsJsonObject().get("when").isJsonObject()
					? part.getAsJsonObject().getAsJsonObject("when") : null;
			if (when == null) {
				continue;
			}
			for (Map.Entry<String, JsonElement> condition : when.entrySet()) {
				if (!SIDES.contains(condition.getKey()) || !condition.getValue().isJsonPrimitive()) {
					continue;
				}
				for (String value : condition.getValue().getAsString().split("\\|")) {
					if (!WALL_SIDES.contains(value)) {
						return;
					}
					wall |= !"none".equals(value);
				}
			}
		}
		if (!wall) {
			return;
		}
		var kept = new com.google.gson.JsonArray();
		for (JsonElement part : parts) {
			JsonObject when = part.isJsonObject() && part.getAsJsonObject().has("when") && part.getAsJsonObject().get("when").isJsonObject()
					? part.getAsJsonObject().getAsJsonObject("when") : null;
			boolean keep = true;
			if (when != null) {
				for (Map.Entry<String, JsonElement> condition : when.entrySet()) {
					if (!SIDES.contains(condition.getKey()) || !condition.getValue().isJsonPrimitive()) {
						continue;
					}
					var values = new java.util.LinkedHashSet<String>();
					for (String value : condition.getValue().getAsString().split("\\|")) {
						values.add("none".equals(value) ? "false" : "low".equals(value) ? "true" : "tall");
					}
					values.remove("tall");
					if (values.isEmpty()) {
						keep = false;
					} else {
						condition.setValue(new JsonPrimitive(String.join("|", values)));
					}
				}
			}
			if (keep) {
				kept.add(part);
			}
		}
		json.getAsJsonObject().add("multipart", kept);
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
		if (this.vanilla) {
			return this.vanillaGives(location);
		}
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
