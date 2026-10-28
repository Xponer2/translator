package io.github.xponer2.translator.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.xponer2.translator.Translator;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;

/**
 * Block states as 1.12.2 metadata (4 bits): 1.21 saves every property of a block state, 1.12.2 only its metadata. The
 * properties are packed one after the other (directions first) as long as they fit into 16 values; the ones that
 * don't fit come back as their default value when the chunk is loaded (logged once per block).
 */
public final class BlockStates {
	private static final Map<Block, List<IProperty<?>>> SAVED = new ConcurrentHashMap<>();

	private BlockStates() { }

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static int toMeta(IBlockState state) {
		int meta = 0;
		int radix = 1;
		for (var property : saved(state.getBlock())) {
			var values = new ArrayList<>(property.getAllowedValues());
			meta += values.indexOf(state.getValue((IProperty) property)) * radix;
			radix *= values.size();
		}
		return meta;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static IBlockState fromMeta(Block block, int meta) {
		var state = block.getDefaultState();
		for (var property : saved(block)) {
			var values = new ArrayList<>(property.getAllowedValues());
			state = state.withProperty((IProperty) property, (Comparable) values.get(meta % values.size()));
			meta /= values.size();
		}
		return state;
	}

	private static List<IProperty<?>> saved(Block block) {
		return SAVED.computeIfAbsent(block, BlockStates::choose);
	}

	private static List<IProperty<?>> choose(Block block) {
		var properties = new ArrayList<IProperty<?>>(block.getBlockState().getProperties());
		properties.sort((a, b) -> Boolean.compare(!(a instanceof PropertyDirection), !(b instanceof PropertyDirection)));

		var saved = new ArrayList<IProperty<?>>();
		int values = 1;
		for (var property : properties) {
			int size = property.getAllowedValues().size();
			if (values * size <= 16) {
				saved.add(property);
				values *= size;
			} else {
				Translator.LOGGER.warn("Block {}: the property {} doesn't fit into the metadata, it isn't saved", block.getRegistryName(), property.getName());
			}
		}
		return saved;
	}
}
