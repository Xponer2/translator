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
package io.github.xponer2.translator.runtime.redirect;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * The static fields 1.21 {@code BlockState} has and 1.12.2's IBlockState doesn't (runtime, a missing GETSTATIC goes
 * here): {@code CODEC}, a state as 1.21 data writes it ({@code {"Name": "minecraft:short_grass", "Properties":
 * {"age": "2"}}}: the block states of the mods' worldgen features, block state providers, ...). The block is found by
 * its 1.21 name, the 1.21 vanilla names 1.12.2 has under another block and meta value too (minecraft:birch_planks is
 * planks of birch); properties the 1.12.2 block hasn't are left out.
 */
public final class BlockStateStatics {
	private static final Codec<Pair<String, Map<String, String>>> DATA = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("Name").forGetter(Pair::getFirst),
			Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("Properties", Collections.emptyMap()).forGetter(Pair::getSecond))
			.apply(instance, Pair::of));

	public static final Codec<IBlockState> CODEC = DATA.comapFlatMap(BlockStateStatics::decode, BlockStateStatics::encode);

	private BlockStateStatics() {
	}

	private static DataResult<IBlockState> decode(Pair<String, Map<String, String>> data) {
		ResourceLocation id;
		try {
			id = new ResourceLocation(data.getFirst());
		} catch (RuntimeException e) {
			return DataResult.error("Not a block name: " + data.getFirst());
		}
		IBlockState state = byName(id);
		if (state == null) {
			return DataResult.error("Unknown block " + id);
		}
		return DataResult.success(withProperties(state, data.getSecond()));
	}

	private static Pair<String, Map<String, String>> encode(IBlockState state) {
		var properties = new LinkedHashMap<String, String>();
		for (Map.Entry<IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet()) {
			properties.put(entry.getKey().getName(), valueName(entry.getKey(), entry.getValue()));
		}
		return Pair.of(String.valueOf(state.getBlock().getRegistryName()), properties);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static String valueName(IProperty property, Comparable value) {
		return property.getName(value);
	}

	/**
	 * 1.21 {@code state.is(holderSet)} (a carried HolderSet: a feature's {@code matching_blocks} predicate, a list of
	 * blocks or a tag): the state's block is one of its members.
	 */
	public static boolean is(IBlockState state, Object holders) {
		if (holders instanceof Iterable<?> members) {
			for (Object member : members) {
				if (member instanceof io.github.xponer2.translator.registry.Holder<?> holder && holder.value() == state.getBlock()) {
					return true;
				}
			}
		}
		return false;
	}

	/** The default state of a 1.21 block name: a mod's block, or the 1.12.2 block and meta value of a vanilla one; null if none. */
	@Nullable
	public static IBlockState byName(ResourceLocation id) {
		if ("minecraft".equals(id.getNamespace())) {
			if ("air".equals(id.getPath()) || "cave_air".equals(id.getPath()) || "void_air".equals(id.getPath())) {
				return Blocks.AIR.getDefaultState();
			}
			// a 1.21 name of a 1.12.2 block with a meta value (birch_planks, granite, short_grass): its item's meta
			ItemStack stack = io.github.xponer2.translator.util.ItemIds.stack(id, 1);
			if (!stack.isEmpty() && stack.getItem() instanceof ItemBlock item && item.getBlock() != Blocks.AIR) {
				try {
					return item.getBlock().getStateFromMeta(item.getMetadata(stack.getMetadata()));
				} catch (RuntimeException e) {
					return item.getBlock().getDefaultState();
				}
			}
			try {
				if (FlattenedBlocks.class.getField(id.getPath().toUpperCase(java.util.Locale.ROOT)).get(null) instanceof Block block && block != Blocks.AIR
						&& !(block instanceof AbsentBlock)) {
					return block.getDefaultState();
				}
			} catch (ReflectiveOperationException | RuntimeException e) {
				// not a 1.21 vanilla block with another 1.12.2 name
			}
		}
		Block block = ForgeRegistries.BLOCKS.containsKey(id) ? ForgeRegistries.BLOCKS.getValue(id) : null;
		return block == null ? null : block.getDefaultState();
	}

	/** The properties (by name and value name) on the state, those its block has. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static IBlockState withProperties(IBlockState state, Map<String, String> properties) {
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			for (IProperty property : state.getPropertyKeys()) {
				if (property.getName().equals(entry.getKey())) {
					var value = property.parseValue(entry.getValue());
					if (value.isPresent()) {
						state = state.withProperty(property, (Comparable) value.get());
					}
				}
			}
		}
		return state;
	}
}
