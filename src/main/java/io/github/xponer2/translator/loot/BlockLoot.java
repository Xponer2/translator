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
package io.github.xponer2.translator.loot;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.xponer2.translator.util.ItemIds;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The 1.21 loot tables of the mods' blocks ({@code data/<mod>/loot_table/blocks/<block>.json}), evaluated for 1.12.2's
 * block drops: pools and their rolls, item / alternatives / group / sequence / tag / loot table entries, the tool
 * conditions (silk touch, shears, a tool tag), fortune bonuses and chances, block state properties, set count, apply
 * bonus, limit count, furnace smelt. 1.12.2 dropped every block as itself: ores without their gems, crops without their
 * produce and seeds, leaves without saplings.
 */
public final class BlockLoot {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator");
	private static final Map<ResourceLocation, Optional<JsonObject>> TABLES = new ConcurrentHashMap<>();

	private BlockLoot() {
	}

	/**
	 * The drops of the block's 1.21 table into {@code drops}; false when it has none (1.12.2's drops then).
	 *
	 * @param table the table's id, null for none (noLootTable: no drops at all)
	 */
	public static boolean drop(@Nullable ResourceLocation table, NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state,
			int fortune, @Nullable net.minecraft.entity.player.EntityPlayer player, Random random) {
		if (table == null) {
			return true;
		}
		JsonObject json = table(table);
		if (json == null) {
			return false;
		}
		try {
			var context = new Context(state, player == null ? ItemStack.EMPTY : player.getHeldItemMainhand(), fortune, random);
			context.table(json, drops::add, 0);
		} catch (RuntimeException e) {
			LOGGER.warn("Couldn't evaluate the loot table {}: {}", table, e.toString());
			return false;
		}
		return true;
	}

	/**
	 * The drops of a mod mob's 1.21 loot table ({@code <mod>:entities/<type>}); false when it has none. Looting,
	 * killed-by-player and on-fire conditions as 1.21 has them.
	 */
	public static boolean entityDrops(@Nullable ResourceLocation table, net.minecraft.entity.EntityLivingBase entity, boolean killedByPlayer, int looting,
			Consumer<ItemStack> out) {
		JsonObject json = table == null ? null : table(table);
		if (json == null) {
			return false;
		}
		try {
			var context = new Context(null, ItemStack.EMPTY, 0, entity.getRNG());
			context.entity = entity;
			context.killedByPlayer = killedByPlayer;
			context.looting = looting;
			context.table(json, out, 0);
		} catch (RuntimeException e) {
			LOGGER.warn("Couldn't evaluate the loot table {}: {}", table, e.toString());
			return false;
		}
		return true;
	}

	@Nullable
	static JsonObject table(ResourceLocation id) {
		return TABLES.computeIfAbsent(id, key -> {
			for (String folder : new String[] {"loot_table", "loot_tables"}) {
				String path = "data/" + key.getNamespace() + "/" + folder + "/" + key.getPath() + ".json";
				var url = BlockLoot.class.getClassLoader().getResource(path);
				if (url == null) {
					continue;
				}
				try (Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
					JsonElement element = new JsonParser().parse(reader);
					if (element.isJsonObject()) {
						return Optional.of(element.getAsJsonObject());
					}
				} catch (Exception e) {
					LOGGER.warn("Couldn't read the loot table {}: {}", path, e.toString());
				}
			}
			return Optional.empty();
		}).orElse(null);
	}

	private static final class Context {
		@Nullable
		final IBlockState state;
		final ItemStack tool;
		final int fortune;
		final Random random;
		@Nullable
		net.minecraft.entity.EntityLivingBase entity;
		boolean killedByPlayer;
		int looting;

		Context(@Nullable IBlockState state, ItemStack tool, int fortune, Random random) {
			this.state = state;
			this.tool = tool;
			this.fortune = fortune;
			this.random = random;
		}

