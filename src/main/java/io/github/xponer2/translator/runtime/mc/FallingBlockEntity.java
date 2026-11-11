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

import io.github.xponer2.translator.entity.SynchedDataBuilder;
import io.github.xponer2.translator.registry.EntityType;

import net.minecraft.world.World;

/**
 * 1.21 {@code FallingBlockEntity} for the mods' falling blocks that extend it (Moonlight's improved falling block, Iron's Spells' visual one): made with their EntityType (1.12.2's
 * EntityFallingBlock has no such constructor). The table maps the class to 1.12.2's; a mod's own one extends this instead (the E
 * line: calls on FallingBlockEntity itself stay 1.12.2's). Without it the mod's constructor couldn't run and no such entity existed. It falls as sand until the mod sets its block (1.21's default; 1.12.2 left none and its update crashed).
 */
public abstract class FallingBlockEntity extends net.minecraft.entity.item.EntityFallingBlock {
	private final EntityType<?> type;

	protected FallingBlockEntity(EntityType<?> type, World world) {
		super(world);
		this.type = type;
		this.preventEntitySpawning = true;
		if (type != null) {
			this.setSize(type.getWidth(), type.getHeight());
		} else {
			this.setSize(0.98F, 0.98F);
		}
		withDefaultBlock(this);
	}

	/** Gives a falling block without a block sand (1.21's default), which 1.12.2's update reads. */
	public static void withDefaultBlock(net.minecraft.entity.item.EntityFallingBlock entity) {
		if (entity.getBlock() != null) {
			return;
		}
		for (java.lang.reflect.Field field : net.minecraft.entity.item.EntityFallingBlock.class.getDeclaredFields()) {
			if (field.getType() == net.minecraft.block.state.IBlockState.class) {
				try {
					field.setAccessible(true);
					field.set(entity, net.minecraft.init.Blocks.SAND.getDefaultState());
				} catch (ReflectiveOperationException | RuntimeException e) {
					io.github.xponer2.translator.runtime.RuntimeStubs.failed("FallingBlockEntity: its default block", e);
				}
				return;
			}
		}
	}

	public EntityType<?> getType() {
		return MobFacades.type(this.type, this);
	}

	/** 1.21 {@code defineSynchedData(builder)}: the mod's data, after 1.12.2's. */
	protected void defineSynchedData(SynchedDataBuilder builder) {
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.defineSynchedData(SynchedDataBuilder.of(this));
	}
}
