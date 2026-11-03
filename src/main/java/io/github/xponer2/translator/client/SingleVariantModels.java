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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.xponer2.translator.Translator;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The models of the ported mods' block states the way 1.21 picks them: a variant key of a 1.21 blockstate names only
 * some properties ("facing=north,support=tray" for every waterlogged and dry state alike, "" for all states), 1.12.2
 * looks a state up by all its properties. Blocks with properties whose blockstate has variants get a state mapper that
 * gives each state the first variant key whose properties it has (the converted "" is "normal").
 */
@SideOnly(Side.CLIENT)
public final class SingleVariantModels {
	@SubscribeEvent
	public void onModels(ModelRegistryEvent event) {
		for (Block block : ForgeRegistries.BLOCKS) {
			var id = block.getRegistryName();
			if (id == null || !Translator.isPortedMod(id.getNamespace()) || block.getBlockState().getProperties().isEmpty()) {
				continue;
			}
			var keys = variantKeys(id);
			if (keys == null || keys.isEmpty()) {
				continue;
			}
			ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
				@Override
				protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
					for (var key : keys) {
						if (matches(state, key.getValue())) {
							return new ModelResourceLocation(id, key.getKey());
						}
					}
					// no key fits: what 1.12.2 would look up (the model loader logs the missing variant)
					return new ModelResourceLocation(id, this.getPropertyString(state.getProperties()));
				}
			});
		}
	}

	private static boolean matches(IBlockState state, java.util.Map<String, String> wanted) {
		for (var entry : wanted.entrySet()) {
			boolean found = false;
			for (var property : state.getPropertyKeys()) {
				if (property.getName().equals(entry.getKey())) {
					found = valueName(property, state).equals(entry.getValue());
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static String valueName(net.minecraft.block.properties.IProperty property, IBlockState state) {
		return property.getName(state.getValue(property));
	}

	/** The variant keys of the block's blockstate file with the properties each names, in file order; null without one. */
	@javax.annotation.Nullable
	private static java.util.List<java.util.Map.Entry<String, java.util.Map<String, String>>> variantKeys(ResourceLocation id) {
		var file = new ResourceLocation(id.getNamespace(), "blockstates/" + id.getPath() + ".json");
		try (var resource = Minecraft.getMinecraft().getResourceManager().getResource(file);
				var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
			var json = new JsonParser().parse(reader);
			if (!json.isJsonObject() || !json.getAsJsonObject().has("variants")) {
				return null;
			}
			JsonObject variants = json.getAsJsonObject().getAsJsonObject("variants");
			var keys = new java.util.ArrayList<java.util.Map.Entry<String, java.util.Map<String, String>>>();
			for (var entry : variants.entrySet()) {
				var properties = new java.util.LinkedHashMap<String, String>();
				if (!"normal".equals(entry.getKey()) && !entry.getKey().isEmpty()) {
					for (String pair : entry.getKey().split(",")) {
						int eq = pair.indexOf('=');
						if (eq <= 0) {
							// a 1.12.2-style key ("inventory"): not a 1.21 blockstate
							return null;
						}
						properties.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
					}
				}
				keys.add(new java.util.AbstractMap.SimpleImmutableEntry<>(entry.getKey(), properties));
			}
			return keys;
		} catch (Exception e) {
			// no blockstate file (the mod maps its states itself) or a broken one (logged by the model loader)
			return null;
		}
	}
}
