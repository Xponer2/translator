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

import net.minecraft.block.BlockButton;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Facade of the 1.21 {@code ButtonBlock}: a 1.12.2 button (wooden when its material is wood), with properties. */
public class ButtonBlock extends BlockButton {
	private final boolean wooden;

	public ButtonBlock(Object blockSetType, int ticksToStayPressed, BlockProperties properties) {
		super(properties.material() == Material.WOOD);
		this.wooden = properties.material() == Material.WOOD;
		properties.applyTo(this, this::setSoundType);
	}

	@Override
	protected void playClickSound(EntityPlayer player, World world, BlockPos pos) {
		world.playSound(player, pos, this.wooden ? SoundEvents.BLOCK_WOOD_BUTTON_CLICK_ON : SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
				0.3F, 0.6F);
	}

	@Override
	protected void playReleaseSound(World world, BlockPos pos) {
		world.playSound(null, pos, this.wooden ? SoundEvents.BLOCK_WOOD_BUTTON_CLICK_OFF : SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS,
				0.3F, 0.5F);
	}
}
