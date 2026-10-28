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
package io.github.xponer2.translator.item;

import io.github.xponer2.translator.util.TagKey;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.Item;
import net.minecraftforge.common.util.EnumHelper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 1.12.2 tool materials and harvest levels of {@link Tier}s.
 */
public final class Tiers {
    private static final Logger LOGGER = LogManager.getLogger("Cucumber Tiers");

    // 1.21 vanilla "incorrect for" tags -> the highest 1.12.2 harvest level such a tool mines
    private static final Map<String, Integer> VANILLA_LEVELS = new HashMap<>();
    // a tier that may mine every block (empty "incorrect for" tag): above every vanilla requirement (obsidian: 3)
    private static final int ANY_LEVEL = 4;
    private static final Map<Tier, Item.ToolMaterial> MATERIALS = new IdentityHashMap<>();

    static {
        VANILLA_LEVELS.put("minecraft:incorrect_for_wooden_tool", 0);
        VANILLA_LEVELS.put("minecraft:incorrect_for_gold_tool", 0);
        VANILLA_LEVELS.put("minecraft:incorrect_for_stone_tool", 1);
        VANILLA_LEVELS.put("minecraft:incorrect_for_iron_tool", 2);
        VANILLA_LEVELS.put("minecraft:incorrect_for_diamond_tool", 3);
        VANILLA_LEVELS.put("minecraft:incorrect_for_netherite_tool", 4);
    }

    // the vanilla tiers (1.21 Tiers enum) on the vanilla 1.12.2 tool materials; netherite doesn't exist: diamond
    public static final Tier WOOD = new Vanilla(Item.ToolMaterial.WOOD, "incorrect_for_wooden_tool");
    public static final Tier STONE = new Vanilla(Item.ToolMaterial.STONE, "incorrect_for_stone_tool");
    public static final Tier IRON = new Vanilla(Item.ToolMaterial.IRON, "incorrect_for_iron_tool");
    public static final Tier DIAMOND = new Vanilla(Item.ToolMaterial.DIAMOND, "incorrect_for_diamond_tool");
    public static final Tier GOLD = new Vanilla(Item.ToolMaterial.GOLD, "incorrect_for_gold_tool");
    public static final Tier NETHERITE = DIAMOND;

    private Tiers() { }

    private static final class Vanilla implements Tier {
        private final Item.ToolMaterial material;
        private final TagKey<net.minecraft.block.Block> incorrect;

        Vanilla(Item.ToolMaterial material, String incorrectTag) {
            this.material = material;
            this.incorrect = TagKey.create(io.github.xponer2.translator.util.Registries.BLOCK, new net.minecraft.util.ResourceLocation("minecraft", incorrectTag));
        }

        @Override
        public int getUses() {
            return this.material.getMaxUses();
        }

        @Override
        public float getSpeed() {
            return this.material.getEfficiency();
        }

        @Override
        public float getAttackDamageBonus() {
            return this.material.getAttackDamage();
        }

        @Override
        public TagKey<net.minecraft.block.Block> getIncorrectBlocksForDrops() {
            return this.incorrect;
        }

        @Override
        public int getEnchantmentValue() {
            return this.material.getEnchantability();
        }

        @Override
        public net.minecraft.item.crafting.Ingredient getRepairIngredient() {
            var stack = this.material.getRepairItemStack();
            return stack.isEmpty() ? net.minecraft.item.crafting.Ingredient.EMPTY : net.minecraft.item.crafting.Ingredient.fromStacks(stack);
        }

        @Override
        public Item.ToolMaterial toolMaterial() {
            return this.material;
        }

        @Override
        public int harvestLevel() {
            return this.material.getHarvestLevel();
        }
    }

    public static synchronized Item.ToolMaterial toolMaterial(Tier tier) {
        return MATERIALS.computeIfAbsent(tier, t -> EnumHelper.addToolMaterial(name(t), t.harvestLevel(), t.getUses(),
                t.getSpeed(), t.getAttackDamageBonus(), t.getEnchantmentValue()));
    }

    private static String name(Tier tier) {
        if (tier instanceof Enum<?> e) {
            return (e.getDeclaringClass().getName() + "_" + e.name()).replace('.', '_').toUpperCase(Locale.ROOT);
        }
        return tier.getIncorrectBlocksForDrops().location().toString().replace(':', '_').toUpperCase(Locale.ROOT);
    }

    /**
     * The harvest level of a tier from its "incorrect for" block tag: the vanilla tags by name, the others by the
     * vanilla tags they include (lowest wins); a tag that includes none of them excludes nothing.
     */
    public static int harvestLevel(TagKey<?> incorrectForDrops) {
        return level(incorrectForDrops.location().toString(), new HashSet<>());
    }

    private static int level(String tag, Set<String> seen) {
        var vanilla = VANILLA_LEVELS.get(tag);
        if (vanilla != null) {
            return vanilla;
        }
        if (!seen.add(tag)) {
            return ANY_LEVEL;
        }
        int level = ANY_LEVEL;
        var colon = tag.indexOf(':');
        var file = "data/" + tag.substring(0, colon) + "/tags/block/" + tag.substring(colon + 1) + ".json";
        try {
            var urls = Tiers.class.getClassLoader().getResources(file);
            while (urls.hasMoreElements()) {
                try (var reader = new InputStreamReader(urls.nextElement().openStream(), StandardCharsets.UTF_8)) {
                    for (JsonElement value : new JsonParser().parse(reader).getAsJsonObject().getAsJsonArray("values")) {
                        var id = value.isJsonObject() ? ((JsonObject) value).get("id").getAsString() : value.getAsString();
                        if (id.startsWith("#")) {
                            level = Math.min(level, level(id.substring(1), seen));
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Can't read the block tag {} for a tool tier", tag, e);
        }
        return level;
    }
}
