/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Writes {@code data/translator/flattening.json} (1.21 vanilla id -> 1.12.2 id + meta) from Minecraft's own world
 * upgrade: the 1.13 flattening table of block states ({@code BlockStateData}, data version 1450), the item flattening
 * ({@code ItemStackTheFlatteningFix}, from 1.12.2's data version 1343) and every later rename fix up to 1.21.1, run
 * through the game's {@code DataFixer}. The idea comes from Legacy Connector (MIT, Kyle Pitcock), which uses the same
 * fixer the other way round. Blocks 1.12.2 tells apart only by their block entity (flower pot content, bed and banner
 * colour, skull type) and the upper halves of double plants take the chunk upgrade's own maps
 * ({@code ChunkPalettedStorageFix}): the flattening table has placeholders for them.
 *
 * <p>Runs against the official obfuscated 1.21.1 client jar: the few game members it needs are found through the
 * Mojang mappings and called by reflection; the fixer runs on JSON ({@code JsonOps}), so nothing of the game is
 * compiled against. The 1.12.2 registry names come from a deobfuscated 1.12.2 jar ({@code Block.registerBlocks},
 * {@code Item.registerItems}, read with ASM).
 *
 * <p>Entries already in the output file are the hand-made table: an entry that differs from the generated one is kept
 * as it is, a {@code similar} entry is kept where no exact 1.12.2 counterpart exists. Every difference goes to the
 * report.
 *
 * <p>Fields of an entry: {@code id}, {@code meta}: the 1.12.2 item (and block); {@code block}: the 1.12.2 block when
 * it is not {@code id} ({@code repeater} is the item {@code repeater} but the block {@code unpowered_repeater});
 * {@code blockMetas}: the 1.12.2 block metas that are this 1.21 block when they are not just {@code meta}
 * ({@code [0, 4, 8]}: an oak log on each axis, {@code "all"}: every state of the block); {@code item: false}: 1.21 has
 * no item of that id (a wall torch is the 1.12.2 torch, but the torch item stays a torch); {@code similar: true}:
 * 1.12.2 has no such thing, a similar item for recipe results only.
 *
 * <p>Usage (run.py builds the class path): {@code java -cp <client jar + 1.21.1 libraries + asm> GenFlattening.java
 * --client-jar <jar> --mappings <txt> --legacy-jar <1.12.2 jar> --out <flattening.json> --report <md>}
 */
public final class GenFlattening {
	/** 1.12.2's data version: item stacks are upgraded from here. */
	private static final int LEGACY_ITEM_VERSION = 1343;
	/** The data version of the flattened block states of BlockStateData (BlockStateFlatteningFix is schema 1450). */
	private static final int FLATTENED_BLOCK_VERSION = 1450;
	private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
	/** 1.12.2 items whose 1.21 counterpart is in the stack's NBT, not in its id and damage. */
	private static final Set<String> NBT_ITEMS = Set.of("minecraft:spawn_egg");

	private GenFlattening() {
	}

	/** A 1.12.2 item with its damage, or a block with its meta. */
	record Legacy(String name, int meta) {
		@Override
		public String toString() {
			return name + ":" + meta;
		}
	}

	public static void main(String[] args) throws Exception {
		Map<String, String> opt = options(args);
		Mappings mappings = Mappings.read(Path.of(require(opt, "mappings")));
		Names112 old = Names112.read(Path.of(require(opt, "legacy-jar")));
		Assets121 assets = Assets121.read(Path.of(require(opt, "client-jar")));
		Path out = Path.of(require(opt, "out"));
		Path report = Path.of(require(opt, "report"));

		Fixer fixer = new Fixer(mappings);
		System.out.println("1.21.1 data version " + fixer.version + "; 1.12.2: " + old.blocksById.size() + " blocks, "
				+ old.itemNames.size() + " items");

		// ---- blocks: every registered 1.12.2 state of the flattening table, upgraded to 1.21.1
		Map<String, Map<String, TreeSet<Integer>>> blockSide = new TreeMap<>(); // 1.21 block -> 1.12.2 block -> metas
		Map<String, TreeSet<Integer>> allMetas = new HashMap<>(); // 1.12.2 block -> its registered metas
		List<String> warnings = new ArrayList<>();
		Dynamic<?>[] map = (Dynamic<?>[]) mappings.staticField("net.minecraft.util.datafix.fixes.BlockStateData", "MAP");
		@SuppressWarnings("unchecked")
		Object2IntMap<Dynamic<?>> idByOld = (Object2IntMap<Dynamic<?>>) mappings.staticField("net.minecraft.util.datafix.fixes.BlockStateData", "ID_BY_OLD");
		Map<Integer, String> keyNames = new TreeMap<>();
		for (Object2IntMap.Entry<Dynamic<?>> e : idByOld.object2IntEntrySet()) {
			keyNames.putIfAbsent(e.getIntValue(), e.getKey().get("Name").asString(""));
		}
		for (var e : keyNames.entrySet()) {
			int key = e.getKey();
			String name = old.blocksById.get(key >> 4);
			if (name == null) {
				warnings.add("block id " + (key >> 4) + " (" + e.getValue() + ") is not a 1.12.2 block");
				continue;
			}
			if (!name.equals(e.getValue())) {
				warnings.add("block id " + (key >> 4) + ": the flattening table says " + e.getValue() + ", 1.12.2 " + name);
			}
			allMetas.computeIfAbsent(name, k -> new TreeSet<>()).add(key & 15);
			String modern = fixer.block(map[key]).get("Name").asString("");
			// %%FILTER_ME%%: a skull, whose 1.21 block the chunk upgrade takes from the block entity (below)
			if (!ID.matcher(modern).matches() || modern.equals("minecraft:air") && key != 0) {
				continue;
			}
			blockSide.computeIfAbsent(modern, k -> new TreeMap<>()).computeIfAbsent(name, k -> new TreeSet<>()).add(key & 15);
		}

		// ---- 1.12.2 blocks whose 1.21 block is in the block entity (pot content, bed and banner colour, skull type):
		// the flattening table gives all their states to a placeholder (potted_cactus, red_bed, white_banner), the chunk
		// upgrade picks the block from the block entity with these maps; each such 1.21 block is the 1.12.2 block's states
		String chunkFix = "net.minecraft.util.datafix.fixes.ChunkPalettedStorageFix";
		Map<String, Map<String, TreeSet<Integer>>> entitySide = new TreeMap<>();
		for (String field : List.of("FLOWER_POT_MAP", "BED_BLOCK_MAP", "BANNER_BLOCK_MAP", "SKULL_MAP")) {
			@SuppressWarnings("unchecked")
			Map<String, Dynamic<?>> variants = (Map<String, Dynamic<?>>) mappings.staticField(chunkFix, field);
			for (Dynamic<?> state : variants.values()) {
				Dynamic<JsonElement> modern = fixer.block(state);
				String name = modern.get("Name").asString("");
				String facing = modern.get("Properties").get("facing").asString("");
				String legacy;
				Set<Integer> metas;
				switch (field) {
					case "FLOWER_POT_MAP" -> {
						legacy = "minecraft:flower_pot";
						metas = allMetas.get(legacy);
					}
					case "BED_BLOCK_MAP" -> {
						legacy = "minecraft:bed";
						metas = allMetas.get(legacy);
					}
					case "BANNER_BLOCK_MAP" -> {
						legacy = facing.isEmpty() ? "minecraft:standing_banner" : "minecraft:wall_banner";
						metas = allMetas.get(legacy);
					}
					default -> {
						// 1.12.2 skull meta: facing (up = on the floor, the rotation is in the block entity) + 8 no-drop
						legacy = "minecraft:skull";
						int side = List.of("down", "up", "north", "south", "west", "east").indexOf(facing.isEmpty() ? "up" : facing);
						metas = Set.of(side, side + 8);
					}
				}
				entitySide.computeIfAbsent(name, k -> new TreeMap<>()).computeIfAbsent(legacy, k -> new TreeSet<>()).addAll(metas);
			}
		}
		// the maps leave out the placeholder's own variant (red bed, white banner), the flattening table has it
		Set<String> entityBlocks = new HashSet<>();
		entitySide.values().forEach(m -> entityBlocks.addAll(m.keySet()));
		entitySide.forEach((name, m) -> m.forEach((legacy, metas) ->
				blockSide.computeIfAbsent(name, k -> new TreeMap<>()).computeIfAbsent(legacy, k -> new TreeSet<>()).addAll(metas)));
		// the upper half of a 1.12.2 double plant is the plant below it (the table says peony for every one of them)
		for (String field : List.of("UPPER_SUNFLOWER", "UPPER_LILAC", "UPPER_TALL_GRASS", "UPPER_LARGE_FERN", "UPPER_ROSE_BUSH", "UPPER_PEONY")) {
			String name = fixer.block((Dynamic<?>) mappings.staticField(chunkFix, field)).get("Name").asString("");
			allMetas.get("minecraft:double_plant").stream().filter(m -> m >= 8).forEach(m ->
					blockSide.computeIfAbsent(name, k -> new TreeMap<>()).computeIfAbsent("minecraft:double_plant", k -> new TreeSet<>()).add(m));
		}

		// ---- items: every 1.12.2 item (damage values of the flattening table), upgraded to 1.21.1
		@SuppressWarnings("unchecked")
		Map<String, String> itemMap = (Map<String, String>) mappings.staticField("net.minecraft.util.datafix.fixes.ItemStackTheFlatteningFix", "MAP");
		@SuppressWarnings("unchecked")
		Set<String> itemIds = (Set<String>) mappings.staticField("net.minecraft.util.datafix.fixes.ItemStackTheFlatteningFix", "IDS");
		Map<String, TreeSet<Integer>> damages = new HashMap<>();
		for (String key : itemMap.keySet()) {
			int dot = key.lastIndexOf('.');
			damages.computeIfAbsent(key.substring(0, dot), k -> new TreeSet<>()).add(Integer.parseInt(key.substring(dot + 1)));
		}
		for (String name : damages.keySet()) {
			if (!old.itemNames.contains(name)) {
				warnings.add("the item flattening knows " + name + ", 1.12.2 has no such item");
			}
		}
		Map<String, List<Legacy>> itemSide = new TreeMap<>(); // 1.21 item -> 1.12.2 items
		Map<Legacy, String> upgraded = new HashMap<>();
		for (String name : old.itemNames) {
			if (NBT_ITEMS.contains(name) || name.equals("minecraft:air")) {
				continue;
			}
			for (int damage : itemIds.contains(name) ? damages.get(name) : Set.of(0)) {
				String modern = fixer.item(name, damage);
				if (modern.isEmpty() || modern.equals("minecraft:air")) {
					warnings.add("item " + name + ":" + damage + " upgrades to nothing");
					continue;
				}
				Legacy legacy = new Legacy(name, damage);
				upgraded.put(legacy, modern);
				itemSide.computeIfAbsent(modern, k -> new ArrayList<>()).add(legacy);
			}
		}

		// ---- one entry per 1.21 id that isn't the 1.12.2 thing of the same name
		Comparator<Legacy> order = Comparator.comparingInt((Legacy l) -> old.order(l.name())).thenComparingInt(Legacy::meta);
		Set<String> modernIds = new TreeSet<>(blockSide.keySet());
		modernIds.addAll(itemSide.keySet());
		Map<String, JsonObject> generated = new TreeMap<>();
		Map<String, String> notes = new HashMap<>();
		for (String id : modernIds) {
			// the item: of the same name, else one of its own (deadbush rather than tallgrass 0), else the first by id
			List<Legacy> items = new ArrayList<>(itemSide.getOrDefault(id, List.of()));
			items.sort(Comparator.comparing((Legacy l) -> !l.name().equals(id))
					.thenComparing(l -> damages.getOrDefault(l.name(), new TreeSet<>()).size() > 1).thenComparing(order));
			Legacy item = items.isEmpty() ? null : items.get(0);
			// the block: the item's, else one with an item (redstone_torch rather than unlit_redstone_torch), else of the
			// same name, else the first by id
			Map<String, TreeSet<Integer>> blocks = blockSide.getOrDefault(id, Map.of());
			String block = blocks.keySet().stream()
					.min(Comparator.comparing((String n) -> !(item != null && n.equals(item.name())))
							.thenComparing(n -> !old.itemNames.contains(n))
							.thenComparing(n -> !n.equals(id)).thenComparingInt(old::order))
					.orElse(null);
			TreeSet<Integer> metas = block == null ? null : blocks.get(block);
			if (block != null && entityBlocks.contains(block)) {
				notes.put(id, "1.12.2 keeps it in the block entity of " + block.replace("minecraft:", ""));
			}
			boolean itemSame = item == null || item.name().equals(id) && item.meta() == 0 && !itemIds.contains(id);
			boolean blockSame = block == null || block.equals(id) && metas.equals(allMetas.get(block));
			// a 1.12.2 item or block of this name that is something else in 1.21
			String asItem = upgraded.get(new Legacy(id, 0));
			boolean reused = item == null && asItem != null && !asItem.equals(id) || block == null && allMetas.containsKey(id);
			if (itemSame && blockSame && !reused) {
				continue;
			}
			JsonObject entry = new JsonObject();
			String legacyId = item != null ? item.name() : block;
			int meta = item != null ? item.meta() : metas.first();
			entry.addProperty("id", legacyId);
			entry.addProperty("meta", meta);
			if (block != null) {
				if (!block.equals(legacyId)) {
					entry.addProperty("block", block);
				}
				if (metas.equals(allMetas.get(block)) && metas.size() > 1) {
					entry.addProperty("blockMetas", "all");
				} else if (!(block.equals(legacyId) && metas.size() == 1 && metas.first() == meta)) {
					JsonArray array = new JsonArray();
					metas.forEach(array::add);
					entry.add("blockMetas", array);
				}
			}
			if (item == null && !assets.items.contains(id)) {
				entry.addProperty("item", false);
			}
			generated.put(id, entry);
		}

		// ---- merge with the hand-made table already in the file
		Map<String, JsonObject> hand = new TreeMap<>();
		if (Files.isRegularFile(out)) {
			JsonObject json = JsonParser.parseString(Files.readString(out)).getAsJsonObject();
			for (var e : json.entrySet()) {
				hand.put(e.getKey(), e.getValue().getAsJsonObject());
			}
		}
		Map<String, JsonObject> result = new TreeMap<>(generated);
		List<String[]> kept = new ArrayList<>();      // id, hand, generated
		List<String[]> replaced = new ArrayList<>();  // similar replaced by an exact entry
		List<String[]> handOnly = new ArrayList<>();
		int same = 0;
		for (var e : hand.entrySet()) {
			String id = e.getKey();
			JsonObject h = e.getValue();
			JsonObject g = generated.get(id);
			boolean similar = h.has("similar") && h.get("similar").getAsBoolean();
			if (g == null) {
				result.put(id, h);
				if (!similar) {
					handOnly.add(new String[] {id, describe(h), why(id, h, upgraded, blockSide, old)});
				}
			} else if (similar) {
				replaced.add(new String[] {id, describe(h), describe(g)});
			} else if (h.get("id").getAsString().equals(g.get("id").getAsString()) && h.get("meta").getAsInt() == g.get("meta").getAsInt()) {
				same++;
			} else {
				result.put(id, h);
				kept.add(new String[] {id, describe(h), describe(g)});
			}
		}
		int similarCount = (int) result.values().stream().filter(o -> o.has("similar") && o.get("similar").getAsBoolean()).count();

		JsonObject json = new JsonObject();
		result.forEach(json::add);
		Files.writeString(out, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json) + "\n", StandardCharsets.UTF_8);

		// ---- report
		StringBuilder md = new StringBuilder();
		md.append("# flattening.json: generated vs hand-made\n\n");
		md.append("Generated by tools/gen_flattening from the 1.21.1 DataFixer (data version ").append(fixer.version)
				.append(") and the 1.12.2 registry names.\n\n");
		md.append("- generated entries: ").append(generated.size()).append('\n');
		md.append("- hand entries: ").append(hand.size()).append(" (").append(hand.size() - (int) hand.values().stream()
				.filter(o -> o.has("similar") && o.get("similar").getAsBoolean()).count()).append(" exact, ")
				.append((int) hand.values().stream().filter(o -> o.has("similar") && o.get("similar").getAsBoolean()).count())
				.append(" similar)\n");
		md.append("- hand entries the generator gives the same id and meta: ").append(same).append('\n');
		md.append("- hand entries kept where the generator differs: ").append(kept.size()).append('\n');
		md.append("- similar hand entries replaced by an exact counterpart: ").append(replaced.size()).append('\n');
		md.append("- exact hand entries the generator has no entry for (kept): ").append(handOnly.size()).append('\n');
		md.append("- new entries: ").append(generated.keySet().stream().filter(k -> !hand.containsKey(k)).count()).append('\n');
		md.append("- output: ").append(result.size()).append(" entries (").append(similarCount).append(" similar)\n\n");
		table(md, "Hand entries kept (the generator differs)", new String[] {"1.21 id", "hand (kept)", "generated"}, kept);
		table(md, "Similar entries replaced by an exact 1.12.2 counterpart", new String[] {"1.21 id", "hand (similar)", "generated (used)"}, replaced);
		table(md, "Exact hand entries the generator has no entry for (kept)", new String[] {"1.21 id", "hand", "what the fixer says"}, handOnly);
		List<String[]> added = new ArrayList<>();
		for (var e : generated.entrySet()) {
			if (!hand.containsKey(e.getKey())) {
				added.add(new String[] {e.getKey(), describe(e.getValue()), notes.getOrDefault(e.getKey(), "")});
			}
		}
		table(md, "New entries", new String[] {"1.21 id", "generated", "note"}, added);
		List<String[]> reusedNames = new ArrayList<>();
		for (String name : new TreeSet<>(old.allNames())) {
			String asItem = upgraded.get(new Legacy(name, 0));
			String asBlock = entityBlocks.contains(name) ? "(by its block entity)" : null;
			for (var e : entityBlocks.contains(name) ? Map.<String, Map<String, TreeSet<Integer>>>of().entrySet() : blockSide.entrySet()) {
				if (e.getValue().containsKey(name) && e.getValue().get(name).contains(allMetas.get(name).first())) {
					asBlock = e.getKey();
				}
			}
			boolean modernHasIt = assets.items.contains(name) || assets.blocks.contains(name);
			if (modernHasIt && (asItem != null && !asItem.equals(name) || asBlock != null && !asBlock.equals(name))) {
				reusedNames.add(new String[] {name, "item -> " + asItem + ", block -> " + asBlock,
						result.containsKey(name) ? describe(result.get(name)) : "(no entry: the 1.12.2 " + name + " is used)"});
			}
		}
		table(md, "1.21 ids that are 1.12.2 names of something else", new String[] {"name", "1.12.2 meaning in 1.21", "entry"}, reusedNames);
		if (!warnings.isEmpty()) {
			md.append("## Warnings\n\n");
			warnings.forEach(w -> md.append("- ").append(w).append('\n'));
			md.append('\n');
		}
		Files.createDirectories(report.toAbsolutePath().getParent());
		Files.writeString(report, md.toString(), StandardCharsets.UTF_8);
		System.out.println("wrote " + out + " (" + result.size() + " entries: " + generated.size() + " generated, "
				+ kept.size() + " hand entries kept over the generated ones, " + handOnly.size() + " hand-only, "
				+ similarCount + " similar) and " + report);
	}

	private static String why(String id, JsonObject hand, Map<Legacy, String> upgraded, Map<String, Map<String, TreeSet<Integer>>> blockSide, Names112 old) {
		Legacy legacy = new Legacy(hand.get("id").getAsString(), hand.get("meta").getAsInt());
		String item = upgraded.get(legacy);
		String block = null;
		for (var e : blockSide.entrySet()) {
			var metas = e.getValue().get(legacy.name());
			if (metas != null && metas.contains(legacy.meta())) {
				block = e.getKey();
			}
		}
		if (!old.itemNames.contains(legacy.name()) && !old.blockIds.containsKey(legacy.name())) {
			return legacy.name() + " is not a 1.12.2 id";
		}
		if (item == null && block == null) {
			return legacy + " is no 1.12.2 item or block state the fixer knows";
		}
		if (Objects.equals(item, id) || Objects.equals(block, id)) {
			return "the same as the 1.12.2 one (no entry needed)";
		}
		return legacy + " is " + (item != null ? "item " + item : "") + (item != null && block != null ? ", " : "")
				+ (block != null ? "block " + block : "") + " in 1.21";
	}

	private static String describe(JsonObject entry) {
		StringBuilder s = new StringBuilder(entry.get("id").getAsString().replace("minecraft:", "")).append(':').append(entry.get("meta").getAsInt());
		for (var e : entry.entrySet()) {
			if (!e.getKey().equals("id") && !e.getKey().equals("meta")) {
				s.append(' ').append(e.getKey()).append('=').append(e.getValue().toString().replace("\"", "").replace("minecraft:", ""));
			}
		}
		return s.toString();
	}

	private static void table(StringBuilder md, String title, String[] header, List<String[]> rows) {
		md.append("## ").append(title).append(" (").append(rows.size()).append(")\n\n");
		if (rows.isEmpty()) {
			md.append("None.\n\n");
			return;
		}
		md.append("| ").append(String.join(" | ", header)).append(" |\n");
		md.append("|").append("---|".repeat(header.length)).append('\n');
		for (String[] row : rows) {
			md.append("| ").append(String.join(" | ", row).replace("minecraft:", "")).append(" |\n");
		}
		md.append('\n');
	}

	private static Map<String, String> options(String[] args) {
		Map<String, String> opt = new HashMap<>();
		for (int i = 0; i + 1 < args.length; i += 2) {
			if (!args[i].startsWith("--")) {
				throw new IllegalArgumentException("expected --option value, got " + args[i]);
			}
			opt.put(args[i].substring(2), args[i + 1]);
		}
		return opt;
	}

	private static String require(Map<String, String> opt, String name) {
		String value = opt.get(name);
		if (value == null) {
			throw new IllegalArgumentException("missing --" + name);
		}
		return value;
	}

	/** The game's DataFixer, called by reflection through the Mojang mappings, on JSON. */
	static final class Fixer {
		final int version;
		private final DataFixer fixer;
		private final DSL.TypeReference blockState;
		private final DSL.TypeReference itemStack;

		Fixer(Mappings mappings) throws ReflectiveOperationException {
			version = (Integer) mappings.staticField("net.minecraft.SharedConstants", "WORLD_VERSION");
			// DataFixers asks for the game version (version.json of the client jar)
			mappings.invokeStatic("net.minecraft.SharedConstants", "tryDetectVersion");
			fixer =(DataFixer) mappings.invokeStatic("net.minecraft.util.datafix.DataFixers", "getDataFixer");
			blockState = (DSL.TypeReference) mappings.staticField("net.minecraft.util.datafix.fixes.References", "BLOCK_STATE");
			itemStack = (DSL.TypeReference) mappings.staticField("net.minecraft.util.datafix.fixes.References", "ITEM_STACK");
		}

		Dynamic<JsonElement> block(Dynamic<?> flattened) {
			return fixer.update(blockState, flattened.convert(JsonOps.INSTANCE), FLATTENED_BLOCK_VERSION, version);
		}

		String item(String name, int damage) {
			JsonObject stack = new JsonObject();
			stack.addProperty("id", name);
			stack.addProperty("Count", (byte) 1);
			stack.addProperty("Damage", (short) damage);
			Dynamic<JsonElement> out = fixer.update(itemStack, new Dynamic<>(JsonOps.INSTANCE, stack), LEGACY_ITEM_VERSION, version);
			return out.get("id").asString("");
		}
	}

	/** Mojang's ProGuard mappings: named -> obfuscated classes, fields and methods. */
	static final class Mappings {
		private static final Pattern MEMBER = Pattern.compile("^\\s+(?:\\d+:\\d+:)?(\\S+) ([^\\s(]+)(\\([^)]*\\))?(?::\\d+:\\d+)? -> (\\S+)$");
		private final Map<String, String> classes = new HashMap<>();
		private final Map<String, Map<String, String>> fields = new HashMap<>();
		private final Map<String, Map<String, String>> methods = new HashMap<>();

		static Mappings read(Path file) throws IOException {
			Mappings m = new Mappings();
			String current = null;
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				if (line.isEmpty() || line.startsWith("#") || line.trim().startsWith("#")) {
					continue;
				}
				if (!line.startsWith(" ")) {
					int arrow = line.indexOf(" -> ");
					current = line.substring(0, arrow);
					m.classes.put(current, line.substring(arrow + 4, line.length() - 1));
					continue;
				}
				Matcher matcher = MEMBER.matcher(line);
				if (current == null || !matcher.matches()) {
					continue;
				}
				if (matcher.group(3) == null) {
					m.fields.computeIfAbsent(current, k -> new HashMap<>()).put(matcher.group(2), matcher.group(4));
				} else {
					m.methods.computeIfAbsent(current, k -> new HashMap<>()).put(matcher.group(2) + matcher.group(3), matcher.group(4));
				}
			}
			return m;
		}

		Class<?> type(String named) throws ClassNotFoundException {
			return Class.forName(classes.getOrDefault(named, named), true, GenFlattening.class.getClassLoader());
		}

		Object staticField(String owner, String name) throws ReflectiveOperationException {
			String obf = fields.getOrDefault(owner, Map.of()).getOrDefault(name, name);
			Field field = type(owner).getDeclaredField(obf);
			field.setAccessible(true);
			return field.get(null);
		}

		Object invokeStatic(String owner, String name) throws ReflectiveOperationException {
			String obf = methods.getOrDefault(owner, Map.of()).getOrDefault(name + "()", name);
			Method method = type(owner).getDeclaredMethod(obf);
			method.setAccessible(true);
			return method.invoke(null);
		}
	}

	/** The ids 1.21.1 has, from its client jar: items by their item model, blocks by their block states file. */
	static final class Assets121 {
		final Set<String> items = new HashSet<>();
		final Set<String> blocks = new HashSet<>();

		static Assets121 read(Path jar) throws IOException {
			Assets121 a = new Assets121();
			try (ZipFile zip = new ZipFile(jar.toFile())) {
				for (var it = zip.entries(); it.hasMoreElements(); ) {
					String name = it.nextElement().getName();
					if (name.startsWith("assets/minecraft/models/item/") && name.endsWith(".json")) {
						a.items.add("minecraft:" + name.substring(29, name.length() - 5));
					} else if (name.startsWith("assets/minecraft/blockstates/") && name.endsWith(".json")) {
						a.blocks.add("minecraft:" + name.substring(29, name.length() - 5));
					}
				}
			}
			if (a.items.isEmpty() || a.blocks.isEmpty()) {
				throw new IOException(jar + " has no item models or block states: not a 1.21.1 client jar");
			}
			return a;
		}
	}

	/** The 1.12.2 block and item registry names, read from Block.registerBlocks and Item.registerItems. */
	static final class Names112 {
		final Map<Integer, String> blocksById = new TreeMap<>();
		final Map<String, Integer> blockIds = new HashMap<>();
		/** every 1.12.2 item: the block items in block id order, then the other items in item id order */
		final Set<String> itemNames = new LinkedHashSet<>();
		private final Map<String, Integer> itemIds = new HashMap<>();

		int order(String name) {
			Integer id = blockIds.get(name);
			if (id == null) {
				id = itemIds.get(name);
			}
			return id == null ? Integer.MAX_VALUE : id;
		}

		Set<String> allNames() {
			Set<String> all = new HashSet<>(blockIds.keySet());
			all.addAll(itemNames);
			return all;
		}

		static Names112 read(Path jar) throws IOException {
			Names112 n = new Names112();
			try (ZipFile zip = new ZipFile(jar.toFile())) {
				MethodNode registerBlocks = method(zip, "net/minecraft/block/Block", "registerBlocks");
				int blockCalls = idNames(registerBlocks, "registerBlock", (id, name) -> {
					n.blocksById.put(id, "minecraft:" + name);
					n.blockIds.put("minecraft:" + name, id);
				});
				n.blocksById.putIfAbsent(0, "minecraft:air");
				n.blockIds.putIfAbsent("minecraft:air", 0);
				if (blockCalls != n.blocksById.size() - 1) {
					throw new IOException("read " + (n.blocksById.size() - 1) + " block names for " + blockCalls + " registerBlock calls");
				}
				// Blocks.STONE = getRegisteredBlock("stone")
				Map<String, String> fieldNames = new HashMap<>();
				String last = null;
				for (AbstractInsnNode insn : method(zip, "net/minecraft/init/Blocks", "<clinit>").instructions) {
					if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
						last = s;
					} else if (insn instanceof FieldInsnNode f && f.getOpcode() == Opcodes.PUTSTATIC && last != null) {
						fieldNames.put(f.name, "minecraft:" + last);
						last = null;
					}
				}
				MethodNode registerItems = method(zip, "net/minecraft/item/Item", "registerItems");
				Map<Integer, String> items = new TreeMap<>();
				int itemCalls = idNames(registerItems, "registerItem", (id, name) -> items.put(id, "minecraft:" + name));
				if (itemCalls != items.size()) {
					throw new IOException("read " + items.size() + " item names for " + itemCalls + " registerItem calls");
				}
				// registerItemBlock(Blocks.X[, item]): the block is the first Blocks field read since the last registration
				Set<String> blockItems = new HashSet<>();
				String block = null;
				for (AbstractInsnNode insn : registerItems.instructions) {
					if (insn instanceof FieldInsnNode f && f.getOpcode() == Opcodes.GETSTATIC && f.owner.equals("net/minecraft/init/Blocks") && block == null) {
						block = f.name;
					} else if (insn instanceof MethodInsnNode m && m.getOpcode() == Opcodes.INVOKESTATIC && m.owner.equals("net/minecraft/item/Item") && m.name.startsWith("register")) {
						if (m.name.equals("registerItemBlock")) {
							String name = fieldNames.get(block);
							if (name == null) {
								throw new IOException("registerItemBlock of an unknown block field " + block);
							}
							blockItems.add(name);
						}
						block = null;
					}
				}
				n.blocksById.values().stream().filter(blockItems::contains).forEach(n.itemNames::add);
				items.forEach((id, name) -> {
					n.itemNames.add(name);
					n.itemIds.put(name, id);
				});
			}
			return n;
		}

		interface IdName {
			void accept(int id, String name);
		}

		/** {@code register*(int id, "name", ...)}: an int constant right before a string constant; returns the call count. */
		private static int idNames(MethodNode method, String call, IdName out) {
			Integer pending = null;
			int calls = 0;
			for (AbstractInsnNode insn : method.instructions) {
				if (insn.getOpcode() < 0) {
					continue;
				}
				// not an array element (new String[] {"shrub", "grass", "fern"} of a block item's variant names)
				if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String s && pending != null && next(insn) != Opcodes.AASTORE) {
					out.accept(pending, s);
				}
				pending = intConstant(insn);
				if (insn instanceof MethodInsnNode m && m.name.equals(call) && m.desc.startsWith("(ILjava/lang/String;")) {
					calls++;
				}
			}
			return calls;
		}

		private static int next(AbstractInsnNode insn) {
			for (AbstractInsnNode n = insn.getNext(); n != null; n = n.getNext()) {
				if (n.getOpcode() >= 0) {
					return n.getOpcode();
				}
			}
			return -1;
		}

		private static Integer intConstant(AbstractInsnNode insn) {
			int op = insn.getOpcode();
			if (op >= Opcodes.ICONST_0 && op <= Opcodes.ICONST_5) {
				return op - Opcodes.ICONST_0;
			}
			if ((op == Opcodes.BIPUSH || op == Opcodes.SIPUSH) && insn instanceof IntInsnNode i) {
				return i.operand;
			}
			if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer i) {
				return i;
			}
			return null;
		}

		private static MethodNode method(ZipFile zip, String owner, String name) throws IOException {
			ZipEntry entry = zip.getEntry(owner + ".class");
			if (entry == null) {
				throw new IOException(zip.getName() + " has no " + owner + ": a deobfuscated (MCP) 1.12.2 jar is needed");
			}
			ClassNode node = new ClassNode();
			try (InputStream in = zip.getInputStream(entry)) {
				new ClassReader(in).accept(node, ClassReader.SKIP_FRAMES);
			}
			return node.methods.stream().filter(m -> m.name.equals(name)).findFirst()
					.orElseThrow(() -> new IOException(owner + " has no method " + name));
		}
	}
}
