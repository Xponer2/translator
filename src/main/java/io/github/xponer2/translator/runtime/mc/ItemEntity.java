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

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.EntityType;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

/**
 * 1.21 {@code ItemEntity} for the item entities of 1.21.1 mods (a backpack that doesn't burn or fall out of the world):
 * made with their EntityType (1.12.2's takes the world only: none could be made), their {@code tick} called from
 * 1.12.2's onUpdate, {@code fireImmune} kept in 1.12.2's fire immunity (its getter is final there) and
 * {@code ignoreExplosion} answering 1.12.2's explosion immunity.
 */
public class ItemEntity extends EntityItem {
	@Nullable
	private final EntityType<?> type;

	public ItemEntity(EntityType<?> type, World world) {
		super(world);
		this.type = type;
	}

	public ItemEntity(World world, double x, double y, double z, ItemStack stack) {
		super(world, x, y, z, stack);
		this.type = null;
	}

	public ItemEntity(World world, double x, double y, double z, ItemStack stack, double motionX, double motionY, double motionZ) {
		super(world, x, y, z, stack);
		this.type = null;
		this.motionX = motionX;
		this.motionY = motionY;
		this.motionZ = motionZ;
	}

	@Nullable
	public EntityType<?> getType() {
		return this.type;
	}

	/** 1.21: the entity's tick, 1.12.2's item entity update. */
	public void tick() {
		super.onUpdate();
	}

	@Override
	public void onUpdate() {
		this.isImmuneToFire = this.fireImmune();
		this.tick();
	}

	/** 1.21: whether fire and lava don't hurt it; here what 1.12.2's item entity has. */
	public boolean fireImmune() {
		return super.isImmuneToFire();
	}

	/** 1.21: whether an explosion leaves it alone. */
	public boolean ignoreExplosion(@Nullable Explosion explosion) {
		return super.isImmuneToExplosions();
	}

	@Override
	public boolean isImmuneToExplosions() {
		return this.ignoreExplosion(null);
	}
}
