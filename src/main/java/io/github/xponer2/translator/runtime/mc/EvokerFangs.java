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
 * 1.21 {@code EvokerFangs} for the mods' fangs that extend it (Ars Nouveau's): made with their EntityType (1.12.2's
 * EntityEvokerFangs has no such constructor). The table maps the class to 1.12.2's; a mod's own one extends this instead (the E
 * line: calls on EvokerFangs itself stay 1.12.2's). Without it the mod's constructor couldn't run and no such entity existed.
 */
public abstract class EvokerFangs extends net.minecraft.entity.projectile.EntityEvokerFangs {
	private final EntityType<?> type;

	protected EvokerFangs(EntityType<?> type, World world) {
		super(world);
		this.type = type;
		if (type != null) {
			this.setSize(type.getWidth(), type.getHeight());
		}
	}

	protected EvokerFangs(World world, double x, double y, double z, float yaw, int warmup, @javax.annotation.Nullable net.minecraft.entity.EntityLivingBase caster) {
		super(world, x, y, z, yaw, warmup, caster);
		this.type = null;
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
