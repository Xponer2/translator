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
import java.util.List;

/**
 * The attribute modifiers of an item (1.21 {@code ItemAttributeModifiers}, neoforge-1.21.1-runtime.tsv): a tool's
 * attack damage and speed ({@code createAttributes}), and what a builder added (kept, the 1.21 item facades read the
 * damage and speed).
 */
public final class ItemAttributeModifiers {
	public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(0.0F, 0.0F);

	private final float attackDamage;
	private final float attackSpeed;
	private final List<Object[]> entries;

	public ItemAttributeModifiers(float attackDamage, float attackSpeed) {
		this(attackDamage, attackSpeed, new ArrayList<>());
	}

	private ItemAttributeModifiers(float attackDamage, float attackSpeed, List<Object[]> entries) {
		this.attackDamage = attackDamage;
		this.attackSpeed = attackSpeed;
		this.entries = entries;
	}

	public float attackDamage() {
		return this.attackDamage;
	}

	public float attackSpeed() {
		return this.attackSpeed;
	}

	/** The added modifiers: {attribute holder, modifier, slot group} each. */
	public List<Object[]> entries() {
		return this.entries;
	}

	/** The modifiers and one more (1.21 builder style): kept with the others. */
	public ItemAttributeModifiers withModifierAdded(Object attribute, Object modifier, Object slot) {
		var entries = new ArrayList<>(this.entries);
		entries.add(new Object[] {attribute, modifier, slot});
		return new ItemAttributeModifiers(this.attackDamage, this.attackSpeed, entries);
	}

	public List<Object[]> modifiers() {
		return this.entries;
	}

	public ItemAttributeModifiers withTooltip(boolean showInTooltip) {
		return this;
	}

	public boolean showInTooltip() {
		return true;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final List<Object[]> entries = new ArrayList<>();

		public Builder add(Object attribute, Object modifier, Object slot) {
			this.entries.add(new Object[] {attribute, modifier, slot});
			return this;
		}

		public ItemAttributeModifiers build() {
			return new ItemAttributeModifiers(0.0F, 0.0F, this.entries);
		}
	}
}
