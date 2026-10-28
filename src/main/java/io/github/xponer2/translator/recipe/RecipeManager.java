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
package io.github.xponer2.translator.recipe;

import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.Registries;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The recipes of the 1.21 recipe system (1.21 {@code RecipeManager}), one for the game: {@link #get()} (1.21
 * {@code level.getRecipeManager()}). {@link #load()} reads {@code data/<namespace>/recipe/**.json} of every mod.
 */
public final class RecipeManager {
    private static final Logger LOGGER = LogManager.getLogger("Cucumber Recipes");

    private static final RecipeManager INSTANCE = new RecipeManager();
    private static final Pattern RECIPE_FILE = Pattern.compile("([a-z0-9_.-]+)/recipe/([a-z0-9_./-]+)\\.json");

    private Map<ResourceLocation, RecipeHolder<?>> byName = Collections.emptyMap();
    private Map<RecipeType<?>, List<RecipeHolder<?>>> byType = Collections.emptyMap();
    private ICondition.IContext context = ICondition.IContext.EMPTY;

    private RecipeManager() { }

    public static RecipeManager get() {
        return INSTANCE;
    }

    /**
     * What the conditions of the recipes may ask (tags); set before {@link #load()}.
     */
    public void setConditionContext(ICondition.IContext context) {
        this.context = context;
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, World level) {
        return this.getRecipeFor(type, input, level, (RecipeHolder<T>) null);
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, World level, @Nullable RecipeHolder<T> lastRecipe) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        if (lastRecipe != null && lastRecipe.value().matches(input, level)) {
            return Optional.of(lastRecipe);
        }
        for (var holder : this.byType(type)) {
            if (holder.value().matches(input, level)) {
                return Optional.of(holder);
            }
        }
        if (type == RecipeType.SMELTING && input instanceof SingleRecipeInput single) {
            return furnaceRecipe(single);
        }
        return Optional.empty();
    }

    /** 1.21 remaining items of a craft: the matching recipe's, else the crafting remainders of the inputs. */
    public <I extends RecipeInput, T extends Recipe<I>> net.minecraft.util.NonNullList<net.minecraft.item.ItemStack> getRemainingItemsFor(RecipeType<T> type, I input, World level) {
        var recipe = this.getRecipeFor(type, input, level);
        if (recipe.isPresent()) {
            return recipe.get().value().getRemainingItems(input);
        }
        var remaining = net.minecraft.util.NonNullList.withSize(input.size(), net.minecraft.item.ItemStack.EMPTY);
        for (int i = 0; i < remaining.size(); i++) {
            var stack = input.getItem(i);
            if (stack.getItem().hasContainerItem(stack)) {
                remaining.set(i, stack.getItem().getContainerItem(stack));
            }
        }
        return remaining;
    }

    // the 1.12.2 furnace recipes (vanilla and other mods') as 1.21 smelting recipes
    @SuppressWarnings("unchecked")
    private static <T extends Recipe<?>> Optional<RecipeHolder<T>> furnaceRecipe(SingleRecipeInput input) {
        var stack = input.item();
        var result = net.minecraft.item.crafting.FurnaceRecipes.instance().getSmeltingResult(stack);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        var single = stack.copy();
        single.setCount(1);
        var recipe = new VanillaRecipes.SmeltingRecipe("", net.minecraft.item.crafting.Ingredient.fromStacks(single), result.copy(),
                net.minecraft.item.crafting.FurnaceRecipes.instance().getSmeltingExperience(result), 200);
        var id = new ResourceLocation("minecraft", "furnace/" + stack.getItem().getRegistryName().getPath() + "_" + stack.getMetadata());
        return Optional.of((RecipeHolder<T>) new RecipeHolder<>(id, recipe));
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, World level, @Nullable ResourceLocation lastRecipe) {
        @SuppressWarnings("unchecked")
        var last = lastRecipe == null ? null : (RecipeHolder<T>) this.byName.get(lastRecipe);
        return this.getRecipeFor(type, input, level, last);
    }

    public <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> getRecipesFor(RecipeType<T> type, I input, World level) {
        var result = new ArrayList<RecipeHolder<T>>();
        for (var holder : this.byType(type)) {
            if (holder.value().matches(input, level)) {
                result.add(holder);
            }
        }
        return result;
    }

    public <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> getAllRecipesFor(RecipeType<T> type) {
        return Collections.unmodifiableList(this.byType(type));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Recipe<?>> List<RecipeHolder<T>> byType(RecipeType<T> type) {
        return (List) this.byType.getOrDefault(type, Collections.emptyList());
    }

    public Optional<RecipeHolder<?>> byKey(ResourceLocation id) {
        return Optional.ofNullable(this.byName.get(id));
    }

    public Collection<RecipeHolder<?>> getRecipes() {
        return Collections.unmodifiableCollection(this.byName.values());
    }

    public Stream<ResourceLocation> getRecipeIds() {
        return this.byName.keySet().stream();
    }

    /**
     * Replaces all recipes (1.21 {@code replaceRecipes}).
     */
    public void replaceRecipes(Iterable<RecipeHolder<?>> recipes) {
        var byName = new LinkedHashMap<ResourceLocation, RecipeHolder<?>>();
        var byType = new IdentityHashMap<RecipeType<?>, List<RecipeHolder<?>>>();
        for (var holder : recipes) {
            if (byName.putIfAbsent(holder.id(), holder) != null) {
                throw new IllegalStateException("Duplicate recipe " + holder.id());
            }
            byType.computeIfAbsent(holder.value().getType(), t -> new ArrayList<>()).add(holder);
        }
        this.byName = byName;
        this.byType = byType;
    }

    /**
     * Loads the recipes of all mods; returns them, also to hand them to the 1.12.2 recipe systems.
     */
    public List<RecipeHolder<?>> load() {
        var files = new LinkedHashMap<ResourceLocation, JsonElement>();
        for (var mod : Loader.instance().getActiveModList()) {
            CraftingHelper.findFiles(mod, "data", null, (root, file) -> {
                var relative = root.relativize(file).toString().replace('\\', '/');
                var m = RECIPE_FILE.matcher(relative);
                if (m.matches()) {
                    try {
                        var text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                        files.put(new ResourceLocation(m.group(1), m.group(2)), new JsonParser().parse(text));
                    } catch (IOException | RuntimeException e) {
                        LOGGER.error("Couldn't read recipe {} of {}", relative, mod.getModId(), e);
                    }
                }
                return true;
            }, true, true);
        }

        var recipes = new ArrayList<RecipeHolder<?>>();
        int skipped = 0;
        // recipes with vanilla content added after 1.12.2 (tuff, turtles, ...) are expected to fail: listed once
        var missing = new java.util.TreeSet<String>();
        int withMissing = 0;
        for (var entry : files.entrySet()) {
            var errors = new ArrayList<String>();
            var holder = this.parse(entry.getKey(), entry.getValue(), errors::add);
            if (holder != null) {
                recipes.add(holder);
                continue;
            }
            skipped++;
            var vanilla = missingVanilla(entry.getValue());
            if (!errors.isEmpty() && !vanilla.isEmpty()) {
                missing.addAll(vanilla);
                withMissing++;
            } else {
                errors.forEach(e -> LOGGER.error("Parsing error loading recipe {}: {}", entry.getKey(), e));
            }
        }
        this.replaceRecipes(recipes);
        LOGGER.info("Loaded {} recipes ({} skipped by their conditions or errors)", recipes.size(), skipped);
        if (!this.skippedBy.isEmpty()) {
            this.skippedBy.forEach((type, ids) -> LOGGER.info("Skipped by {}: {} recipes, e.g. {}", type, ids.size(),
                    String.join(", ", ids.stream().limit(6).collect(java.util.stream.Collectors.toList()))));
            this.skippedBy.forEach((type, ids) -> LOGGER.debug("Skipped by {}: {}", type, ids));
            this.skippedBy.clear();
        }
        if (withMissing > 0) {
            LOGGER.info("Skipped {} recipes with vanilla content 1.12.2 doesn't have: {}", withMissing, String.join(", ", missing));
        }
        return recipes;
    }

    // ids of vanilla items, blocks, entities and enchantments a recipe names that 1.12.2 doesn't have
    private static java.util.Set<String> missingVanilla(JsonElement json) {
        var missing = new java.util.TreeSet<String>();
        collectMissingVanilla(json, null, missing);
        return missing;
    }

    private static void collectMissingVanilla(JsonElement json, @Nullable String key, java.util.Set<String> missing) {
        if (json.isJsonObject()) {
            for (var entry : json.getAsJsonObject().entrySet()) {
                collectMissingVanilla(entry.getValue(), entry.getKey(), missing);
            }
        } else if (json.isJsonArray()) {
            for (var element : json.getAsJsonArray()) {
                collectMissingVanilla(element, key, missing);
            }
        } else if (key != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isString() && json.getAsString().startsWith("minecraft:")) {
            var id = io.github.xponer2.translator.util.ResourceLocations.parse(json.getAsString());
            boolean known = switch (key) {
                // inputs need the item itself, results may be a similar one
                case "item" -> !io.github.xponer2.translator.util.ItemIds.stack(id, 1).isEmpty();
                case "id", "result" -> !io.github.xponer2.translator.util.ItemIds.resultStack(id, 1).isEmpty();
                case "block" -> net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS.containsKey(id);
                case "entity", "entities" -> BuiltInRegistries.byKey(Registries.ENTITY_TYPE).get(id) != null;
                case "enchantment" -> BuiltInRegistries.byKey(Registries.ENCHANTMENT).get(id) != null;
                default -> true;
            };
            if (!known) {
                missing.add(id.toString());
            }
        }
    }

    /**
     * One recipe file: null if its conditions don't hold or it can't be read (reported to the consumer).
     */
    @Nullable
    // condition type -> recipes it left out, for the load summary
    private final java.util.Map<String, java.util.Set<String>> skippedBy = new java.util.TreeMap<>();

    public RecipeHolder<?> parse(ResourceLocation id, JsonElement json, Consumer<String> errors) {
        if (!json.isJsonObject()) {
            errors.accept("not a JSON object");
            return null;
        }
        var object = (JsonObject) json;
        if (object.has("neoforge:conditions")) {
            var conditions = ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, object.get("neoforge:conditions"));
            var error = conditions.error();
            if (error.isPresent()) {
                errors.accept(error.get().message());
                return null;
            }
            for (var condition : conditions.result().orElse(Collections.emptyList())) {
                if (!condition.test(this.context)) {
                    var type = io.github.xponer2.translator.registry.NeoForgeRegistries.CONDITION_SERIALIZERS.getKey(condition.codec());
                    this.skippedBy.computeIfAbsent(String.valueOf(type), t -> new java.util.TreeSet<>()).add(id.toString());
                    return null;
                }
            }
        }
        if (!object.has("type")) {
            errors.accept("no recipe type");
            return null;
        }
        var type = object.get("type").getAsString();
        var serializer = BuiltInRegistries.byKey(Registries.RECIPE_SERIALIZER).get(io.github.xponer2.translator.util.ResourceLocations.parse(type));
        if (serializer == null) {
            errors.accept("unknown recipe serializer " + type);
            return null;
        }
        var recipe = serializer.codec().codec().parse(JsonOps.INSTANCE, object);
        var error = recipe.error();
        if (error.isPresent()) {
            errors.accept(error.get().message());
            return null;
        }
        return new RecipeHolder<>(id, (Recipe<?>) recipe.result().get());
    }
}
