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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * The states of a 1.21.1 mod's block, each finding the state with one property changed by a lookup in the block's
 * states, as 1.21 does. 1.12.2 gives every state a table of every property and value: for a block with thousands of
 * states that is millions of entries (Handcrafted's book stack, 4 x 4 x 256 states, ran the game out of memory while
 * registering).
 */
public class LazyStateContainer extends BlockStateContainer {
	public LazyStateContainer(Block block, IProperty<?>... properties) {
		super(block, properties, ImmutableMap.of());
	}

	@Override
	protected StateImplementation createState(Block block, ImmutableMap<IProperty<?>, Comparable<?>> properties,
			@Nullable ImmutableMap<IUnlistedProperty<?>, Optional<?>> unlistedProperties) {
		return new State(block, properties);
	}

	/** {@link BlockStateContainer.Builder} that makes a lazy container (an extended one, with unlisted properties, as before). */
	public static class Builder extends BlockStateContainer.Builder {
		private final Block block;
		private final List<IProperty<?>> listed = new ArrayList<>();
		private boolean unlisted;

		public Builder(Block block) {
			super(block);
			this.block = block;
		}

		@Override
		public BlockStateContainer.Builder add(IProperty<?>... props) {
			var present = new ArrayList<IProperty<?>>();
			for (IProperty<?> prop : props) {
				// a property a mod's code couldn't make (null) is left out, and one of a name the block has already
				if (prop != null && this.listed.stream().noneMatch(other -> other.getName().equals(prop.getName()))) {
					this.listed.add(prop);
					present.add(prop);
				}
			}
			return super.add(present.toArray(new IProperty<?>[0]));
		}

		@Override
		public BlockStateContainer.Builder add(IUnlistedProperty<?>... props) {
			this.unlisted |= props.length > 0;
			return super.add(props);
		}

		@Override
		public BlockStateContainer build() {
			return this.unlisted ? super.build() : new LazyStateContainer(this.block, this.listed.toArray(new IProperty<?>[0]));
		}
	}

	static final class State extends StateImplementation {
		private Map<Map<IProperty<?>, Comparable<?>>, StateImplementation> states;

		State(Block block, ImmutableMap<IProperty<?>, Comparable<?>> properties) {
			super(block, properties);
		}

		@Override
		public void buildPropertyValueTable(Map<Map<IProperty<?>, Comparable<?>>, StateImplementation> map) {
			this.states = map;
		}

		@Override
		public <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property, V value) {
			Comparable<?> current = this.getProperties().get(property);
			if (current == null) {
				throw new IllegalArgumentException("Cannot set property " + property + " as it does not exist in " + this.getBlock().getBlockState());
			}
			if (current.equals(value)) {
				return this;
			}
			var key = new HashMap<IProperty<?>, Comparable<?>>(this.getProperties());
			key.put(property, value);
			IBlockState state = this.states.get(key);
			if (state == null) {
				throw new IllegalArgumentException("Cannot set property " + property + " to " + value + " on block "
						+ Block.REGISTRY.getNameForObject(this.getBlock()) + ", it is not an allowed value");
			}
			return state;
		}
	}
}
