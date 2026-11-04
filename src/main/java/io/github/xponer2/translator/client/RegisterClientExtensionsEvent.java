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

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * NeoForge client extension registration (items), posted by the Translator on the client in init; {@link HandRenderer}
 * applies the hand transforms.
 */
@SideOnly(Side.CLIENT)
public class RegisterClientExtensionsEvent extends Event {
	private static final Map<Item, IClientItemExtensions> ITEMS = new IdentityHashMap<>();

	/**
	 * NeoForge {@code registerFluidType(extensions, types...)}: the still and flowing textures and the tint of the
	 * fluids of those types go to their Forge fluids (the 1.21 classes are the carried ones, read by name).
	 */
	public void registerFluidType(Object extensions, Object[] types) {
		var wanted = java.util.Arrays.asList(types);
		net.minecraft.util.ResourceLocation still = (net.minecraft.util.ResourceLocation) call(extensions, "getStillTexture");
		net.minecraft.util.ResourceLocation flowing = (net.minecraft.util.ResourceLocation) call(extensions, "getFlowingTexture");
		Object tint = call(extensions, "getTintColor");
		for (var fluid : io.github.xponer2.translator.fluids.FluidBase.all()) {
			if (wanted.contains(call(fluid, "getFluidType"))) {
				fluid.setTextures(still, flowing, tint instanceof Integer color ? color : -1);
			}
		}
	}

	@javax.annotation.Nullable
	private static Object call(Object target, String method) {
		try {
			var m = target.getClass().getMethod(method);
			m.setAccessible(true);
			return m.invoke(target);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	/**
	 * NeoForge {@code registerBlock(extensions, blocks...)}: hit and break particles, the 1.12.2 blocks make their own
	 * (Sophisticated Storage's barrels registered theirs here, and the rest of its client setup was skipped).
	 */
	public void registerBlock(Object extensions, net.minecraft.block.Block... blocks) {
	}

	/** NeoForge {@code registerMobEffect(extensions, effects...)}: the 1.12.2 effects draw their own icons. */
	public void registerMobEffect(Object extensions, Object... effects) {
	}

	public void registerItem(IClientItemExtensions extensions, Item... items) {
		for (var item : items) {
			ITEMS.put(item, extensions);
			// the item's own renderer of its built-in model (a "builtin/entity" model): 1.12.2 asks the item for it
			try {
				var renderer = extensions.getCustomRenderer();
				if (renderer != null && renderer != net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer.instance) {
					item.setTileEntityItemStackRenderer(renderer);
				}
			} catch (RuntimeException | LinkageError e) {
				io.github.xponer2.translator.runtime.RuntimeStubs.failed("the item renderer of " + item.getRegistryName(), e);
			}
		}
	}

	public static final class HandRenderer {
		@SubscribeEvent
		public void onRenderHand(RenderSpecificHandEvent event) {
			var stack = event.getItemStack();
			var extensions = stack.isEmpty() ? null : ITEMS.get(stack.getItem());
			var player = Minecraft.getMinecraft().player;
			if (extensions == null || player == null) {
				return;
			}

			var arm = event.getHand() == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
			extensions.applyHandTransform(player, arm == null ? EnumHandSide.RIGHT : arm, stack, event.getPartialTicks(),
					event.getEquipProgress(), event.getSwingProgress());
		}
	}
}
