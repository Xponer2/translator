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
package io.github.xponer2.translator.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.Holder;

import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;

/**
 * 1.21 {@code PotionContents} (the potion_contents component): the potion of a stack, its own color and extra effects.
 * On 1.12.2 they are the stack's "Potion", "CustomPotionColor" and "CustomPotionEffects" tags, read and written by
 * {@link DataComponents#POTION_CONTENTS}, so vanilla potions and the mods' see the same.
 */
public final class PotionContents {
	public static final PotionContents EMPTY = new PotionContents(Optional.empty(), Optional.empty(), Collections.emptyList());

	private final Optional<Holder<PotionType>> potion;
	private final Optional<Integer> customColor;
	private final List<PotionEffect> customEffects;

	public PotionContents(Optional<Holder<PotionType>> potion, Optional<Integer> customColor, List<PotionEffect> customEffects) {
		this.potion = potion;
		this.customColor = customColor;
		this.customEffects = customEffects;
	}

	public PotionContents(Holder<PotionType> potion) {
		this(Optional.of(potion), Optional.empty(), Collections.emptyList());
	}

	public static ItemStack createItemStack(Item item, Holder<PotionType> potion) {
		ItemStack stack = new ItemStack(item);
		PotionUtils.addPotionToItemStack(stack, potion.value());
		return stack;
	}

	public Optional<Holder<PotionType>> potion() {
		return this.potion;
	}

	public Optional<Integer> customColor() {
		return this.customColor;
	}

	public List<PotionEffect> customEffects() {
		return this.customEffects;
	}

	public PotionContents withPotion(Holder<PotionType> potion) {
		return new PotionContents(Optional.of(potion), this.customColor, this.customEffects);
	}

	public PotionContents withEffectAdded(PotionEffect effect) {
		var effects = new ArrayList<>(this.customEffects);
		effects.add(effect);
		return new PotionContents(this.potion, this.customColor, effects);
	}

	public boolean is(Holder<PotionType> potion) {
		return this.potion.isPresent() && this.potion.get().value() == potion.value();
	}

	public Iterable<PotionEffect> getAllEffects() {
		var effects = new ArrayList<PotionEffect>();
		this.forEachEffect(effects::add);
		return effects;
	}

	public void forEachEffect(Consumer<PotionEffect> action) {
		this.potion.ifPresent(holder -> holder.value().getEffects().forEach(effect -> action.accept(new PotionEffect(effect))));
		this.customEffects.forEach(effect -> action.accept(new PotionEffect(effect)));
	}

	public boolean hasEffects() {
		return !this.customEffects.isEmpty() || this.potion.isPresent() && !this.potion.get().value().getEffects().isEmpty();
	}

	public int getColor() {
		return this.customColor.orElseGet(() -> getColor(this.getAllEffects()));
	}

	public static int getColor(Holder<PotionType> potion) {
		return PotionUtils.getPotionColor(potion.value());
	}

	public static int getColor(Iterable<PotionEffect> effects) {
		var list = new ArrayList<PotionEffect>();
		effects.forEach(list::add);
		return PotionUtils.getPotionColorFromEffectList(list);
	}

	/** The contents a 1.12.2 stack has (null when it has none of the potion tags). */
	@Nullable
	static PotionContents read(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null || !tag.hasKey("Potion") && !tag.hasKey("CustomPotionEffects") && !tag.hasKey("CustomPotionColor")) {
			return null;
		}
		PotionType type = PotionUtils.getPotionFromItem(stack);
		var custom = new ArrayList<PotionEffect>();
		PotionUtils.addCustomPotionEffectToList(tag, custom);
		return new PotionContents(type == PotionTypes.EMPTY ? Optional.empty() : Optional.of(Holder.direct(type)),
				tag.hasKey("CustomPotionColor", 99) ? Optional.of(tag.getInteger("CustomPotionColor")) : Optional.empty(), custom);
	}

	/** Writes the contents as 1.12.2's potion tags (null removes them). */
	static void write(ItemStack stack, @Nullable PotionContents contents) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag != null) {
			tag.removeTag("Potion");
			tag.removeTag("CustomPotionEffects");
			tag.removeTag("CustomPotionColor");
		}
		if (contents == null) {
			return;
		}
		contents.potion.ifPresent(holder -> PotionUtils.addPotionToItemStack(stack, holder.value()));
		if (!contents.customEffects.isEmpty()) {
			PotionUtils.appendEffects(stack, contents.customEffects);
		}
		contents.customColor.ifPresent(color -> {
			if (!stack.hasTagCompound()) {
				stack.setTagCompound(new NBTTagCompound());
			}
			stack.getTagCompound().setInteger("CustomPotionColor", color);
		});
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof PotionContents contents && this.potion.map(Holder::value).equals(contents.potion.map(Holder::value))
				&& this.customColor.equals(contents.customColor) && this.customEffects.equals(contents.customEffects);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.potion.map(Holder::value), this.customColor, this.customEffects);
	}

	@Override
	public String toString() {
		return "PotionContents[potion=" + this.potion.map(Holder::value) + ", customColor=" + this.customColor + ", customEffects=" + this.customEffects + "]";
	}

	/** 1.21 {@code addPotionTooltip(tooltip, durationFactor, ticksPerSecond)}: a line per effect, as 1.12.2's potion tooltip. */
	public void addPotionTooltip(Consumer<net.minecraft.util.text.ITextComponent> tooltip, float durationFactor, float ticksPerSecond) {
		addPotionTooltip(this.getAllEffects(), tooltip, durationFactor, ticksPerSecond);
	}

	/** 1.21 {@code PotionContents.addPotionTooltip(effects, tooltip, durationFactor, ticksPerSecond)}. */
	public static void addPotionTooltip(Iterable<PotionEffect> effects, Consumer<net.minecraft.util.text.ITextComponent> tooltip, float durationFactor,
			float ticksPerSecond) {
		boolean any = false;
		for (PotionEffect effect : effects) {
			any = true;
			net.minecraft.util.text.ITextComponent line = new net.minecraft.util.text.TextComponentTranslation(effect.getEffectName());
			if (effect.getAmplifier() > 0) {
				line.appendText(" ").appendSibling(new net.minecraft.util.text.TextComponentTranslation("potion.potency." + effect.getAmplifier()));
			}
			if (effect.getDuration() > 20) {
				String time = effect.getIsPotionDurationMax() ? "**:**"
						: net.minecraft.util.StringUtils.ticksToElapsedTime((int) (effect.getDuration() * durationFactor));
				line.appendText(" (" + time + ")");
			}
			line.getStyle().setColor(effect.getPotion().isBadEffect() ? net.minecraft.util.text.TextFormatting.RED
					: net.minecraft.util.text.TextFormatting.BLUE);
			tooltip.accept(line);
		}
		if (!any) {
			var none = new net.minecraft.util.text.TextComponentTranslation("effect.none");
			none.getStyle().setColor(net.minecraft.util.text.TextFormatting.GRAY);
			tooltip.accept(none);
		}
	}
}
