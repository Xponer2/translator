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
package io.github.xponer2.translator.client;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The render layer of a 1.21.1 mod's block: set by the mod ({@code ItemBlockRenderTypes.setRenderLayer}), else the
 * {@code render_type} of its 1.21 block model (cutout plants and doors, translucent glass), found through its
 * blockstate file and the models' parents. Null when neither says (the block decides).
 */
@SideOnly(Side.CLIENT)
public final class RenderLayers {
	private static final Map<Block, BlockRenderLayer> SET = new ConcurrentHashMap<>();
	private static final Map<Block, Optional<BlockRenderLayer>> FROM_MODELS = new ConcurrentHashMap<>();

	private RenderLayers() {
	}

	/** 1.21 {@code ItemBlockRenderTypes.setRenderLayer(block, type)}. */
	public static void setRenderLayer(Block block, @Nullable RenderType type) {
		BlockRenderLayer layer = type == null ? null : layer(type.getChunkLayerId());
		if (block != null && layer != null) {
			SET.put(block, layer);
		}
	}

	public static void setRenderLayer(Block block, java.util.function.Predicate<RenderType> predicate) {
		for (RenderType type : new RenderType[] {RenderType.translucent(), RenderType.cutout(), RenderType.cutoutMipped()}) {
			if (predicate.test(type)) {
				setRenderLayer(block, type);
				return;
			}
		}
	}

	@Nullable
	public static BlockRenderLayer of(Block block) {
		BlockRenderLayer set = SET.get(block);
		if (set != null) {
			return set;
		}
		return FROM_MODELS.computeIfAbsent(block, RenderLayers::fromModels).orElse(null);
	}

	@Nullable
	private static BlockRenderLayer layer(int chunkLayer) {
		switch (chunkLayer) {
		case 0:
			return BlockRenderLayer.SOLID;
		case 1:
			return BlockRenderLayer.CUTOUT_MIPPED;
		case 2:
			return BlockRenderLayer.CUTOUT;
		case 3:
			return BlockRenderLayer.TRANSLUCENT;
		default:
			return null;
		}
	}

	@Nullable
	private static BlockRenderLayer layer(String renderType) {
		String name = renderType.substring(renderType.indexOf(':') + 1);
		switch (name) {
		case "cutout":
			return BlockRenderLayer.CUTOUT;
		case "cutout_mipped":
		case "cutout_mipped_all":
			return BlockRenderLayer.CUTOUT_MIPPED;
		case "translucent":
			return BlockRenderLayer.TRANSLUCENT;
		case "solid":
			return BlockRenderLayer.SOLID;
		default:
			return null;
		}
	}

	private static Optional<BlockRenderLayer> fromModels(Block block) {
		ResourceLocation id = block.getRegistryName();
		if (id == null || "minecraft".equals(id.getNamespace())) {
			return Optional.empty();
		}
		JsonObject blockstate = json(new ResourceLocation(id.getNamespace(), "blockstates/" + id.getPath() + ".json"));
		if (blockstate == null) {
			return Optional.empty();
		}
		var models = new java.util.LinkedHashSet<String>();
		collectModels(blockstate, models);
		for (String model : models) {
			BlockRenderLayer layer = modelLayer(model, 0);
			if (layer != null) {
				return Optional.of(layer);
			}
		}
		return Optional.empty();
	}

	/** Every "model" string of a blockstate file (variants and multipart, arrays of them). */
	private static void collectModels(JsonElement element, java.util.Set<String> models) {
		if (element.isJsonObject()) {
			for (var entry : element.getAsJsonObject().entrySet()) {
				if ("model".equals(entry.getKey()) && entry.getValue().isJsonPrimitive()) {
					models.add(entry.getValue().getAsString());
				} else {
					collectModels(entry.getValue(), models);
				}
			}
		} else if (element.isJsonArray()) {
			for (JsonElement item : element.getAsJsonArray()) {
				collectModels(item, models);
			}
		}
	}

	@Nullable
	private static BlockRenderLayer modelLayer(String model, int depth) {
		if (depth > 4) {
			return null;
		}
		ResourceLocation id = new ResourceLocation(model);
		JsonObject json = json(new ResourceLocation(id.getNamespace(), "models/" + id.getPath() + ".json"));
		if (json == null && id.getPath().indexOf('/') < 0) {
			// the blockstates 1.12.2 reads name the model without its folder (RuntimeResourcePack: "ae2:block/quartz_cluster"
			// is "ae2:quartz_cluster"), 1.12.2 looks in block/: missed, every cutout crystal and glass was drawn solid
			json = json(new ResourceLocation(id.getNamespace(), "models/block/" + id.getPath() + ".json"));
		}
		if (json == null) {
			return null;
		}
		if (json.has("render_type") && json.get("render_type").isJsonPrimitive()) {
			return layer(json.get("render_type").getAsString());
		}
		return json.has("parent") && json.get("parent").isJsonPrimitive() ? modelLayer(json.get("parent").getAsString(), depth + 1) : null;
	}

	@Nullable
	private static JsonObject json(ResourceLocation location) {
		try (var resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
				var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
			JsonElement element = new JsonParser().parse(reader);
			return element.isJsonObject() ? element.getAsJsonObject() : null;
		} catch (Exception e) {
			return null;
		}
	}
}
