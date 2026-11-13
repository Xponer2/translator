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

import io.github.xponer2.translator.block.BlockProperties;
import io.github.xponer2.translator.block.LazyStateContainer;

import net.minecraft.block.BlockBed;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.item.EnumDyeColor;

/**
 * Facade of the 1.21 {@code BedBlock}: the 1.12.2 bed made with the 1.21 constructor (color, properties), its states
 * those the mod's createBlockStateDefinition adds. A mod's bed couldn't be made before (1.12.2's BlockBed takes no
 * arguments): Comforts' sleeping bags and hammocks were missing.
 */
public class BedBlock extends BlockBed {
	private final EnumDyeColor color;

	public BedBlock(EnumDyeColor color, BlockProperties properties) {
		super();
		this.color = color;
		properties.applyTo(this, this::setSoundType);
	}

	public EnumDyeColor getColor() {
		return this.color;
	}

	/** 1.21 {@code BedBlock.createBlockStateDefinition}: facing, part, occupied; a mod's bed adds its own. */
	protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
		builder.add(FACING, PART, OCCUPIED);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		var builder = new LazyStateContainer.Builder(this);
		this.createBlockStateDefinition(builder);
		return builder.build();
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
