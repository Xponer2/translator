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
package io.github.xponer2.translator.energy;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagInt;

/**
 * NeoForge {@code EnergyStorage}: the 1.12.2 Forge energy storage, saved as an int tag like on NeoForge (1.12.2
 * Forge's doesn't save itself).
 */
public class EnergyStorage extends net.minecraftforge.energy.EnergyStorage {
	public EnergyStorage(int capacity) {
		super(capacity);
	}

	public EnergyStorage(int capacity, int maxTransfer) {
		super(capacity, maxTransfer);
	}

	public EnergyStorage(int capacity, int maxReceive, int maxExtract) {
		super(capacity, maxReceive, maxExtract);
	}

	public EnergyStorage(int capacity, int maxReceive, int maxExtract, int energy) {
		super(capacity, maxReceive, maxExtract, energy);
	}

	public NBTBase serializeNBT() {
		return new NBTTagInt(this.getEnergyStored());
	}

	public void deserializeNBT(NBTBase tag) {
		if (tag instanceof NBTTagInt value) {
			this.energy = value.getInt();
		}
	}
}
