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

import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;

/**
 * 1.21 {@code StatType}: a kind of statistic per block or item ({@code Stats.BLOCK_MINED.get(block)}). The constants
 * are the 1.21 {@code Stats} fields 1.12.2's {@link StatList} lacks; {@link #get} gives the 1.12.2 statistic, null
 * where 1.12.2 has none (a player ignores a null statistic).
 */
public final class StatType<T> {
	public static final StatType<Block> BLOCK_MINED = new StatType<>(StatList::getBlockStats);
	public static final StatType<Item> ITEM_CRAFTED = new StatType<>(StatList::getCraftStats);
	public static final StatType<Item> ITEM_USED = new StatType<>(StatList::getObjectUseStats);
	public static final StatType<Item> ITEM_BROKEN = new StatType<>(StatList::getObjectBreakStats);
	public static final StatType<Item> ITEM_PICKED_UP = new StatType<>(StatList::getObjectsPickedUpStats);
	public static final StatType<Item> ITEM_DROPPED = new StatType<>(StatList::getDroppedObjectStats);
	public static final StatType<Object> ENTITY_KILLED = new StatType<>(value -> null);
	public static final StatType<Object> ENTITY_KILLED_BY = new StatType<>(value -> null);
	public static final StatType<Object> CUSTOM = new StatType<>(value -> null);

	private final Function<T, StatBase> stats;

	private StatType(Function<T, StatBase> stats) {
		this.stats = stats;
	}

	@Nullable
	public StatBase get(T value) {
		return this.stats.apply(value);
	}
}
