package io.github.xponer2.translator.registry;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonParser;

import io.github.xponer2.translator.Translator;

import net.minecraft.util.ResourceLocation;

/**
 * 1.21 ids of registry entries 1.12.2 has under another name or as a similar entry ({@code data/translator/aliases.json}:
 * registry path -> 1.21 id -> 1.12.2 id), e.g. {@code minecraft:sweeping_edge} -> {@code minecraft:sweeping}.
 */
public final class Aliases {
	private static Map<String, Map<ResourceLocation, ResourceLocation>> aliases;

	private Aliases() { }

	@Nullable
	public static synchronized ResourceLocation get(ResourceLocation registry, ResourceLocation id) {
		if (aliases == null) {
			aliases = read();
		}
		var map = aliases.get(registry.getPath());
		return map == null ? null : map.get(id);
	}

	private static Map<String, Map<ResourceLocation, ResourceLocation>> read() {
		var result = new HashMap<String, Map<ResourceLocation, ResourceLocation>>();
		var stream = Aliases.class.getResourceAsStream("/data/translator/aliases.json");
		if (stream == null) {
			Translator.LOGGER.error("data/translator/aliases.json is missing");
			return result;
		}
		try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			for (var registry : new JsonParser().parse(reader).getAsJsonObject().entrySet()) {
				var map = new HashMap<ResourceLocation, ResourceLocation>();
				for (var entry : registry.getValue().getAsJsonObject().entrySet()) {
					map.put(io.github.xponer2.translator.util.ResourceLocations.parse(entry.getKey()), io.github.xponer2.translator.util.ResourceLocations.parse(entry.getValue().getAsString()));
				}
				result.put(registry.getKey(), map);
			}
		} catch (Exception e) {
			Translator.LOGGER.error("Couldn't read data/translator/aliases.json", e);
		}
		return result;
	}
}