		void table(JsonObject table, Consumer<ItemStack> out, int depth) {
			if (depth > 8) {
				return;
			}
			List<JsonObject> functions = objects(table.get("functions"));
			Consumer<ItemStack> sink = functions.isEmpty() ? out : stack -> out.accept(this.functions(functions, stack));
			for (JsonObject pool : objects(table.get("pools"))) {
				if (!this.conditions(pool.get("conditions"))) {
					continue;
				}
				List<JsonObject> poolFunctions = objects(pool.get("functions"));
				Consumer<ItemStack> poolSink = poolFunctions.isEmpty() ? sink : stack -> sink.accept(this.functions(poolFunctions, stack));
				int rolls = this.number(pool.get("rolls"), 1) + (int) Math.floor(this.numberFloat(pool.get("bonus_rolls"), 0.0F) * 0);
				for (int i = 0; i < rolls; i++) {
					this.roll(objects(pool.get("entries")), poolSink, depth);
				}
			}
		}

		/** One roll of a pool: the expanded entries that pass, one of them by weight. */
		private void roll(List<JsonObject> entries, Consumer<ItemStack> out, int depth) {
			var candidates = new ArrayList<JsonObject>();
			for (JsonObject entry : entries) {
				this.expand(entry, candidates);
			}
			int total = 0;
			for (JsonObject candidate : candidates) {
				total += Math.max(0, candidate.has("weight") ? candidate.get("weight").getAsInt() : 1);
			}
			if (candidates.isEmpty() || total <= 0) {
				return;
			}
			int pick = this.random.nextInt(total);
			for (JsonObject candidate : candidates) {
				pick -= Math.max(0, candidate.has("weight") ? candidate.get("weight").getAsInt() : 1);
				if (pick < 0) {
					this.generate(candidate, out, depth);
					return;
				}
			}
		}

		/** The entries an entry stands for in a roll (composites by their rule). */
		private boolean expand(JsonObject entry, List<JsonObject> out) {
			if (!this.conditions(entry.get("conditions"))) {
				return false;
			}
			switch (type(entry)) {
				case "alternatives":
					for (JsonObject child : objects(entry.get("children"))) {
						if (this.expand(child, out)) {
							return true;
						}
					}
					return false;
				case "group":
					for (JsonObject child : objects(entry.get("children"))) {
						this.expand(child, out);
					}
					return true;
				case "sequence":
					for (JsonObject child : objects(entry.get("children"))) {
						if (!this.expand(child, out)) {
							return true;
						}
					}
					return true;
				default:
					out.add(entry);
					return true;
			}
		}

		private void generate(JsonObject entry, Consumer<ItemStack> out, int depth) {
			List<JsonObject> functions = objects(entry.get("functions"));
			Consumer<ItemStack> sink = functions.isEmpty() ? out : stack -> out.accept(this.functions(functions, stack));
			switch (type(entry)) {
				case "item": {
					ItemStack stack = ItemIds.resultStack(new ResourceLocation(entry.get("name").getAsString()), 1);
					if (!stack.isEmpty()) {
						sink.accept(stack);
					}
					break;
				}
				case "tag": {
					var items = io.github.xponer2.translator.tags.TagManager.itemTag(new ResourceLocation(entry.get("name").getAsString()));
					boolean expand = entry.has("expand") && entry.get("expand").getAsBoolean();
					if (expand && !items.isEmpty()) {
						sink.accept(items.get(this.random.nextInt(items.size())).copy());
					} else {
						for (ItemStack stack : items) {
							sink.accept(stack.copy());
						}
					}
					break;
				}
				case "loot_table": {
					JsonElement value = entry.has("value") ? entry.get("value") : entry.get("name");
					JsonObject table = value == null ? null : value.isJsonObject() ? value.getAsJsonObject() : BlockLoot.table(new ResourceLocation(value.getAsString()));
					if (table != null) {
						this.table(table, sink, depth + 1);
					}
					break;
				}
				default:
					// empty, dynamic (a block entity's contents): nothing here
					break;
			}
		}

		// --- conditions ---------------------------------------------------------------------------------------------

		private boolean conditions(@Nullable JsonElement conditions) {
			for (JsonObject condition : objects(conditions)) {
				if (!this.condition(condition)) {
					return false;
				}
			}
			return true;
		}

