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
 * A 1.21 blockstate with the variant "" is used for every state of the block; on 1.12.2 its "normal" variant (the
 * converted "") only for a block without properties. Blocks of the ported mods with properties and only that variant
 * get a state mapper that gives all their states the "normal" model.
 */
@SideOnly(Side.CLIENT)
public final class SingleVariantModels {
	@SubscribeEvent
	public void onModels(ModelRegistryEvent event) {
		for (Block block : ForgeRegistries.BLOCKS) {
			var id = block.getRegistryName();
			if (id != null && Translator.isPortedMod(id.getNamespace()) && !block.getBlockState().getProperties().isEmpty() && onlyNormal(id)) {
				var model = new ModelResourceLocation(id, "normal");
				ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
					@Override
					protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
						return model;
					}
				});
			}
		}
	}

	private static boolean onlyNormal(ResourceLocation id) {
		var file = new ResourceLocation(id.getNamespace(), "blockstates/" + id.getPath() + ".json");
		try (var resource = Minecraft.getMinecraft().getResourceManager().getResource(file);
				var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
			var json = new JsonParser().parse(reader);
			if (!json.isJsonObject() || !json.getAsJsonObject().has("variants")) {
				return false;
			}
			JsonObject variants = json.getAsJsonObject().getAsJsonObject("variants");
			return variants.entrySet().size() == 1 && variants.has("normal");
		} catch (Exception e) {
			// no blockstate file (the mod maps its states itself) or a broken one (logged by the model loader)
			return false;
		}
	}
}
