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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderBoat;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The boat of a 1.21.1 mod: 1.12.2's boat with the texture of its wood, {@code <mod>:textures/entity/boat/<wood>.png}
 * (the wood from the boat's item: "fir_boat", "fir_chest_boat"). The chest of a chest boat isn't drawn.
 */
@SideOnly(Side.CLIENT)
public final class ModBoatRenderer extends RenderBoat {
	private static final Map<Item, ResourceLocation> TEXTURES = new ConcurrentHashMap<>();
	private static final ResourceLocation OAK = new ResourceLocation("textures/entity/boat/boat_oak.png");

	public ModBoatRenderer(RenderManager manager) {
		super(manager);
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityBoat boat) {
		Item item;
		try {
			item = boat.getItemBoat();
		} catch (RuntimeException e) {
			return OAK;
		}
		if (item == null || item.getRegistryName() == null) {
			return OAK;
		}
		return TEXTURES.computeIfAbsent(item, ModBoatRenderer::texture);
	}

	private static ResourceLocation texture(Item item) {
		ResourceLocation id = item.getRegistryName();
		String wood = id.getPath().replace("_chest_boat", "").replace("_chest_raft", "").replace("_boat", "").replace("_raft", "");
		for (String folder : new String[] {"boat", "chest_boat"}) {
			ResourceLocation texture = new ResourceLocation(id.getNamespace(), "textures/entity/" + folder + "/" + wood + ".png");
			try (var resource = Minecraft.getMinecraft().getResourceManager().getResource(texture)) {
				return texture;
			} catch (Exception e) {
				// not there
			}
		}
		return OAK;
	}
}