		private boolean condition(JsonObject condition) {
			String type = condition.has("condition") ? path(condition.get("condition").getAsString()) : "";
			switch (type) {
				case "inverted":
					return !this.condition(condition.getAsJsonObject("term"));
				case "any_of":
				case "alternative":
					for (JsonObject term : objects(condition.get("terms"))) {
						if (this.condition(term)) {
							return true;
						}
					}
					return false;
				case "all_of":
					return this.conditions(condition.get("terms"));
				case "random_chance":
					return this.random.nextFloat() < this.numberFloat(condition.get("chance"), 1.0F);
				case "random_chance_with_enchanted_bonus": {
					float chance = this.numberFloat(condition.get("unenchanted_chance"), 0.0F);
					if (this.looting > 0 && condition.has("enchanted_chance")) {
						JsonElement enchanted = condition.get("enchanted_chance");
						if (enchanted.isJsonObject() && enchanted.getAsJsonObject().has("base")) {
							JsonObject linear = enchanted.getAsJsonObject();
							chance = linear.get("base").getAsFloat()
									+ (linear.has("per_level_above_first") ? linear.get("per_level_above_first").getAsFloat() : 0.0F) * (this.looting - 1);
						} else {
							chance = this.numberFloat(enchanted, chance);
						}
					}
					return this.random.nextFloat() < chance;
				}
				case "table_bonus": {
					JsonArray chances = condition.getAsJsonArray("chances");
					int level = this.level(condition.get("enchantment").getAsString());
					return chances.size() > 0 && this.random.nextFloat() < chances.get(Math.min(level, chances.size() - 1)).getAsFloat();
				}
				case "match_tool":
					return this.tool(condition.has("predicate") ? condition.getAsJsonObject("predicate") : new JsonObject());
				case "block_state_property":
					return this.properties(condition.has("properties") ? condition.getAsJsonObject("properties") : new JsonObject());
				case "killed_by_player":
					return this.killedByPlayer;
				case "entity_properties": {
					JsonObject predicate = condition.has("predicate") ? condition.getAsJsonObject("predicate") : new JsonObject();
					if (predicate.has("flags") && predicate.getAsJsonObject("flags").has("is_on_fire")) {
						boolean onFire = predicate.getAsJsonObject("flags").get("is_on_fire").getAsBoolean();
						return this.entity != null && this.entity.isBurning() == onFire;
					}
					return true;
				}
				default:
					// survives_explosion, entity / location / weather / time checks: as for a block a player breaks
					return true;
			}
		}

