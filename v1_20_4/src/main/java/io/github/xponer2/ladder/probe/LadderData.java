/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.ladder.probe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

/**
 * Whether the converted mods' data loaded: every JSON file of a mod's {@code data/} folder that names a registry entry
 * (worldgen and the other data-driven registries), a tag, a loot table, a recipe or an advancement must be in the
 * running game. A file that isn't was dropped by the game (a format 1.20.4 doesn't read), unless its own conditions
 * say so (counted apart: the same file is absent on 1.21.1 then too).
 */
final class LadderData {
    private LadderData() {
    }

    static JsonObject record(MinecraftServer server) {
        // folder (below data/<namespace>/) -> what it holds
        Map<String, Registry<?>> registries = new HashMap<>();
        Map<String, Registry<?>> tagDirs = new HashMap<>();
        server.registryAccess().registries().forEach(entry -> {
            ResourceKey<? extends Registry<?>> key = entry.key();
            String folder = key.location().getNamespace().equals("minecraft") ? key.location().getPath()
                    : key.location().getNamespace() + "/" + key.location().getPath();
            registries.put(folder, entry.value());
            tagDirs.put(TagManager.getTagDir(key), entry.value());
        });
        Map<String, Set<ResourceLocation>> loot = new HashMap<>();
        for (LootDataType<?> type : List.of(LootDataType.TABLE, LootDataType.PREDICATE, LootDataType.MODIFIER)) {
            loot.put(type.directory(), new HashSet<>(server.getLootData().getKeys(type)));
        }
        JsonObject result = new JsonObject();
        for (IModFileInfo info : ModList.get().getModFiles()) {
            String mod = info.getMods().isEmpty() ? info.getFile().getFileName() : info.getMods().get(0).getModId();
            if (mod.equals("minecraft") || mod.equals("neoforge") || mod.equals("ladder_probe")) {
                continue;
            }
            Path data = info.getFile().findResource("data");
            if (!Files.isDirectory(data)) {
                continue;
            }
            Map<String, int[]> counts = new TreeMap<>();
            JsonArray missing = new JsonArray();
            try (Stream<Path> files = Files.walk(data)) {
                for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                    String rel = data.relativize(file).toString().replace('\\', '/');
                    int slash = rel.indexOf('/');
                    if (slash < 0) {
                        continue;
                    }
                    String namespace = rel.substring(0, slash), rest = rel.substring(slash + 1, rel.length() - 5);
                    String[] kind = new String[1];
                    Boolean present = present(server, namespace, rest, registries, tagDirs, loot, kind);
                    if (present == null) {
                        continue;
                    }
                    int[] c = counts.computeIfAbsent(kind[0], k -> new int[3]);
                    c[0]++;
                    if (present) {
                        c[1]++;
                    } else if (conditional(file)) {
                        c[2]++;
                    } else if (missing.size() < 20) {
                        missing.add(rel);
                    }
                }
            } catch (IOException | RuntimeException e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", e.toString());
                result.add(mod, error);
                continue;
            }
            JsonObject modResult = new JsonObject();
            int files = 0, loaded = 0, conditional = 0;
            for (var e : counts.entrySet()) {
                JsonObject k = new JsonObject();
                k.addProperty("files", e.getValue()[0]);
                k.addProperty("loaded", e.getValue()[1]);
                k.addProperty("conditional", e.getValue()[2]);
                modResult.add(e.getKey(), k);
                files += e.getValue()[0];
                loaded += e.getValue()[1];
                conditional += e.getValue()[2];
            }
            if (files == 0) {
                continue;
            }
            modResult.addProperty("files", files);
            modResult.addProperty("loaded", loaded);
            modResult.addProperty("conditional", conditional);
            modResult.add("missing", missing);
            result.add(mod, modResult);
        }
        return result;
    }

    /** null: not a file of a kind checked here; else whether the game has it (kind[0]: its kind) */
    private static Boolean present(MinecraftServer server, String namespace, String rest, Map<String, Registry<?>> registries,
            Map<String, Registry<?>> tagDirs, Map<String, Set<ResourceLocation>> loot, String[] kind) {
        // the longest folder that is a known kind wins (worldgen/structure before worldgen)
        for (int cut = rest.lastIndexOf('/'); cut > 0; cut = rest.lastIndexOf('/', cut - 1)) {
            String folder = rest.substring(0, cut);
            ResourceLocation id = new ResourceLocation(namespace, rest.substring(cut + 1));
            if (registries.containsKey(folder)) {
                kind[0] = folder;
                return registries.get(folder).containsKey(id);
            }
            if (tagDirs.containsKey(folder)) {
                kind[0] = folder;
                return tag(tagDirs.get(folder), id);
            }
            if (loot.containsKey(folder)) {
                kind[0] = folder;
                return loot.get(folder).contains(id);
            }
            if (folder.equals("recipes")) {
                kind[0] = folder;
                return server.getRecipeManager().byKey(id).isPresent();
            }
            if (folder.equals("advancements")) {
                kind[0] = folder;
                return server.getAdvancements().get(id) != null;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean tag(Registry registry, ResourceLocation id) {
        return registry.getTag(TagKey.create(registry.key(), id)).isPresent();
    }

    /** whether the file carries load conditions (then the game may leave it out on 1.21.1 too) */
    private static boolean conditional(Path file) {
        try {
            var json = com.google.gson.JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!json.isJsonObject()) {
                return false;
            }
            var top = json.getAsJsonObject();
            return top.has("neoforge:conditions") || top.has("forge:conditions") || top.has("fabric:load_conditions");
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
