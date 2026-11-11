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
 * 1.21 {@code LightningBolt} for the mods' lightning that extend it (Ars Nouveau's): made with their EntityType (1.12.2's
 * EntityLightningBolt has no such constructor). The table maps the class to 1.12.2's; a mod's own one extends this instead (the E
 * line: calls on LightningBolt itself stay 1.12.2's). Without it the mod's constructor couldn't run and no such entity existed. Made below the world (1.12.2's constructor sets fire around where it's made, nothing is loaded
 * there); the mod moves it where it strikes and its updates set fire there.
 */
public abstract class LightningBolt extends net.minecraft.entity.effect.EntityLightningBolt {
	private final EntityType<?> type;

	protected LightningBolt(EntityType<?> type, World world) {
		super(world, 0.0D, -100.0D, 0.0D, false);
		this.type = type;
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
