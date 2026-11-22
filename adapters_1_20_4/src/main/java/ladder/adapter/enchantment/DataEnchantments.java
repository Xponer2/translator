/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.enchantment;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 1.21's enchantments are data (data/<namespace>/enchantment/<name>.json); 1.20.4's are a built-in registry of classes.
 * The mods' enchantments are made from their jars' data when the enchantments register: 1.20.6 enchantments with the
 * definition of the file (items, weight, levels, costs, slots), its exclusive set and the treasure and curse tags. Their
 * 1.21 effects are kept by enchantment (EFFECTS) for the effects engine.
 */
public final class DataEnchantments {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("ladder");
    /** a data enchantment's 1.21 effects, as its file has them */
    public static final Map<Enchantment, JsonObject> EFFECTS = new ConcurrentHashMap<>();

    private DataEnchantments() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DataEnchantments::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ENCHANTMENT)) {
            return;
        }
        for (var info : ModList.get().getModFiles()) {
            Path data = info.getFile().getSecureJar().getRootPath().resolve("data");
            if (!Files.isDirectory(data)) {
                continue;
            }
            try (Stream<Path> namespaces = Files.list(data)) {
                for (Path namespace : (Iterable<Path>) namespaces::iterator) {
                    Path folder = namespace.resolve("enchantment");
                    if (Files.isDirectory(folder)) {
                        registerFolder(event, namespace.getFileName().toString().replace("/", ""), folder);
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("can't read the enchantments of {}", info.getFile().getFileName(), e);
            }
        }
    }

    private static void registerFolder(RegisterEvent event, String namespace, Path folder) throws IOException {
        try (Stream<Path> files = Files.walk(folder)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String name = folder.relativize(file).toString().replace('\\', '/');
                if (!name.endsWith(".json") || Files.isDirectory(file)) {
                    continue;
                }
                ResourceLocation id = new ResourceLocation(namespace, name.substring(0, name.length() - ".json".length()));
                if (BuiltInRegistries.ENCHANTMENT.containsKey(id)) {
                    // the game's own (a mod's file overriding a vanilla enchantment): 1.20.6 keeps its own
                    continue;
                }
                try (Reader reader = Files.newBufferedReader(file)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    TagKey<Item> supported = itemTag(json.get("supported_items"));
                    net.minecraft.world.item.enchantment.EnchantmentCategory category = net.minecraft.world.item.enchantment.EnchantmentCategory.create(
                            "LADDER_" + id.toString().toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]", "_"),
                            item -> item.builtInRegistryHolder().is(supported));
                    Data enchantment = new Data(rarity(json.get("weight").getAsInt()), category, slots(json.get("slots")), json.get("max_level").getAsInt(),
                            cost(json.getAsJsonObject("min_cost")), cost(json.getAsJsonObject("max_cost")), supported, set(json, "exclusive_set"));
                    if (json.has("effects") && json.get("effects").isJsonObject()) {
                        EFFECTS.put(enchantment, json.getAsJsonObject("effects"));
                    }
                    event.register(Registries.ENCHANTMENT, id, () -> enchantment);
                } catch (RuntimeException e) {
                    LOGGER.warn("an enchantment {} that can't be read: {}", id, e.toString());
                }
            }
        }
    }

    private static Enchantment.Rarity rarity(int weight) {
        return weight >= 10 ? Enchantment.Rarity.COMMON : weight >= 5 ? Enchantment.Rarity.UNCOMMON : weight >= 2 ? Enchantment.Rarity.RARE
                : Enchantment.Rarity.VERY_RARE;
    }

    /** an item set by tag ("#ns:path"); a list or an item has no tag in 1.20.6: an empty one of that name */
    private static TagKey<Item> itemTag(JsonElement value) {
        String text = value.isJsonPrimitive() ? value.getAsString() : "ladder:none";
        return TagKey.create(Registries.ITEM, new ResourceLocation(text.startsWith("#") ? text.substring(1) : "ladder:none"));
    }

    /** 1.21's cost: base + per level above the first */
    private static int[] cost(JsonObject cost) {
        return new int[] {cost.get("base").getAsInt(), cost.get("per_level_above_first").getAsInt()};
    }

    /** 1.21's slot groups as 1.20.6's slots */
    private static EquipmentSlot[] slots(JsonElement value) {
        Set<EquipmentSlot> slots = new LinkedHashSet<>();
        List<JsonElement> groups = new ArrayList<>();
        if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(groups::add);
        } else {
            groups.add(value);
        }
        for (JsonElement g : groups) {
            switch (g.getAsString()) {
                case "any" -> slots.addAll(List.of(EquipmentSlot.values()));
                case "mainhand" -> slots.add(EquipmentSlot.MAINHAND);
                case "offhand" -> slots.add(EquipmentSlot.OFFHAND);
                case "hand" -> slots.addAll(List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));
                case "feet" -> slots.add(EquipmentSlot.FEET);
                case "legs" -> slots.add(EquipmentSlot.LEGS);
                case "chest" -> slots.add(EquipmentSlot.CHEST);
                case "head" -> slots.add(EquipmentSlot.HEAD);
                case "armor" -> slots.addAll(List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD));
                default -> {
                }
            }
        }
        return slots.toArray(EquipmentSlot[]::new);
    }

    /** an enchantment set: a tag ("#ns:path") or ids */
    private static EnchantmentSet set(JsonObject json, String key) {
        if (!json.has(key)) {
            return EnchantmentSet.NONE;
        }
        JsonElement value = json.get(key);
        if (value.isJsonPrimitive() && value.getAsString().startsWith("#")) {
            return new EnchantmentSet(Optional.of(TagKey.create(Registries.ENCHANTMENT, new ResourceLocation(value.getAsString().substring(1)))), Set.of());
        }
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(e -> ids.add(new ResourceLocation(e.getAsString())));
        } else {
            ids.add(new ResourceLocation(value.getAsString()));
        }
        return new EnchantmentSet(Optional.empty(), ids);
    }

    record EnchantmentSet(Optional<TagKey<Enchantment>> tag, Set<ResourceLocation> ids) {
        static final EnchantmentSet NONE = new EnchantmentSet(Optional.empty(), Set.of());

        boolean contains(Enchantment enchantment) {
            ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
            return id != null && (this.ids.contains(id)
                    || this.tag.isPresent() && BuiltInRegistries.ENCHANTMENT.wrapAsHolder(enchantment).is(this.tag.get()));
        }
    }

    /** an enchantment of a mod's data: its definition, its exclusive set, treasure and curse by 1.21's tags */
    static final class Data extends Enchantment {
        private static final TagKey<Enchantment> TREASURE = TagKey.create(Registries.ENCHANTMENT, new ResourceLocation("treasure"));
        private static final TagKey<Enchantment> CURSE = TagKey.create(Registries.ENCHANTMENT, new ResourceLocation("curse"));
        private final int maxLevel;
        private final int[] minCost;
        private final int[] maxCost;
        private final TagKey<Item> supported;
        private final EnchantmentSet exclusive;

        Data(Rarity rarity, net.minecraft.world.item.enchantment.EnchantmentCategory category, EquipmentSlot[] slots, int maxLevel, int[] minCost,
                int[] maxCost, TagKey<Item> supported, EnchantmentSet exclusive) {
            super(rarity, category, slots);
            this.maxLevel = maxLevel;
            this.minCost = minCost;
            this.maxCost = maxCost;
            this.supported = supported;
            this.exclusive = exclusive;
        }

        private boolean is(TagKey<Enchantment> tag) {
            return BuiltInRegistries.ENCHANTMENT.wrapAsHolder(this).is(tag);
        }

        @Override
        public int getMaxLevel() {
            return this.maxLevel;
        }

        @Override
        public int getMinCost(int level) {
            return this.minCost[0] + this.minCost[1] * (level - 1);
        }

        @Override
        public int getMaxCost(int level) {
            return this.maxCost[0] + this.maxCost[1] * (level - 1);
        }

        @Override
        public boolean canEnchant(net.minecraft.world.item.ItemStack stack) {
            return stack.is(this.supported);
        }

        @Override
        protected boolean checkCompatibility(Enchantment other) {
            return super.checkCompatibility(other) && !this.exclusive.contains(other);
        }

        @Override
        public boolean isTreasureOnly() {
            return this.is(TREASURE);
        }

        @Override
        public boolean isCurse() {
            return this.is(CURSE);
        }
    }
}
