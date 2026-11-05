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
package io.github.xponer2.translator.client.model;

import java.util.Collections;

import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code ItemOverrides} for the mods' own (S row): 1.12.2 asks {@code handleItemState}, the mod
 * answers in {@code resolve} (the model of that stack: a backpack's colors and upgrades, a tank's fluid).
 */
@SideOnly(Side.CLIENT)
public class ItemOverrides extends ItemOverrideList {
	public ItemOverrides() {
		super(Collections.emptyList());
	}

	/** 1.21 {@code resolve(model, stack, level, entity, seed)}: the model itself (a mod's override chooses). */
	@Nullable
	public IBakedModel resolve(IBakedModel model, ItemStack stack, @Nullable WorldClient level, @Nullable EntityLivingBase entity, int seed) {
		return model;
	}

	@Override
	public IBakedModel handleItemState(IBakedModel original, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
		try {
			IBakedModel resolved = this.resolve(original, stack, world instanceof WorldClient client ? client : null, entity, 0);
			return resolved != null ? resolved : original;
		} catch (RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".resolve", e);
			return original;
		}
	}
}
