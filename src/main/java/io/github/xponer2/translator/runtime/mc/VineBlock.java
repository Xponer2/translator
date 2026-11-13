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
package io.github.xponer2.translator.runtime.mc;

import java.util.Collections;
import java.util.List;

import io.github.xponer2.translator.block.BlockProperties;

import net.minecraft.block.BlockVine;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Facade of the 1.21 {@code VineBlock}: the 1.12.2 vine (the same up/north/east/south/west properties, climbing,
 * spreading), with properties. 1.12.2's has no constructor taking properties, so a mod's vine wasn't made at all
 * (Biomes O' Plenty's willow vine); shears give this vine, not the vanilla one.
 */
public class VineBlock extends BlockVine {
	public VineBlock(BlockProperties properties) {
		super();
		properties.applyTo(this, this::setSoundType);
	}

	@Override
	public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
		return Collections.singletonList(new ItemStack(this));
	}

	/**
	 * The layer the block is drawn in: the mod's choice or its model's render_type (a hedge of leaves, a door with
	 * glass: drawn solid by the 1.12.2 class this one extends, their holes showed as a background), else 1.12.2's.
	 */
	@Override
	@net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
	public net.minecraft.util.BlockRenderLayer getRenderLayer() {
		net.minecraft.util.BlockRenderLayer layer = io.github.xponer2.translator.client.RenderLayers.of(this);
		return layer != null ? layer : super.getRenderLayer();
	}
}
