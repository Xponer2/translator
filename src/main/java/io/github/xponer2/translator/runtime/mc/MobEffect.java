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

import io.github.xponer2.translator.entity.MobEffectCategory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

/**
 * Facade of the 1.21 {@code MobEffect} (neoforge-1.21.1-supers.tsv): a 1.12.2 potion of a category and color whose
 * ticks, instant effect and name are the mod's 1.21 methods.
 */
public class MobEffect extends Potion {
	private final MobEffectCategory category;

	public MobEffect(MobEffectCategory category, int color) {
		super(category == MobEffectCategory.HARMFUL, color);
		this.category = category;
		if (category == MobEffectCategory.BENEFICIAL) {
			this.setBeneficial();
		}
	}

	public MobEffectCategory getCategory() {
		return this.category;
	}

	// --- the 1.21 methods a mod overrides ----------------------------------------------------------------------------

	/** One tick of the effect; false removes it (1.21). */
	public boolean applyEffectTick(EntityLivingBase entity, int amplifier) {
		return true;
	}

	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return false;
	}

	public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirectSource, EntityLivingBase entity, int amplifier, double health) {
		this.applyEffectTick(entity, amplifier);
	}

	public boolean isInstantenous() {
		return false;
	}

	public void onEffectStarted(EntityLivingBase entity, int amplifier) {
	}

	public String getDescriptionId() {
		ResourceLocation id = this.getRegistryName();
		return id == null ? this.getName() : "effect." + id.getNamespace() + "." + id.getPath();
	}

	/** Attribute modifiers while the effect is on (1.21 builder style): the 1.12.2 potion modifier. */
	public MobEffect addAttributeModifier(io.github.xponer2.translator.registry.Holder<io.github.xponer2.translator.entity.Attribute> attribute,
			ResourceLocation id, double amount, io.github.xponer2.translator.entity.AttributeModifier.Operation operation) {
		var forge = attribute.value().forge();
		if (forge != null) {
			this.registerPotionAttributeModifier(forge, io.github.xponer2.translator.entity.AttributeModifier.uuidOf(id).toString(), amount,
					operation.ordinal());
		}
		return this;
	}

	/** The attribute modifiers of the effect at an amplifier (1.21): the 1.12.2 potion applies its own. */
	public void createModifiers(int amplifier, java.util.function.BiConsumer<?, ?> output) {
	}

	// --- the 1.12.2 calls, to the 1.21 methods -----------------------------------------------------------------------

	@Override
	public boolean isReady(int duration, int amplifier) {
		return this.shouldApplyEffectTickThisTick(duration, amplifier);
	}

	@Override
	public void performEffect(EntityLivingBase entity, int amplifier) {
		if (!this.applyEffectTick(entity, amplifier)) {
			entity.removePotionEffect(this);
		}
	}

	@Override
	public void affectEntity(@Nullable Entity source, @Nullable Entity indirectSource, EntityLivingBase entity, int amplifier, double health) {
		this.applyInstantenousEffect(source, indirectSource, entity, amplifier, health);
	}

	@Override
	public boolean isInstant() {
		return this.isInstantenous();
	}

	@Override
	public String getName() {
		ResourceLocation id = this.getRegistryName();
		return id == null ? super.getName() : "effect." + id.getNamespace() + "." + id.getPath();
	}

	/** 1.12.2 draws effects by an index of its own texture: a mod's effect has none. */
	@Override
	public boolean hasStatusIcon() {
		return false;
	}
}
