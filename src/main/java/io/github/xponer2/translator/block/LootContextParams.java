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
package io.github.xponer2.translator.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.Vec3d;

/**
 * 1.21 loot context parameters of block drops.
 */
public final class LootContextParams {
	public static final Param<Vec3d> ORIGIN = new Param<>("origin");
	public static final Param<Entity> THIS_ENTITY = new Param<>("this_entity");
	public static final Param<ItemStack> TOOL = new Param<>("tool");
	public static final Param<IBlockState> BLOCK_STATE = new Param<>("block_state");
	public static final Param<TileEntity> BLOCK_ENTITY = new Param<>("block_entity");
	public static final Param<Float> EXPLOSION_RADIUS = new Param<>("explosion_radius");
	public static final Param<net.minecraft.entity.player.EntityPlayer> LAST_DAMAGE_PLAYER = new Param<>("last_damage_player");
	public static final Param<net.minecraft.util.DamageSource> DAMAGE_SOURCE = new Param<>("damage_source");
	public static final Param<Entity> ATTACKING_ENTITY = new Param<>("attacking_entity");
	public static final Param<Entity> DIRECT_ATTACKING_ENTITY = new Param<>("direct_attacking_entity");
	public static final Param<Integer> ENCHANTMENT_LEVEL = new Param<>("enchantment_level");
	public static final Param<Boolean> ENCHANTMENT_ACTIVE = new Param<>("enchantment_active");

	private LootContextParams() {
	}

	public static final class Param<T> {
		private final String name;

		Param(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}
