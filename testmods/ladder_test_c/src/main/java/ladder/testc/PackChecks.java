/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/**
 * Client only: the mod's built-in resource pack (found, on, its location and selection, a file read from it, again
 * after a reload) and its data pack (a recipe the server loaded from it, as the client got it).
 */
final class PackChecks {
    static final String RESOURCES = "mod/ladder_test_c:resourcepacks/extra";
    private static volatile boolean reloading;

    private PackChecks() {
    }

    static void run(Map<String, String> results) {
        Minecraft minecraft = Minecraft.getInstance();
        PackRepository repository = minecraft.getResourcePackRepository();
        Pack pack = repository.getPack(RESOURCES);
        results.put("packFound", pack == null ? "none" : pack.getId() + " " + pack.getTitle().getString() + " required=" + pack.isRequired() + " "
                + pack.getDefaultPosition() + " fixed=" + pack.isFixedPosition() + " builtIn=" + (pack.getPackSource() == net.minecraft.server.packs.repository.PackSource.BUILT_IN));
        results.put("packSelected", String.valueOf(repository.getSelectedIds().contains(RESOURCES)));
        results.put("packLocation", pack == null ? "none" : pack.location().id() + " " + pack.location().title().getString() + " "
                + pack.location().knownPackInfo().map(k -> k.namespace() + "/" + k.id()).orElse("unknown") + " "
                + pack.selectionConfig().required() + "/" + pack.selectionConfig().defaultPosition() + "/" + pack.selectionConfig().fixedPosition());
        results.put("packValue", read(minecraft));
        results.put("packOverlay", read(minecraft, "ladder/overlay.txt"));
        results.put("dataPackOverlayRecipe", String.valueOf(minecraft.level.getRecipeManager().byKey(Payloads.id("overlay_recipe")).isPresent()));
        var recipes = minecraft.level.getRecipeManager();
        SingleRecipeInput apples = new SingleRecipeInput(new ItemStack(Items.APPLE, 2));
        results.put("dataPackRecipe", recipes.byKey(Payloads.id("pack_recipe")).map(h -> h.id() + " "
                + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(recipes.getRecipeFor(Recipes.INFUSING.get(), apples, minecraft.level)
                        .map(r -> r.value().assemble(apples, minecraft.level.registryAccess()).getItem()).orElse(Items.AIR))).orElse("none"));
        reloading = true;
        minecraft.reloadResourcePacks().thenRun(() -> {
            results.put("packValueReloaded", read(minecraft) + " selected=" + minecraft.getResourcePackRepository().getSelectedIds().contains(RESOURCES));
            reloading = false;
        });
    }

    static boolean done() {
        return !reloading;
    }

    private static String read(Minecraft minecraft) {
        return read(minecraft, "ladder/value.txt");
    }

    private static String read(Minecraft minecraft, String path) {
        Optional<Resource> resource = minecraft.getResourceManager().getResource(Payloads.id(path));
        if (resource.isEmpty()) {
            return "none";
        }
        try (InputStream in = resource.get().open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim() + " from " + resource.get().sourcePackId();
        } catch (IOException e) {
            return "error " + e.getMessage();
        }
    }
}
