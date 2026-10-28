package io.github.xponer2.translator.tags;

import io.github.xponer2.translator.recipe.ICondition;
import io.github.xponer2.translator.util.Codecs;
import io.github.xponer2.translator.util.ItemIds;
import io.github.xponer2.translator.util.Registries;
import io.github.xponer2.translator.util.TagKey;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The tags of the 1.21 data of all mods ({@code data/<namespace>/tags/<item|block|...>/**.json}). 1.12.2 has no tags:
 * <ul>
 * <li>files of the same tag in several mods are merged ({@code "replace": true} drops what was read before);</li>
 * <li>ids are 1.21 ids: flattened vanilla ids are translated by {@code data/translator/flattening.json}, ids of items
 * that don't exist are skipped (1.21 would fail the tag unless {@code "required": false});</li>
 * <li>{@code ore:<name>} entries are the stacks of that OreDictionary name, so the tags also see the materials of
 * 1.12.2 mods.</li>
 * </ul>
 * Item tags are lists of stacks (item + meta, {@code OreDictionary.WILDCARD_VALUE} = any), block tags lists of block
 * states patterns. Contents are built on first use and rebuilt by {@link #reload()} (post init: every mod has
 * registered its OreDictionary entries).
 */
public final class TagManager {
    private static final Logger LOGGER = LogManager.getLogger("Cucumber Tags");

    private static final Pattern TAG_FILE = Pattern.compile("([a-z0-9_.-]+)/tags/([a-z_]+)/([a-z0-9_./-]+)\\.json");
    private static final Map<String, Map<ResourceLocation, List<Entry>>> FILES = new HashMap<>();
    private static final Map<ResourceLocation, IBlockState> FLATTENED_BLOCKS = new HashMap<>();
    private static final Map<ResourceLocation, List<ItemStack>> ITEM_TAGS = new HashMap<>();
    private static final Map<ResourceLocation, List<BlockEntry>> BLOCK_TAGS = new HashMap<>();
    private static boolean read;

    private TagManager() { }

    /**
     * Rebuilds the tag contents and hands the item tags to the ingredient codecs and the recipe conditions.
     */
    public static synchronized void reload() {
        ITEM_TAGS.clear();
        BLOCK_TAGS.clear();
        // read again: a tag used before every item was registered may have missed flattened ids
        FILES.clear();
        FLATTENED_BLOCKS.clear();
        read = false;
        readFiles();
        Codecs.setItemTagResolver(TagManager::itemTag);
    }

    /**
     * The condition context of the recipe loader: item and block tags.
     */
    public static ICondition.IContext conditionContext() {
        return new ICondition.IContext() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Collection<T> getTag(TagKey<T> key) {
                if (key.isFor(Registries.ITEM)) {
                    var items = new ArrayList<Item>();
                    for (var stack : itemTag(key.location())) {
                        items.add(stack.getItem());
                    }
                    return (Collection<T>) items;
                }
                if (key.isFor(Registries.BLOCK)) {
                    var blocks = new ArrayList<Block>();
                    for (var entry : blockTag(key.location())) {
                        blocks.add(entry.block);
                    }
                    return (Collection<T>) blocks;
                }
                return Collections.emptyList();
            }
        };
    }

    /**
     * The stacks of an item tag (meta {@code OreDictionary.WILDCARD_VALUE} = any meta).
     */
    public static synchronized List<ItemStack> itemTag(ResourceLocation tag) {
        readFiles();
        var cached = ITEM_TAGS.get(tag);
        if (cached == null) {
            var result = new ArrayList<ItemStack>();
            collectItems(tag, result, new HashSet<>());
            cached = Collections.unmodifiableList(result);
            ITEM_TAGS.put(tag, cached);
        }
        return cached;
    }

    public static boolean is(ItemStack stack, TagKey<Item> tag) {
        if (stack.isEmpty()) {
            return false;
        }
        for (var entry : itemTag(tag.location())) {
            if (entry.getItem() == stack.getItem()
                    && (entry.getMetadata() == OreDictionary.WILDCARD_VALUE || entry.getMetadata() == stack.getMetadata())) {
                return true;
            }
        }
        return false;
    }

    public static boolean is(Item item, TagKey<Item> tag) {
        for (var entry : itemTag(tag.location())) {
            if (entry.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    /**
     * The item tags a stack is in (1.21 {@code stack.getTags()}).
     */
    public static synchronized java.util.stream.Stream<TagKey<Item>> getTags(ItemStack stack) {
        readFiles();
        var ids = new ArrayList<>(FILES.getOrDefault("item", Collections.emptyMap()).keySet());
        return ids.stream().map(id -> TagKey.create(Registries.ITEM, id)).filter(tag -> is(stack, tag));
    }

    /**
     * The block tags a block state is in (1.21 {@code state.getTags()}).
     */
    public static synchronized java.util.stream.Stream<TagKey<Block>> getTags(IBlockState state) {
        readFiles();
        var ids = new ArrayList<>(FILES.getOrDefault("block", Collections.emptyMap()).keySet());
        return ids.stream().map(id -> TagKey.create(Registries.BLOCK, id)).filter(tag -> is(state, tag));
    }

    public static synchronized List<BlockEntry> blockTag(ResourceLocation tag) {
        readFiles();
        var cached = BLOCK_TAGS.get(tag);
        if (cached == null) {
            var result = new ArrayList<BlockEntry>();
            collectBlocks(tag, result, new HashSet<>());
            cached = Collections.unmodifiableList(result);
            BLOCK_TAGS.put(tag, cached);
        }
        return cached;
    }

    @SuppressWarnings("deprecation")
    public static boolean is(IBlockState state, TagKey<Block> tag) {
        var meta = state.getBlock().getMetaFromState(state);
        for (var entry : blockTag(tag.location())) {
            if (entry.block == state.getBlock() && (entry.meta < 0 || entry.meta == meta)) {
                return true;
            }
        }
        return false;
    }

    public static boolean is(Block block, TagKey<Block> tag) {
        for (var entry : blockTag(tag.location())) {
            if (entry.block == block) {
                return true;
            }
        }
        return false;
    }

    private static void collectItems(ResourceLocation tag, List<ItemStack> result, Set<ResourceLocation> seen) {
        if (!seen.add(tag)) {
            return;
        }
        for (var entry : FILES.getOrDefault("item", Collections.emptyMap()).getOrDefault(tag, Collections.emptyList())) {
            if (entry.tag) {
                collectItems(new ResourceLocation(entry.id), result, seen);
            } else if (entry.id.startsWith("ore:")) {
                for (var stack : OreDictionary.getOres(entry.id.substring(4), false)) {
                    addItem(result, stack);
                }
            } else {
                var stack = ItemIds.stack(new ResourceLocation(entry.id), 1);
                if (!stack.isEmpty()) {
                    addItem(result, stack);
                }
            }
        }
    }

    private static void addItem(List<ItemStack> result, ItemStack stack) {
        for (var existing : result) {
            if (existing.getItem() == stack.getItem() && existing.getMetadata() == stack.getMetadata()) {
                return;
            }
        }
        var copy = stack.copy();
        copy.setCount(1);
        result.add(copy);
    }

    @SuppressWarnings("deprecation")
    private static void collectBlocks(ResourceLocation tag, List<BlockEntry> result, Set<ResourceLocation> seen) {
        if (!seen.add(tag)) {
            return;
        }
        for (var entry : FILES.getOrDefault("block", Collections.emptyMap()).getOrDefault(tag, Collections.emptyList())) {
            if (entry.tag) {
                collectBlocks(new ResourceLocation(entry.id), result, seen);
            } else if (entry.id.startsWith("ore:")) {
                for (var stack : OreDictionary.getOres(entry.id.substring(4), false)) {
                    if (stack.getItem() instanceof ItemBlock itemBlock) {
                        var meta = stack.getMetadata() == OreDictionary.WILDCARD_VALUE ? -1 : itemBlock.getMetadata(stack.getMetadata());
                        result.add(new BlockEntry(itemBlock.getBlock(), meta));
                    }
                }
            } else {
                var id = new ResourceLocation(entry.id);
                var flattened = FLATTENED_BLOCKS.get(id);
                if (flattened != null) {
                    result.add(new BlockEntry(flattened.getBlock(), flattened.getBlock().getMetaFromState(flattened)));
                } else if (ForgeRegistries.BLOCKS.containsKey(id)) {
                    result.add(new BlockEntry(ForgeRegistries.BLOCKS.getValue(id), -1));
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void readFiles() {
        if (read) {
            return;
        }
        read = true;
        readFlattening();
        for (var mod : Loader.instance().getActiveModList()) {
            CraftingHelper.findFiles(mod, "data", null, (root, file) -> {
                var relative = root.relativize(file).toString().replace('\\', '/');
                var m = TAG_FILE.matcher(relative);
                if (m.matches()) {
                    try {
                        var json = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
                        var tag = new ResourceLocation(m.group(1), m.group(3));
                        var entries = FILES.computeIfAbsent(m.group(2), k -> new LinkedHashMap<>()).computeIfAbsent(tag, k -> new ArrayList<>());
                        if (json.has("replace") && json.get("replace").getAsBoolean()) {
                            entries.clear();
                        }
                        for (JsonElement value : json.getAsJsonArray("values")) {
                            entries.add(Entry.of(value));
                        }
                    } catch (IOException | RuntimeException e) {
                        LOGGER.error("Couldn't read tag {} of {}", relative, mod.getModId(), e);
                    }
                }
                return true;
            }, true, true);
        }
        LOGGER.info("Read {} item tags and {} block tags", FILES.getOrDefault("item", Collections.emptyMap()).size(),
                FILES.getOrDefault("block", Collections.emptyMap()).size());
    }

    @SuppressWarnings("deprecation")
    private static void readFlattening() {
        var stream = TagManager.class.getResourceAsStream("/data/translator/flattening.json");
        if (stream == null) {
            LOGGER.error("data/translator/flattening.json is missing, 1.21 vanilla ids of flattened items won't resolve");
            return;
        }
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            for (var entry : new JsonParser().parse(reader).getAsJsonObject().entrySet()) {
                var target = entry.getValue().getAsJsonObject();
                var id = new ResourceLocation(entry.getKey());
                var oldId = new ResourceLocation(target.get("id").getAsString());
                var meta = target.get("meta").getAsInt();
                var item = ForgeRegistries.ITEMS.getValue(oldId);
                if (target.has("similar") && target.get("similar").getAsBoolean()) {
                    // not for tags and block states: only recipe results
                    if (item != null) {
                        ItemIds.registerSimilar(id, item, meta);
                    }
                    continue;
                }
                if (item != null) {
                    ItemIds.registerFlattened(id, item, meta);
                }
                var block = ForgeRegistries.BLOCKS.containsKey(oldId) ? ForgeRegistries.BLOCKS.getValue(oldId) : null;
                if (block != null) {
                    FLATTENED_BLOCKS.put(id, block.getStateFromMeta(meta));
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Couldn't read data/translator/flattening.json", e);
        }
    }

    /**
     * A block of a block tag; meta -1 = every state of the block.
     */
    public static final class BlockEntry {
        public final Block block;
        public final int meta;

        BlockEntry(Block block, int meta) {
            this.block = block;
            this.meta = meta;
        }
    }

    private static final class Entry {
        final String id;
        final boolean tag;

        private Entry(String id, boolean tag) {
            this.id = id;
            this.tag = tag;
        }

        static Entry of(JsonElement value) {
            var id = value.isJsonObject() ? ((JsonObject) value).get("id").getAsString() : value.getAsString();
            return id.startsWith("#") ? new Entry(id.substring(1), true) : new Entry(id, false);
        }
    }
}
