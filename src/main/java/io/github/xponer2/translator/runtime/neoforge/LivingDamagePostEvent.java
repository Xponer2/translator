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
package io.github.xponer2.translator.runtime.neoforge;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * NeoForge {@code LivingDamageEvent.Post}: an entity took a hit's damage (the mods react: life steal, on-hit effects).
 * 1.12.2 has no such event; posted after the last LivingDamageEvent handler, with the damage it leaves.
 */
public class LivingDamagePostEvent extends LivingEvent {
	private final DamageSource source;
	private final float originalDamage;
	private final float newDamage;

	public LivingDamagePostEvent(EntityLivingBase entity, DamageSource source, float originalDamage, float newDamage) {
		super(entity);
		this.source = source;
		this.originalDamage = originalDamage;
		this.newDamage = newDamage;
	}

	public DamageSource getSource() {
		return this.source;
	}

	public float getOriginalDamage() {
		return this.originalDamage;
	}

	public float getNewDamage() {
		return this.newDamage;
	}
}