		private boolean tool(JsonObject predicate) {
			if (predicate.has("items")) {
				JsonElement items = predicate.get("items");
				boolean matches = false;
				for (String item : strings(items)) {
					if (item.startsWith("#")) {
						for (ItemStack stack : io.github.xponer2.translator.tags.TagManager.itemTag(new ResourceLocation(item.substring(1)))) {
							matches |= !this.tool.isEmpty() && stack.getItem() == this.tool.getItem();
						}
					} else {
						ItemStack stack = ItemIds.stack(new ResourceLocation(item), 1);
						matches |= !this.tool.isEmpty() && !stack.isEmpty() && stack.getItem() == this.tool.getItem();
					}
				}
				if (!matches) {
					return false;
				}
			}
			var enchantments = new ArrayList<JsonObject>();
			if (predicate.has("predicates") && predicate.getAsJsonObject("predicates").has("minecraft:enchantments")) {
				enchantments.addAll(objects(predicate.getAsJsonObject("predicates").get("minecraft:enchantments")));
			}
			enchantments.addAll(objects(predicate.get("enchantments")));
			for (JsonObject enchantment : enchantments) {
				int level = 0;
				for (String id : strings(enchantment.get("enchantments"))) {
					level = Math.max(level, this.level(id));
				}
				int min = 1;
				int max = Integer.MAX_VALUE;
				if (enchantment.has("levels")) {
					JsonElement levels = enchantment.get("levels");
					if (levels.isJsonPrimitive()) {
						min = max = levels.getAsInt();
					} else {
						JsonObject range = levels.getAsJsonObject();
						min = range.has("min") ? range.get("min").getAsInt() : 1;
						max = range.has("max") ? range.get("max").getAsInt() : Integer.MAX_VALUE;
					}
				}
				if (level < min || level > max) {
					return false;
				}
			}
			return true;
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private boolean properties(JsonObject properties) {
			if (this.state == null) {
				return true;
			}
			for (var entry : properties.entrySet()) {
				IProperty property = null;
				for (IProperty<?> candidate : this.state.getPropertyKeys()) {
					if (candidate.getName().equals(entry.getKey())) {
						property = candidate;
					}
				}
				if (property == null) {
					continue;
				}
				String actual = property.getName(this.state.getValue(property));
				JsonElement expected = entry.getValue();
				if (expected.isJsonPrimitive()) {
					if (!actual.equals(expected.getAsString())) {
						return false;
					}
				} else if (expected.isJsonObject()) {
					try {
						int value = Integer.parseInt(actual);
						JsonObject range = expected.getAsJsonObject();
						if (range.has("min") && value < range.get("min").getAsInt() || range.has("max") && value > range.get("max").getAsInt()) {
							return false;
						}
					} catch (NumberFormatException e) {
						return false;
					}
				}
			}
			return true;
		}

		private int level(String enchantment) {
			if (this.tool.isEmpty()) {
				return 0;
			}
			if (enchantment.endsWith("fortune")) {
				return Math.max(this.fortune, EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.FORTUNE, this.tool));
			}
			Enchantment found = Enchantment.getEnchantmentByLocation(enchantment.startsWith("#") ? enchantment.substring(1) : enchantment);
			return found == null ? 0 : EnchantmentHelper.getEnchantmentLevel(found, this.tool);
		}

		// --- functions ----------------------------------------------------------------------------------------------

		private ItemStack functions(List<JsonObject> functions, ItemStack stack) {
			ItemStack current = stack;
			for (JsonObject function : functions) {
				if (current.isEmpty() || !this.conditions(function.get("conditions"))) {
					continue;
				}
				String type = function.has("function") ? path(function.get("function").getAsString()) : "";
				switch (type) {
					case "set_count": {
						int count = this.number(function.get("count"), 1);
						boolean add = function.has("add") && function.get("add").getAsBoolean();
						current.setCount(Math.max(0, Math.min(current.getMaxStackSize(), add ? current.getCount() + count : count)));
						break;
					}
					case "apply_bonus":
						current.setCount(Math.min(current.getMaxStackSize(), this.bonus(function, current.getCount())));
						break;
					case "limit_count":
						if (function.has("limit")) {
							JsonElement limit = function.get("limit");
							int min = limit.isJsonObject() && limit.getAsJsonObject().has("min") ? this.number(limit.getAsJsonObject().get("min"), 0) : 0;
							int max = limit.isJsonObject() && limit.getAsJsonObject().has("max") ? this.number(limit.getAsJsonObject().get("max"), 64)
									: limit.isJsonPrimitive() ? limit.getAsInt() : 64;
							current.setCount(Math.max(min, Math.min(max, current.getCount())));
						}
						break;
					case "enchanted_count_increase":
					case "looting_enchant":
						if (this.looting > 0) {
							float per = this.numberFloat(function.get("count"), 1.0F);
							int extra = Math.round(this.looting * per * this.random.nextFloat());
							int limit = function.has("limit") ? function.get("limit").getAsInt() : 0;
							int count = current.getCount() + extra;
							current.setCount(Math.min(current.getMaxStackSize(), limit > 0 ? Math.min(limit, count) : count));
						}
						break;
					case "furnace_smelt": {
						ItemStack smelted = FurnaceRecipes.instance().getSmeltingResult(current);
						if (!smelted.isEmpty()) {
							int count = current.getCount();
							current = smelted.copy();
							current.setCount(count);
						}
						break;
					}
					default:
						// explosion decay, names, components, block entity data: as they are
						break;
				}
			}
			return current;
		}

