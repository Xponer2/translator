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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The vanilla 1.21 effects 1.12.2 hasn't (bad omen, slow falling, darkness...): mods give them, test for them and
 * name them (Iron's Spells' omens, Twilight Forest's, Supplementaries'). They were missing, so those calls did nothing.
 * Registered under their 1.21 ids. Slow falling and conduit power work as in 1.21; the others mark the entity for the
 * mods' own logic.
 */
public final class NewMobEffects {
	private static final List<Potion> ALL = new ArrayList<>();

	public static final Potion SLOW_FALLING = effect("slow_falling", false, 0xF3CFB9);
	public static final Potion CONDUIT_POWER = effect("conduit_power", false, 0x1DC2D1);
	public static final Potion DOLPHINS_GRACE = effect("dolphins_grace", false, 0x88A3BE);
	public static final Potion BAD_OMEN = effect("bad_omen", false, 0x0B6138);
	public static final Potion HERO_OF_THE_VILLAGE = effect("hero_of_the_village", false, 0x44FF44);
	public static final Potion DARKNESS = effect("darkness", true, 0x292721);
	public static final Potion TRIAL_OMEN = effect("trial_omen", false, 0x16A6A6);
	public static final Potion RAID_OMEN = effect("raid_omen", false, 0xDE4058);
	public static final Potion WIND_CHARGED = effect("wind_charged", true, 0xBDC9FF);
	public static final Potion WEAVING = effect("weaving", true, 0x78695A);
	public static final Potion OOZING = effect("oozing", true, 0x99FFA3);
	public static final Potion INFESTED = effect("infested", true, 0x8C9B8C);

	private static Potion effect(String name, boolean bad, int color) {
		Potion potion = new Effect(bad, color).setPotionName("effect.minecraft." + name);
		potion.setRegistryName("minecraft", name);
		ALL.add(potion);
		return potion;
	}

	@SubscribeEvent
	public void onRegister(RegistryEvent.Register<Potion> event) {
		for (Potion potion : ALL) {
			event.getRegistry().register(potion);
		}
	}

	@SubscribeEvent
	public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
		EntityLivingBase entity = event.getEntityLiving();
		if (entity.isPotionActive(SLOW_FALLING)) {
			// 1.21: a fall of 0.01 a tick instead of 0.08, and no fall damage
			if (entity.motionY < -0.1) {
				entity.motionY = -0.1;
			}
			entity.fallDistance = 0;
		}
		if (entity.isPotionActive(CONDUIT_POWER) && entity.isInWater()) {
			entity.setAir(300);
		}
	}

	private static final class Effect extends Potion {
		Effect(boolean bad, int color) {
			super(bad, color);
		}
	}
}
