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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * NeoForge {@code DamageContainer}: the damage of one hit, which the mods' damage handlers read and change
 * ({@code event.getContainer().setNewDamage(..)}). Here over the 1.12.2 Forge event of that moment: LivingHurtEvent
 * (1.21's LivingIncomingDamageEvent, before armor) and LivingDamageEvent (1.21's LivingDamageEvent.Pre, after it). Its
 * original damage is the hit's before any change. Before, getContainer() returned nothing: Mekanism's, Iron's Spells',
 * Twilight Forest's and Ars Nouveau's damage changes (resistances, spell power, armor) were lost.
 */
public final class DamageContainer {
	/** The damage each entity was hit with first (LivingHurtEvent before any handler): the original damage. */
	private static final Map<EntityLivingBase, Float> ORIGINAL = Collections.synchronizedMap(new WeakHashMap<>());

	@javax.annotation.Nullable
	private final Object event;
	@javax.annotation.Nullable
	private final EntityLivingBase entity;
	private final DamageSource source;
	/** The damage of a container a mod made itself (no event behind it). */
	private float standalone;

	private DamageContainer(Object event, EntityLivingBase entity, DamageSource source) {
		this.event = event;
		this.entity = entity;
		this.source = source;
	}

	/** NeoForge {@code new DamageContainer(source, damage)}: a mod's own (Mekanism's laser against a shield). */
	public DamageContainer(DamageSource source, float damage) {
		this(null, null, source);
		this.standalone = damage;
	}

	/** 1.21 {@code LivingIncomingDamageEvent.getContainer()}. */
	public static DamageContainer getContainer(LivingHurtEvent event) {
		return new DamageContainer(event, event.getEntityLiving(), event.getSource());
	}

	/** 1.21 {@code LivingDamageEvent.Pre.getContainer()}. */
	public static DamageContainer getContainer(LivingDamageEvent event) {
		return new DamageContainer(event, event.getEntityLiving(), event.getSource());
	}

	/** 1.21 {@code LivingDamageEvent.Pre.getOriginalDamage()}. */
	public static float getOriginalDamage(LivingDamageEvent event) {
		return original(event.getEntityLiving(), event.getAmount());
	}

	static float original(EntityLivingBase entity, float otherwise) {
		Float first = entity == null ? null : ORIGINAL.get(entity);
		return first == null ? otherwise : first;
	}

	public float getNewDamage() {
		if (this.event == null) {
			return this.standalone;
		}
		if (this.event instanceof LivingHurtEvent) {
			return ((LivingHurtEvent) this.event).getAmount();
		}
		return ((LivingDamageEvent) this.event).getAmount();
	}

	public void setNewDamage(float damage) {
		if (this.event == null) {
			this.standalone = damage;
		} else if (this.event instanceof LivingHurtEvent) {
			((LivingHurtEvent) this.event).setAmount(damage);
		} else {
			((LivingDamageEvent) this.event).setAmount(damage);
		}
	}

	public float getOriginalDamage() {
		return original(this.entity, this.getNewDamage());
	}

	public DamageSource getSource() {
		return this.source;
	}

	/** NeoForge {@code setPostAttackInvulnerabilityTicks(ticks)}: how long the entity can't be hurt again after this hit (Iron's Spells' fast spells). */
	public void setPostAttackInvulnerabilityTicks(int ticks) {
		if (this.entity != null) {
			this.entity.hurtResistantTime = ticks;
		}
	}

	/** Records the hits' original damage and posts 1.21's LivingDamageEvent.Post after the last damage handler. */
	public static final class Hooks {
		public static void register() {
			MinecraftForge.EVENT_BUS.register(new Hooks());
		}

		@SubscribeEvent(priority = EventPriority.HIGHEST)
		public void onHurt(LivingHurtEvent event) {
			if (event.getEntityLiving() != null) {
				ORIGINAL.put(event.getEntityLiving(), event.getAmount());
			}
		}

		@SubscribeEvent(priority = EventPriority.LOWEST)
		public void onDamage(LivingDamageEvent event) {
			EntityLivingBase entity = event.getEntityLiving();
			if (entity == null || entity.world.isRemote) {
				return;
			}
			float original = original(entity, event.getAmount());
			ORIGINAL.remove(entity);
			ServerEvents.post(new LivingDamagePostEvent(entity, event.getSource(), original, event.getAmount()));
		}
	}
}