		private int bonus(JsonObject function, int count) {
			int level = this.level(function.has("enchantment") ? function.get("enchantment").getAsString() : "minecraft:fortune");
			String formula = function.has("formula") ? path(function.get("formula").getAsString()) : "";
			JsonObject parameters = function.has("parameters") ? function.getAsJsonObject("parameters") : new JsonObject();
			switch (formula) {
				case "ore_drops":
					if (level > 0) {
						int multiplier = this.random.nextInt(level + 2) - 1;
						return count * (Math.max(0, multiplier) + 1);
					}
					return count;
				case "uniform_bonus_count":
					return count + this.random.nextInt((parameters.has("bonusMultiplier") ? parameters.get("bonusMultiplier").getAsInt() : 1) * level + 1);
				case "binomial_with_bonus_count": {
					int extra = parameters.has("extra") ? parameters.get("extra").getAsInt() : 0;
					float probability = parameters.has("probability") ? parameters.get("probability").getAsFloat() : 0.5F;
					for (int i = 0; i < level + extra; i++) {
						if (this.random.nextFloat() < probability) {
							count++;
						}
					}
					return count;
				}
				default:
					return count;
			}
		}

		// --- numbers ------------------------------------------------------------------------------------------------

		private int number(@Nullable JsonElement value, int fallback) {
			return Math.round(this.numberFloat(value, fallback));
		}

		private float numberFloat(@Nullable JsonElement value, float fallback) {
			if (value == null || value.isJsonNull()) {
				return fallback;
			}
			if (value.isJsonPrimitive()) {
				return value.getAsFloat();
			}
			JsonObject object = value.getAsJsonObject();
			String type = object.has("type") ? path(object.get("type").getAsString()) : object.has("min") ? "uniform" : "constant";
			switch (type) {
				case "uniform": {
					float min = this.numberFloat(object.get("min"), 0.0F);
					float max = this.numberFloat(object.get("max"), min);
					return min + this.random.nextFloat() * (max - min);
				}
				case "binomial": {
					int n = this.number(object.get("n"), 0);
					float p = this.numberFloat(object.get("p"), 0.5F);
					int hits = 0;
					for (int i = 0; i < n; i++) {
						if (this.random.nextFloat() < p) {
							hits++;
						}
					}
					return hits;
				}
				case "constant":
					return this.numberFloat(object.get("value"), fallback);
				default:
					return fallback;
			}
		}
	}

	private static String type(JsonObject entry) {
		return entry.has("type") ? path(entry.get("type").getAsString()) : "item";
	}

	private static String path(String id) {
		int colon = id.indexOf(':');
		return colon < 0 ? id : id.substring(colon + 1);
	}

	private static List<JsonObject> objects(@Nullable JsonElement element) {
		var out = new ArrayList<JsonObject>();
		if (element != null && element.isJsonArray()) {
			for (JsonElement item : element.getAsJsonArray()) {
				if (item.isJsonObject()) {
					out.add(item.getAsJsonObject());
				}
			}
		} else if (element != null && element.isJsonObject()) {
			out.add(element.getAsJsonObject());
		}
		return out;
	}

	private static List<String> strings(@Nullable JsonElement element) {
		var out = new ArrayList<String>();
		if (element == null) {
			return out;
		}
		if (element.isJsonArray()) {
			for (JsonElement item : element.getAsJsonArray()) {
				out.add(item.getAsString());
			}
		} else if (element.isJsonPrimitive()) {
			out.add(element.getAsString());
		}
		return out;
	}

	/** The table id of a block: lootFrom's block's, none for noLootTable, else blocks/<id>. */
	@Nullable
	public static ResourceLocation tableOf(Block block, @Nullable io.github.xponer2.translator.block.BlockProperties properties) {
		if (properties != null && properties.hasNoLootTable()) {
			return null;
		}
		Block source = block;
		if (properties != null && properties.lootSource() != null) {
			try {
				Object other = properties.lootSource().get();
				if (other instanceof Block otherBlock) {
					source = otherBlock;
				}
			} catch (RuntimeException e) {
				// its own table
			}
		}
		ResourceLocation id = source.getRegistryName();
		return id == null ? new ResourceLocation("minecraft", "empty") : new ResourceLocation(id.getNamespace(), "blocks/" + id.getPath());
	}
}
