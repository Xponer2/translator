/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.painting;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 1.21's painting variants are data (data/<namespace>/painting_variant/<name>.json: width, height, asset_id); 1.20.6's
 * are a built-in registry. The mods' variants are registered with it from their jars' data when the variants are
 * (the texture is found by the variant's id, which 1.21's asset id is for the mods' own paintings).
 */
public final class Paintings {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("ladder");

    private Paintings() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(Paintings::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.PAINTING_VARIANT)) {
            return;
        }
        for (var info : ModList.get().getModFiles()) {
            Path data = info.getFile().getSecureJar().getRootPath().resolve("data");
            if (!Files.isDirectory(data)) {
                continue;
            }
            try (Stream<Path> namespaces = Files.list(data)) {
                for (Path namespace : (Iterable<Path>) namespaces::iterator) {
                    Path folder = namespace.resolve("painting_variant");
                    if (Files.isDirectory(folder)) {
                        registerFolder(event, namespace.getFileName().toString().replace("/", ""), folder);
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("can't read the painting variants of {}", info.getFile().getFileName(), e);
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
                try (Reader reader = Files.newBufferedReader(file)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    PaintingVariant variant = new PaintingVariant(json.get("width").getAsInt(), json.get("height").getAsInt());
                    event.register(Registries.PAINTING_VARIANT, id, () -> variant);
                } catch (RuntimeException e) {
                    LOGGER.warn("a painting variant {} that can't be read: {}", id, e.toString());
                }
            }
        }
    }
}
