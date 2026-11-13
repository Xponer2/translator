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
package io.github.xponer2.translator.advancement;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import io.github.xponer2.translator.runtime.RuntimeMods;
import io.github.xponer2.translator.util.ItemIds;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementList;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * The advancements of the 1.21.1 mods ({@code data/<mod>/advancement/*.json}) in 1.12.2's advancements: 1.12.2 reads
 * only {@code assets/<mod>/advancements} of its own mods, in its own format. Each file is turned into that format
 * ({@link #convert}): the icon by its 1.21 item id, the criteria whose triggers 1.12.2 has (visiting a biome, items in
 * the inventory, placing or entering a block, unlocking a recipe, eating, changing dimension, killing, breeding,
 * taming); a 1.21 list of biomes or items (or a tag of them) becomes one criterion for each, any of which counts. A
 * criterion 1.12.2 can't check is impossible, so its advancement stays to be done rather than done at once. Biomes O'
 * Plenty: its biome advancements and its 308 recipe unlocks (recipe book).
 * <p>
 * They are added to the server's list once the overworld loads, and again after {@code /reload} cleared it.
 */
public final class DataAdvancements {
	private static final Logger LOGGER = LogManager.getLogger("Translator Runtime");
	private static final Pattern FILE = Pattern.compile("([a-z0-9_.-]+)/advancements?/([a-z0-9_./-]+)\\.json");

	@Nullable
	private static Map<ResourceLocation, JsonObject> files;
	/** One of the advancements added: when it is gone, the list was reloaded. */
	@Nullable
	private static ResourceLocation marker;
	private int ticks;

	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {
		if (!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0 && event.getWorld().getMinecraftServer() != null) {
			add(event.getWorld().getMinecraftServer());
		}
	}

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || ++this.ticks % 40 != 0 || marker == null) {
			return;
		}
		MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		if (server != null && server.getAdvancementManager().getAdvancement(marker) == null) {
			add(server);
			for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
				player.getAdvancements().reload();
			}
		}
	}

	/** The mods' advancements into the server's list (those it has already are left). */
	static synchronized void add(MinecraftServer server) {
		AdvancementList list = list();
		if (list == null) {
			return;
		}
		var builders = new LinkedHashMap<ResourceLocation, Advancement.Builder>();
		int failed = 0;
		int unlocksNothing = 0;
		String firstError = null;
		for (var entry : files().entrySet()) {
			if (server.getAdvancementManager().getAdvancement(entry.getKey()) != null) {
				continue;
			}
			try {
				JsonObject converted = convert(entry.getValue(), !parents().contains(entry.getKey().toString()));
				if (converted == null) {
					unlocksNothing++;
					continue;
				}
				builders.put(entry.getKey(), AdvancementManager.GSON.fromJson(converted, Advancement.Builder.class));
			} catch (RuntimeException e) {
				failed++;
				if (firstError == null) {
					firstError = entry.getKey() + ": " + e.getMessage();
				}
			}
		}
		if (builders.isEmpty()) {
			return;
		}
		// 1.12.2's list takes the builders it loaded out of the map: the ids kept first
		var ids = new java.util.ArrayList<>(builders.keySet());
		list.loadAdvancements(builders);
		int loaded = 0;
		for (ResourceLocation id : ids) {
			if (list.getAdvancement(id) != null) {
				loaded++;
				if (marker == null) {
					marker = id;
				}
			}
		}
		LOGGER.info("Advancements of the 1.21.1 mods: {} of {} loaded ({} recipe unlocks of recipes 1.12.2 has no table for left out){}",
				loaded, ids.size() + failed, unlocksNothing, firstError == null ? "" : " (first unreadable: " + firstError + ")");
	}

	@Nullable
	private static AdvancementList list() {
		for (Field field : AdvancementManager.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()) && field.getType() == AdvancementList.class) {
				try {
					field.setAccessible(true);
					return (AdvancementList) field.get(null);
				} catch (ReflectiveOperationException | RuntimeException e) {
					LOGGER.warn("Couldn't reach the server's advancements: {}", e.toString());
					return null;
				}
			}
		}
		return null;
	}

	private static Set<String> parents;

	/** The ids the mods' advancements name as their parent (those aren't leaves). */
	private static synchronized Set<String> parents() {
		if (parents == null) {
			var found = new java.util.HashSet<String>();
			for (JsonObject file : files().values()) {
				if (file.has("parent") && file.get("parent").isJsonPrimitive()) {
					String parent = file.get("parent").getAsString();
					found.add(parent.contains(":") ? parent : "minecraft:" + parent);
				}
			}
			parents = found;
		}
		return parents;
	}

	private static synchronized Map<ResourceLocation, JsonObject> files() {
		if (files == null) {
			var found = new LinkedHashMap<ResourceLocation, JsonObject>();
			RuntimeMods.forEachData((path, bytes) -> {
				var m = FILE.matcher(path);
				if (!m.matches()) {
					return;
				}
				try {
					found.put(new ResourceLocation(m.group(1), m.group(2)),
							new JsonParser().parse(new String(bytes.get(), StandardCharsets.UTF_8)).getAsJsonObject());
				} catch (RuntimeException e) {
					LOGGER.warn("Couldn't read {}: {}", path, e.toString());
				}
			});
			files = found;
		}
		return files;
	}

	// --- 1.21 format -> 1.12.2 format ------------------------------------------------------------------------------------

	/**
	 * A 1.21 advancement as 1.12.2 reads one; null for one that only unlocks recipes 1.12.2's recipe book hasn't (smelting,
	 * stonecutting: 1.12.2 checks every recipe it names against its crafting recipes).
	 */
	@Nullable
	static JsonObject convert(JsonObject in, boolean leaf) {
		var out = new JsonObject();
		// the parent only places an advancement in the advancement screens' trees, where a leaf without a display isn't
		// shown: those (the mods' recipe unlocks, 5920 of them) come without it. Under minecraft:recipes/root 1.12.2's
		// visibility update walked all of root's children again for each one that changed, nested: picking up a boat
		// completed hundreds at once and the server died of a StackOverflowError (Twilight Forest's chest boats)
		if (in.has("parent") && (in.has("display") || !leaf)) {
			out.add("parent", in.get("parent"));
		}
		if (in.has("display") && in.get("display").isJsonObject()) {
			out.add("display", display(in.getAsJsonObject("display")));
		}
		if (in.has("rewards") && in.get("rewards").isJsonObject()) {
			JsonObject rewards = copy(in.getAsJsonObject("rewards"));
			if (rewards.has("recipes") && rewards.get("recipes").isJsonArray()) {
				var known = new JsonArray();
				for (JsonElement recipe : rewards.getAsJsonArray("recipes")) {
					if (knownRecipe(recipe.getAsString())) {
						known.add(recipe);
					}
				}
				if (known.size() == 0 && rewards.getAsJsonArray("recipes").size() > 0 && !in.has("display")) {
					return null;
				}
				rewards.add("recipes", known);
			}
			out.add("rewards", rewards);
		}
		var criteria = new JsonObject();
		// each 1.21 criterion: the names of the 1.12.2 ones it became (any of them counts)
		var names = new LinkedHashMap<String, List<String>>();
		JsonObject source = in.has("criteria") ? in.getAsJsonObject("criteria") : new JsonObject();
		for (var entry : source.entrySet()) {
			List<JsonObject> alternatives = entry.getValue().isJsonObject() ? criterion(entry.getValue().getAsJsonObject()) : Collections.emptyList();
			if (alternatives.isEmpty()) {
				alternatives = Collections.singletonList(impossible());
			}
			var own = new ArrayList<String>();
			for (int i = 0; i < alternatives.size(); i++) {
				String name = alternatives.size() == 1 ? entry.getKey() : entry.getKey() + "#" + i;
				criteria.add(name, alternatives.get(i));
				own.add(name);
			}
			names.put(entry.getKey(), own);
		}
		out.add("criteria", criteria);
		var requirements = new JsonArray();
		if (in.has("requirements") && in.get("requirements").isJsonArray()) {
			for (JsonElement any : in.getAsJsonArray("requirements")) {
				var group = new JsonArray();
				for (JsonElement name : any.getAsJsonArray()) {
					for (String one : names.getOrDefault(name.getAsString(), Collections.singletonList(name.getAsString()))) {
						if (criteria.has(one)) {
							group.add(new JsonPrimitive(one));
						}
					}
				}
				if (group.size() > 0) {
					requirements.add(group);
				}
			}
		} else {
			// 1.21's default: every criterion
			for (List<String> own : names.values()) {
				var group = new JsonArray();
				own.forEach(name -> group.add(new JsonPrimitive(name)));
				requirements.add(group);
			}
		}
		out.add("requirements", requirements);
		return out;
	}

	private static JsonObject display(JsonObject in) {
		var out = new JsonObject();
		for (var entry : in.entrySet()) {
			out.add(entry.getKey(), entry.getValue());
		}
		if (in.has("icon") && in.get("icon").isJsonObject()) {
			JsonObject icon = in.getAsJsonObject("icon");
			String id = icon.has("id") ? icon.get("id").getAsString() : icon.has("item") ? icon.get("item").getAsString() : "minecraft:stone";
			var stacks = stacks(id);
			ItemStack stack = stacks.isEmpty() ? new ItemStack(net.minecraft.init.Blocks.STONE) : stacks.get(0);
			var converted = new JsonObject();
			converted.addProperty("item", String.valueOf(stack.getItem().getRegistryName()));
			converted.addProperty("data", stack.getMetadata());
			out.add("icon", converted);
		}
		return out;
	}

	/** The 1.12.2 criteria a 1.21 criterion becomes (any of them counts); empty when 1.12.2 can't check it. */
	static List<JsonObject> criterion(JsonObject in) {
		String trigger = in.has("trigger") ? in.get("trigger").getAsString() : "";
		JsonObject conditions = in.has("conditions") && in.get("conditions").isJsonObject() ? in.getAsJsonObject("conditions") : new JsonObject();
		var out = new ArrayList<JsonObject>();
		switch (trigger) {
			case "minecraft:impossible", "minecraft:tick" -> out.add(trigger(trigger, null));
			case "minecraft:location", "minecraft:slept_in_bed" -> {
				JsonObject location = location(conditions.get("player"));
				if (location == null) {
					if (conditions.has("player")) {
						return out;
					}
					out.add(trigger(trigger, null));
					return out;
				}
				String dimension = location.has("dimension") ? dimension(location.get("dimension").getAsString()) : null;
				List<String> biomes = location.has("biomes") ? biomes(location.get("biomes"))
						: location.has("biome") ? biomes(location.get("biome")) : null;
				if (biomes == null) {
					if (dimension == null) {
						return out;
					}
					var c = new JsonObject();
					c.addProperty("dimension", dimension);
					out.add(trigger(trigger, at(trigger, c)));
					return out;
				}
				for (String biome : biomes) {
					var c = new JsonObject();
					c.addProperty("biome", biome);
					if (dimension != null) {
						c.addProperty("dimension", dimension);
					}
					out.add(trigger(trigger, at(trigger, c)));
				}
			}
			case "minecraft:inventory_changed" -> {
				JsonArray items = conditions.has("items") && conditions.get("items").isJsonArray() ? conditions.getAsJsonArray("items") : new JsonArray();
				if (items.size() == 0) {
					out.add(trigger(trigger, null));
					return out;
				}
				// several predicates must all hold: each its first item then; one with several items: one criterion each
				if (items.size() == 1) {
					for (JsonObject predicate : itemPredicates(items.get(0))) {
						var list = new JsonArray();
						list.add(predicate);
						var c = new JsonObject();
						c.add("items", list);
						out.add(trigger(trigger, c));
					}
				} else {
					var list = new JsonArray();
					for (JsonElement one : items) {
						List<JsonObject> predicates = itemPredicates(one);
						if (predicates.isEmpty()) {
							return out;
						}
						list.add(predicates.get(0));
					}
					var c = new JsonObject();
					c.add("items", list);
					out.add(trigger(trigger, c));
				}
			}
			case "minecraft:consume_item" -> {
				if (!conditions.has("item")) {
					out.add(trigger(trigger, null));
					return out;
				}
				for (JsonObject predicate : itemPredicates(conditions.get("item"))) {
					var c = new JsonObject();
					c.add("item", predicate);
					out.add(trigger(trigger, c));
				}
			}
			case "minecraft:enter_block" -> {
				String block = conditions.has("block") ? block(conditions.get("block").getAsString()) : null;
				if (conditions.has("block") && block == null) {
					return out;
				}
				var c = new JsonObject();
				if (block != null) {
					c.addProperty("block", block);
				}
				out.add(trigger(trigger, c));
			}
			case "minecraft:placed_block" -> {
				String block = placedBlock(conditions);
				if (block == null) {
					if (conditions.has("location")) {
						return out;
					}
					out.add(trigger(trigger, null));
					return out;
				}
				var c = new JsonObject();
				c.addProperty("block", block);
				out.add(trigger(trigger, c));
			}
			case "minecraft:recipe_unlocked" -> {
				if (conditions.has("recipe") && knownRecipe(conditions.get("recipe").getAsString())) {
					var c = new JsonObject();
					c.add("recipe", conditions.get("recipe"));
					out.add(trigger(trigger, c));
				}
			}
			case "minecraft:changed_dimension" -> {
				var c = new JsonObject();
				for (String key : new String[] {"from", "to"}) {
					if (conditions.has(key)) {
						String dimension = dimension(conditions.get(key).getAsString());
						if (dimension == null) {
							return out;
						}
						c.addProperty(key, dimension);
					}
				}
				out.add(trigger(trigger, c));
			}
			case "minecraft:player_killed_entity", "minecraft:entity_killed_player", "minecraft:bred_animals", "minecraft:summoned_entity",
					"minecraft:tame_animal" -> {
				String key = "minecraft:bred_animals".equals(trigger) ? "child" : "minecraft:tame_animal".equals(trigger) ? "entity" : "entity";
				if (!conditions.has(key)) {
					out.add(trigger(trigger, null));
					return out;
				}
				String type = entityType(conditions.get(key));
				if (type == null) {
					return out;
				}
				var entity = new JsonObject();
				entity.addProperty("type", type);
				var c = new JsonObject();
				c.add(key, entity);
				out.add(trigger(trigger, c));
			}
			default -> {
				// a trigger 1.12.2 hasn't: impossible (by the caller)
			}
		}
		return out;
	}

	/** A recipe 1.12.2's crafting recipes have (a recipe_unlocked condition or reward naming another one fails to load). */
	private static boolean knownRecipe(String id) {
		try {
			return net.minecraft.item.crafting.CraftingManager.getRecipe(new ResourceLocation(id)) != null;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private static JsonObject copy(JsonObject in) {
		var out = new JsonObject();
		for (var entry : in.entrySet()) {
			out.add(entry.getKey(), entry.getValue());
		}
		return out;
	}

	private static JsonObject trigger(String trigger, @Nullable JsonObject conditions) {
		var out = new JsonObject();
		out.addProperty("trigger", trigger);
		if (conditions != null && conditions.size() > 0) {
			out.add("conditions", conditions);
		}
		return out;
	}

	/** 1.12.2's location trigger takes the location as its conditions, slept_in_bed under "location". */
	private static JsonObject at(String trigger, JsonObject location) {
		if (!"minecraft:slept_in_bed".equals(trigger)) {
			return location;
		}
		var c = new JsonObject();
		c.add("location", location);
		return c;
	}

	private static JsonObject impossible() {
		return trigger("minecraft:impossible", null);
	}

	/** The location of a 1.21 player predicate (a list of loot conditions, or the entity predicate itself). */
	@Nullable
	private static JsonObject location(@Nullable JsonElement player) {
		if (player == null) {
			return null;
		}
		if (player.isJsonArray()) {
			for (JsonElement condition : player.getAsJsonArray()) {
				if (condition.isJsonObject() && condition.getAsJsonObject().has("predicate")) {
					JsonObject found = location(condition.getAsJsonObject().get("predicate"));
					if (found != null) {
						return found;
					}
				}
			}
			return null;
		}
		JsonObject object = player.isJsonObject() ? player.getAsJsonObject() : null;
		return object != null && object.has("location") && object.get("location").isJsonObject() ? object.getAsJsonObject("location") : null;
	}

	/** The 1.12.2 biomes of a 1.21 biome list (an id, a list, a tag): those registered here. */
	public static List<String> biomes(JsonElement biomes) {
		var ids = new ArrayList<String>();
		for (String name : names(biomes)) {
			if (name.startsWith("#")) {
				for (ResourceLocation id : io.github.xponer2.translator.tags.TagManager.ids("worldgen/biome", new ResourceLocation(name.substring(1)))) {
					ids.add(id.toString());
				}
			} else {
				ids.add(name);
			}
		}
		var out = new ArrayList<String>();
		for (String id : ids) {
			if (ForgeRegistries.BIOMES.containsKey(new ResourceLocation(id)) && !out.contains(id)) {
				out.add(id);
			}
		}
		return out;
	}

	private static List<String> names(JsonElement element) {
		var out = new ArrayList<String>();
		if (element.isJsonArray()) {
			for (JsonElement one : element.getAsJsonArray()) {
				out.add(one.getAsString());
			}
		} else if (element.isJsonPrimitive()) {
			out.add(element.getAsString());
		}
		return out;
	}

	/** A 1.21 dimension id as 1.12.2 names it (overworld, the_nether, the_end); null for another. */
	@Nullable
	public static String dimension(String id) {
		String path = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
		return path.equals("overworld") || path.equals("the_nether") || path.equals("the_end") ? path : null;
	}

	/** 1.12.2 item predicates for a 1.21 one (its items, a list or a tag, each its own): empty when none is here. */
	public static List<JsonObject> itemPredicates(JsonElement predicate) {
		var out = new ArrayList<JsonObject>();
		if (!predicate.isJsonObject()) {
			return out;
		}
		JsonObject in = predicate.getAsJsonObject();
		var names = new ArrayList<String>();
		if (in.has("items")) {
			names.addAll(names(in.get("items")));
		}
		if (in.has("item")) {
			names.addAll(names(in.get("item")));
		}
		if (in.has("tag")) {
			names.add("#" + in.get("tag").getAsString());
		}
		var stacks = new ArrayList<ItemStack>();
		for (String name : names) {
			stacks.addAll(stacks(name));
		}
		if (names.isEmpty()) {
			// any item (a count or components only): the count is kept
			var any = new JsonObject();
			if (in.has("count")) {
				any.add("count", in.get("count"));
			}
			out.add(any);
			return out;
		}
		for (ItemStack stack : stacks) {
			var one = new JsonObject();
			one.addProperty("item", String.valueOf(stack.getItem().getRegistryName()));
			if (stack.getHasSubtypes() || stack.getMetadata() != 0) {
				one.addProperty("data", stack.getMetadata());
			}
			if (in.has("count")) {
				one.add("count", in.get("count"));
			}
			out.add(one);
		}
		return out;
	}

	/** The 1.12.2 stacks of a 1.21 item id or #tag. */
	private static List<ItemStack> stacks(String name) {
		var out = new ArrayList<ItemStack>();
		if (name.startsWith("#")) {
			for (ItemStack stack : io.github.xponer2.translator.tags.TagManager.itemTag(new ResourceLocation(name.substring(1)))) {
				if (!stack.isEmpty()) {
					out.add(stack);
				}
			}
			return out;
		}
		ItemStack stack = ItemIds.stack(new ResourceLocation(name), 1);
		if (!stack.isEmpty() && stack.getItem().getRegistryName() != null) {
			out.add(stack);
		}
		return out;
	}

	/** The 1.12.2 block name of a 1.21 block id, null when none is here. */
	@Nullable
	private static String block(String name) {
		IBlockState state = io.github.xponer2.translator.runtime.redirect.BlockStateStatics.byName(new ResourceLocation(name));
		return state == null || state.getBlock().getRegistryName() == null ? null : state.getBlock().getRegistryName().toString();
	}

	/** The block of a 1.21 placed_block criterion (its block_state_property location condition). */
	@Nullable
	private static String placedBlock(JsonObject conditions) {
		if (conditions.has("block")) {
			return block(conditions.get("block").getAsString());
		}
		if (conditions.has("location") && conditions.get("location").isJsonArray()) {
			for (JsonElement condition : conditions.getAsJsonArray("location")) {
				if (condition.isJsonObject() && condition.getAsJsonObject().has("block")) {
					return block(condition.getAsJsonObject().get("block").getAsString());
				}
			}
		}
		return null;
	}

	/** The entity type of a 1.21 entity predicate (a list of conditions or the predicate), as 1.12.2 names it. */
	@Nullable
	private static String entityType(JsonElement entity) {
		if (entity.isJsonArray()) {
			for (JsonElement condition : entity.getAsJsonArray()) {
				if (condition.isJsonObject() && condition.getAsJsonObject().has("predicate")) {
					String found = entityType(condition.getAsJsonObject().get("predicate"));
					if (found != null) {
						return found;
					}
				}
			}
			return null;
		}
		if (!entity.isJsonObject() || !entity.getAsJsonObject().has("type")) {
			return null;
		}
		JsonElement type = entity.getAsJsonObject().get("type");
		String name = type.isJsonPrimitive() ? type.getAsString() : null;
		if (name == null || name.startsWith("#")) {
			return null;
		}
		ResourceLocation id = new ResourceLocation(name);
		return ForgeRegistries.ENTITIES.containsKey(id) ? id.toString() : null;
	}

}
