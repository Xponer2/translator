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
package io.github.xponer2.translator.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import io.github.xponer2.translator.util.ItemLike;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

/**
 * 1.21 {@code FoodProperties}: what eating an item gives (hunger, saturation, effects, what is left). An item gets it
 * with {@link ItemProperties#food}; eating is {@link #eat}, the 1.12.2 {@code ItemFood} steps.
 */
public final class FoodProperties {
	private final int nutrition;
	private final float saturation;
	private final boolean canAlwaysEat;
	private final float eatSeconds;
	private final Optional<ItemStack> usingConvertsTo;
	private final List<PossibleEffect> effects;

	public FoodProperties(int nutrition, float saturation, boolean canAlwaysEat, float eatSeconds, Optional<ItemStack> usingConvertsTo,
			List<PossibleEffect> effects) {
		this.nutrition = nutrition;
		this.saturation = saturation;
		this.canAlwaysEat = canAlwaysEat;
		this.eatSeconds = eatSeconds;
		this.usingConvertsTo = usingConvertsTo;
		this.effects = effects;
	}

	public int nutrition() {
		return this.nutrition;
	}

	/** The saturation points (1.21), not the 1.12.2 modifier. */
	public float saturation() {
		return this.saturation;
	}

	public boolean canAlwaysEat() {
		return this.canAlwaysEat;
	}

	public float eatSeconds() {
		return this.eatSeconds;
	}

	public int eatDurationTicks() {
		return (int) (this.eatSeconds * 20.0F);
	}

	public Optional<ItemStack> usingConvertsTo() {
		return this.usingConvertsTo;
	}

	public List<PossibleEffect> effects() {
		return this.effects;
	}

	/** 1.21 {@code LivingEntity.eat}: food, sound, statistics and effects; the stack shrinks unless creative. */
	public ItemStack eat(ItemStack stack, World level, EntityLivingBase entity) {
		if (entity instanceof EntityPlayer player) {
			// 1.12.2 adds food * modifier * 2 saturation; 1.21 keeps the points
			player.getFoodStats().addStats(this.nutrition, this.nutrition == 0 ? 0.0F : this.saturation / (this.nutrition * 2.0F));
			level.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5F,
					level.rand.nextFloat() * 0.1F + 0.9F);
			player.addStat(StatList.getObjectUseStats(stack.getItem()));
			if (player instanceof EntityPlayerMP serverPlayer) {
				net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
			}
		}
		if (!level.isRemote) {
			for (PossibleEffect effect : this.effects) {
				if (level.rand.nextFloat() < effect.probability()) {
					entity.addPotionEffect(effect.effect());
				}
			}
		}
		boolean creative = entity instanceof EntityPlayer player && player.capabilities.isCreativeMode;
		if (!creative) {
			stack.shrink(1);
		}
		if (this.usingConvertsTo.isPresent() && !creative) {
			ItemStack remainder = this.usingConvertsTo.get().copy();
			if (stack.isEmpty()) {
				return remainder;
			}
			if (entity instanceof EntityPlayer player && !player.inventory.addItemStackToInventory(remainder)) {
				player.dropItem(remainder, false);
			}
		}
		return stack;
	}

	/** An effect eating gives with a probability. */
	public static final class PossibleEffect {
		private final Supplier<PotionEffect> effect;
		private final float probability;

		public PossibleEffect(Supplier<PotionEffect> effect, float probability) {
			this.effect = effect;
			this.probability = probability;
		}

		/** A fresh copy (the effect instance is applied to an entity). */
		public PotionEffect effect() {
			return new PotionEffect(this.effect.get());
		}

		public float probability() {
			return this.probability;
		}
	}

	public static class Builder {
		private int nutrition;
		private float saturationModifier;
		private boolean canAlwaysEat;
		private float eatSeconds = 1.6F;
		private Optional<ItemStack> usingConvertsTo = Optional.empty();
		private final List<PossibleEffect> effects = new ArrayList<>();

		public Builder nutrition(int nutrition) {
			this.nutrition = nutrition;
			return this;
		}

		public Builder saturationModifier(float saturationModifier) {
			this.saturationModifier = saturationModifier;
			return this;
		}

		public Builder alwaysEdible() {
			this.canAlwaysEat = true;
			return this;
		}

		public Builder fast() {
			this.eatSeconds = 0.8F;
			return this;
		}

		public Builder effect(Supplier<PotionEffect> effect, float probability) {
			this.effects.add(new PossibleEffect(effect, probability));
			return this;
		}

		public Builder effect(PotionEffect effect, float probability) {
			return this.effect(() -> effect, probability);
		}

		public Builder usingConvertsTo(Object item) {
			this.usingConvertsTo = Optional.of(new ItemStack(ItemLike.item(item)));
			return this;
		}

		public FoodProperties build() {
			return new FoodProperties(this.nutrition, this.nutrition * this.saturationModifier * 2.0F, this.canAlwaysEat, this.eatSeconds,
					this.usingConvertsTo, Collections.unmodifiableList(new ArrayList<>(this.effects)));
		}
	}
}
