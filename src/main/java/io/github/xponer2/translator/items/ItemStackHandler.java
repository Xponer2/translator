package io.github.xponer2.translator.items;

import io.github.xponer2.translator.registry.HolderLookup;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;

/**
 * NeoForge {@code ItemStackHandler}: the 1.12.2 Forge one with the 1.21 NBT signatures. 1.12.2 saves the count of a
 * stack as a byte, NeoForge as an int: a stack above 127 items keeps its real count in an extra tag.
 */
public class ItemStackHandler extends net.minecraftforge.items.ItemStackHandler {
	private static final String INT_COUNT = "IntCount";

	public ItemStackHandler() {
		super();
	}

	public ItemStackHandler(int size) {
		super(size);
	}

	public ItemStackHandler(NonNullList<ItemStack> stacks) {
		super(stacks);
	}

	@Override
	public NBTTagCompound serializeNBT() {
		var items = new NBTTagList();
		for (int i = 0; i < this.stacks.size(); ++i) {
			var stack = this.stacks.get(i);
			if (!stack.isEmpty()) {
				var item = new NBTTagCompound();
				item.setInteger("Slot", i);
				stack.writeToNBT(item);
				if (stack.getCount() > Byte.MAX_VALUE) {
					item.setInteger(INT_COUNT, stack.getCount());
				}
				items.appendTag(item);
			}
		}
		var nbt = new NBTTagCompound();
		nbt.setTag("Items", items);
		nbt.setInteger("Size", this.stacks.size());
		return nbt;
	}

	// a saved size never shrinks the handler (the slots of the current version stay)
	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		var size = nbt.hasKey("Size", 3) ? nbt.getInteger("Size") : this.stacks.size();
		this.setSize(Math.max(size, this.stacks.size()));
		var items = nbt.getTagList("Items", 10);
		for (int i = 0; i < items.tagCount(); ++i) {
			var item = items.getCompoundTagAt(i);
			int slot = item.getInteger("Slot");
			if (slot >= 0 && slot < this.stacks.size()) {
				var stack = new ItemStack(item);
				if (item.hasKey(INT_COUNT, 3)) {
					stack.setCount(item.getInteger(INT_COUNT));
				}
				this.stacks.set(slot, stack);
			}
		}
		this.onLoad();
	}

	// 1.21 signatures

	public NBTTagCompound serializeNBT(HolderLookup.Provider lookup) {
		return this.serializeNBT();
	}

	public void deserializeNBT(HolderLookup.Provider lookup, NBTTagCompound nbt) {
		this.deserializeNBT(nbt);
	}
}
