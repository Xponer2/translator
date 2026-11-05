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
package io.github.xponer2.translator.block;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.BlockEntityType;
import io.github.xponer2.translator.registry.HolderLookup;
import io.github.xponer2.translator.registry.ResourceKey;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;

/**
 * 1.21 {@code RandomizableContainerBlockEntity} (neoforge-1.21.1-runtime.tsv): a container that may hold a loot table
 * until it is first opened (Twilight Forest's chests); the loot is rolled with the 1.12.2 loot table of that name.
 */
public abstract class RandomizableContainerBlockEntity extends BaseContainerBlockEntity {
	@Nullable
	protected ResourceLocation lootTable;
	protected long lootTableSeed;

	protected RandomizableContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, IBlockState state) {
		super(type, pos, state);
	}

	/** 1.21 {@code setLootTable(ResourceKey<LootTable>, long)}. */
	public void setLootTable(@Nullable ResourceKey<?> table, long seed) {
		this.lootTable = table == null ? null : table.location();
		this.lootTableSeed = seed;
	}

	public void setLootTable(@Nullable ResourceKey<?> table) {
		this.setLootTable(table, 0L);
	}

	protected boolean tryLoadLootTable(NBTTagCompound tag) {
		if (tag.hasKey("LootTable", 8)) {
			this.lootTable = new ResourceLocation(tag.getString("LootTable"));
			this.lootTableSeed = tag.getLong("LootTableSeed");
			return true;
		}
		return false;
	}

	protected boolean trySaveLootTable(NBTTagCompound tag) {
		if (this.lootTable == null) {
			return false;
		}
		tag.setString("LootTable", this.lootTable.toString());
		if (this.lootTableSeed != 0L) {
			tag.setLong("LootTableSeed", this.lootTableSeed);
		}
		return true;
	}

	/** The loot rolled into the slots, once (a player opening it, a hopper taking from it). */
	public void unpackLootTable(@Nullable EntityPlayer player) {
		if (this.lootTable == null || !(this.world instanceof WorldServer server)) {
			return;
		}
		LootTable table = io.github.xponer2.translator.loot.JsonLootTable.of(this.lootTable,
				server.getLootTableManager().getLootTableFromLocation(this.lootTable));
		this.lootTable = null;
		java.util.Random random = this.lootTableSeed == 0L ? new java.util.Random() : new java.util.Random(this.lootTableSeed);
		LootContext.Builder context = new LootContext.Builder(server);
		if (player != null) {
			context.withLuck(player.getLuck()).withPlayer(player);
		}
		table.fillInventory(this, random, context.build());
		this.setChanged();
	}

	@Override
	public boolean isEmpty() {
		this.unpackLootTable(null);
		return super.isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		this.unpackLootTable(null);
		return super.getItem(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		this.unpackLootTable(null);
		return super.removeItem(slot, count);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		this.unpackLootTable(null);
		return super.removeItemNoUpdate(slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		this.unpackLootTable(null);
		super.setItem(slot, stack);
	}

	@Override
	@Nullable
	public net.minecraft.inventory.Container createMenu(int windowId, net.minecraft.entity.player.InventoryPlayer playerInventory, EntityPlayer player) {
		if (this.canOpen(player)) {
			this.unpackLootTable(player);
		}
		return super.createMenu(windowId, playerInventory, player);
	}

	@Override
	protected void loadAdditional(NBTTagCompound tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.tryLoadLootTable(tag);
	}

	@Override
	protected void saveAdditional(NBTTagCompound tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		this.trySaveLootTable(tag);
	}
}
