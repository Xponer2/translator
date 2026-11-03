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
package io.github.xponer2.translator.runtime.redirect;

import io.github.xponer2.translator.registry.Holder;
import io.github.xponer2.translator.util.BuiltInRegistries;

import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;

/**
 * 1.21 {@code MobEffects}: holders of the vanilla effects 1.12.2 has (1.12.2's class has the effects themselves,
 * some under other names). A GETSTATIC of a 1.21 effect reads here.
 */
public final class MobEffectHolders {
	public static final Holder<Potion> MOVEMENT_SPEED = holder(MobEffects.SPEED);
	public static final Holder<Potion> MOVEMENT_SLOWDOWN = holder(MobEffects.SLOWNESS);
	public static final Holder<Potion> DIG_SPEED = holder(MobEffects.HASTE);
	public static final Holder<Potion> DIG_SLOWDOWN = holder(MobEffects.MINING_FATIGUE);
	public static final Holder<Potion> DAMAGE_BOOST = holder(MobEffects.STRENGTH);
	public static final Holder<Potion> HEAL = holder(MobEffects.INSTANT_HEALTH);
	public static final Holder<Potion> HARM = holder(MobEffects.INSTANT_DAMAGE);
	public static final Holder<Potion> JUMP = holder(MobEffects.JUMP_BOOST);
	public static final Holder<Potion> CONFUSION = holder(MobEffects.NAUSEA);
	public static final Holder<Potion> REGENERATION = holder(MobEffects.REGENERATION);
	public static final Holder<Potion> DAMAGE_RESISTANCE = holder(MobEffects.RESISTANCE);
	public static final Holder<Potion> FIRE_RESISTANCE = holder(MobEffects.FIRE_RESISTANCE);
	public static final Holder<Potion> WATER_BREATHING = holder(MobEffects.WATER_BREATHING);
	public static final Holder<Potion> INVISIBILITY = holder(MobEffects.INVISIBILITY);
	public static final Holder<Potion> BLINDNESS = holder(MobEffects.BLINDNESS);
	public static final Holder<Potion> NIGHT_VISION = holder(MobEffects.NIGHT_VISION);
	public static final Holder<Potion> HUNGER = holder(MobEffects.HUNGER);
	public static final Holder<Potion> WEAKNESS = holder(MobEffects.WEAKNESS);
	public static final Holder<Potion> POISON = holder(MobEffects.POISON);
	public static final Holder<Potion> WITHER = holder(MobEffects.WITHER);
	public static final Holder<Potion> HEALTH_BOOST = holder(MobEffects.HEALTH_BOOST);
	public static final Holder<Potion> ABSORPTION = holder(MobEffects.ABSORPTION);
	public static final Holder<Potion> SATURATION = holder(MobEffects.SATURATION);
	public static final Holder<Potion> GLOWING = holder(MobEffects.GLOWING);
	public static final Holder<Potion> LEVITATION = holder(MobEffects.LEVITATION);
	public static final Holder<Potion> LUCK = holder(MobEffects.LUCK);
	public static final Holder<Potion> UNLUCK = holder(MobEffects.UNLUCK);

	private MobEffectHolders() {
	}

	private static Holder<Potion> holder(Potion potion) {
		return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(potion);
	}
}
