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
