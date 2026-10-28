package io.github.xponer2.translator.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 finds an item's model by its id (models/item/&lt;id&gt;.json); 1.12.2 needs it registered. Every item registered
 * through the Translator gets its "inventory" model unless a mod set one itself.
 */
public final class ItemModels {
	private static final List<Item> ITEMS = new ArrayList<>();

	private ItemModels() {
	}

	/** Called by the Translator's registries for every item they register. */
	public static synchronized void track(Item item) {
		ITEMS.add(item);
	}

	@SideOnly(Side.CLIENT)
	public static final class Registrar {
		@SubscribeEvent
		public void onModelRegistry(ModelRegistryEvent event) {
			synchronized (ItemModels.class) {
				for (var item : ITEMS) {
					var id = item.getRegistryName();
					if (id != null) {
						ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(id, "inventory"));
					}
				}
			}
		}
	}
}
