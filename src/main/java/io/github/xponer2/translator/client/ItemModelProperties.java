package io.github.xponer2.translator.client;

import io.github.xponer2.translator.item.ItemPropertyFunction;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 client ItemProperties#register: a model property of an item (1.12.2 property override).
 */
@SideOnly(Side.CLIENT)
public final class ItemModelProperties {
	private ItemModelProperties() {
	}

	public static void register(Item item, ResourceLocation name, ItemPropertyFunction function) {
		item.addPropertyOverride(name, (stack, world, entity) -> function.call(stack, world, entity, 0));
	}
}
