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
package io.github.xponer2.translator.world;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/**
 * A block state saved by 1.21 ({@code {Name, Properties}}: structure templates, falling blocks, a mod's own data) as a
 * 1.12.2 state. 1.12.2's NBTUtil knows only its own names: a 1.21 name of a block 1.12.2 has as a variant
 * ({@code minecraft:stone_bricks} is {@code stonebrick} variant 0) or under another name read as air, so the mods'
 * structure templates came out as holes. Names go through the flattening table ({@code data/translator/flattening.json})
 * and the renames below; the properties the 1.12.2 block has by the same name and value are set.
 */
public final class BlockStateNbt {
	/** 1.21 names 1.12.2 has as one block under another name (not variants: those are in the flattening table). */
	private static final Map<String, String> RENAMED = new HashMap<>();
	@Nullable
	private static Map<String, Object[]> flattening;

	static {
		RENAMED.put("minecraft:cave_air", "minecraft:air");
		RENAMED.put("minecraft:void_air", "minecraft:air");
		RENAMED.put("minecraft:dirt_path", "minecraft:grass_path");
		RENAMED.put("minecraft:wall_torch", "minecraft:torch");
		RENAMED.put("minecraft:redstone_wall_torch", "minecraft:redstone_torch");
		RENAMED.put("minecraft:oak_fence", "minecraft:fence");
		RENAMED.put("minecraft:oak_fence_gate", "minecraft:fence_gate");
		RENAMED.put("minecraft:oak_door", "minecraft:wooden_door");
		RENAMED.put("minecraft:oak_trapdoor", "minecraft:trapdoor");
		RENAMED.put("minecraft:oak_pressure_plate", "minecraft:wooden_pressure_plate");
		RENAMED.put("minecraft:oak_button", "minecraft:wooden_button");
		RENAMED.put("minecraft:oak_sign", "minecraft:standing_sign");
		RENAMED.put("minecraft:oak_wall_sign", "minecraft:wall_sign");
		RENAMED.put("minecraft:nether_bricks", "minecraft:nether_brick");
		RENAMED.put("minecraft:snow_block", "minecraft:snow");
		RENAMED.put("minecraft:snow", "minecraft:snow_layer");
		RENAMED.put("minecraft:lily_pad", "minecraft:waterlily");
		RENAMED.put("minecraft:sugar_cane", "minecraft:reeds");
		RENAMED.put("minecraft:cobweb", "minecraft:web");
		RENAMED.put("minecraft:spawner", "minecraft:mob_spawner");
		RENAMED.put("minecraft:bricks", "minecraft:brick_block");
		RENAMED.put("minecraft:magma_block", "minecraft:magma");
		RENAMED.put("minecraft:terracotta", "minecraft:hardened_clay");
		RENAMED.put("minecraft:jack_o_lantern", "minecraft:lit_pumpkin");
		RENAMED.put("minecraft:carved_pumpkin", "minecraft:pumpkin");
		RENAMED.put("minecraft:nether_quartz_ore", "minecraft:quartz_ore");
		RENAMED.put("minecraft:end_stone_bricks", "minecraft:end_bricks");
		RENAMED.put("minecraft:moving_piston", "minecraft:piston_extension");
		RENAMED.put("minecraft:melon", "minecraft:melon_block");
		RENAMED.put("minecraft:powered_rail", "minecraft:golden_rail");
		RENAMED.put("minecraft:cobblestone_stairs", "minecraft:stone_stairs");
		RENAMED.put("minecraft:smooth_quartz", "minecraft:quartz_block");
		RENAMED.put("minecraft:repeater", "minecraft:unpowered_repeater");
		RENAMED.put("minecraft:comparator", "minecraft:unpowered_comparator");
		RENAMED.put("minecraft:note_block", "minecraft:noteblock");
		RENAMED.put("minecraft:slime_block", "minecraft:slime");
		RENAMED.put("minecraft:red_nether_bricks", "minecraft:red_nether_brick");
		RENAMED.put("minecraft:shulker_box", "minecraft:purple_shulker_box");
	}

	private BlockStateNbt() {
	}

	/** 1.21 {@code NbtUtils.readBlockState}: the 1.12.2 state of a saved 1.21 one (air when the block isn't there). */
	public static IBlockState read(NBTTagCompound tag) {
		if (!tag.hasKey("Name", 8)) {
			return Blocks.AIR.getDefaultState();
		}
		String name = tag.getString("Name");
		if (name.indexOf(':') < 0) {
			name = "minecraft:" + name;
		}
		NBTTagCompound properties = tag.getCompoundTag("Properties");
		IBlockState state = state(name, properties);
		if (state == null) {
			return Blocks.AIR.getDefaultState();
		}
		for (String key : properties.getKeySet()) {
			state = with(state, key, properties.getString(key));
		}
		if ("minecraft:torch".equals(name) || "minecraft:redstone_torch".equals(name)) {
			// a 1.21 floor torch has no facing (1.12.2's default may be another side)
			state = with(state, "facing", "up");
		}
		return state;
	}

	@Nullable
	@SuppressWarnings("deprecation")
	private static IBlockState state(String name, NBTTagCompound properties) {
		Object[] flat = flattening().get(name);
		if (flat != null) {
			Block block = Block.getBlockFromName((String) flat[0]);
			if (block != null) {
				IBlockState state = block.getStateFromMeta((Integer) flat[1]);
				if (block == Blocks.STONE_SLAB || block == Blocks.WOODEN_SLAB || block == Blocks.STONE_SLAB2 || block == Blocks.PURPUR_SLAB) {
					// 1.21 slabs say type=top|bottom|double; 1.12.2's half, and a double slab is another block
					String type = properties.getString("type");
					if ("top".equals(type)) {
						state = with(state, "half", "top");
					}
				}
				return state;
			}
		}
		String renamed = RENAMED.getOrDefault(name, name);
		Block block;
		try {
			block = Block.REGISTRY.containsKey(new ResourceLocation(renamed)) ? Block.REGISTRY.getObject(new ResourceLocation(renamed)) : null;
		} catch (RuntimeException e) {
			block = null;
		}
		return block == null ? null : block.getDefaultState();
	}

	/** The state with the property of that name set to that value, if the block has both; else the state as it is. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static IBlockState with(IBlockState state, String key, String value) {
		IProperty property = state.getBlock().getBlockState().getProperty(key);
		if (property == null) {
			return state;
		}
		com.google.common.base.Optional parsed = property.parseValue(value);
		return parsed.isPresent() ? state.withProperty(property, (Comparable) parsed.get()) : state;
	}

	private static synchronized Map<String, Object[]> flattening() {
		if (flattening != null) {
			return flattening;
		}
		Map<String, Object[]> map = new HashMap<>();
		var stream = BlockStateNbt.class.getResourceAsStream("/data/translator/flattening.json");
		if (stream != null) {
			try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				@SuppressWarnings("deprecation")
				JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
				for (var entry : json.entrySet()) {
					JsonObject target = entry.getValue().getAsJsonObject();
					if (target.has("similar") && target.get("similar").getAsBoolean()) {
						continue;
					}
					map.put(entry.getKey(), new Object[] {target.get("id").getAsString(), target.get("meta").getAsInt()});
				}
			} catch (java.io.IOException | RuntimeException e) {
				org.apache.logging.log4j.LogManager.getLogger("Translator").warn("Couldn't read the flattening table: {}", e.toString());
			}
		}
		flattening = map;
		return map;
	}

	/**
	 * 1.21 {@code BlockStateParser.parseForBlock(blocks, text, allowNbt)} (an X line): {@code ns:block[key=value,...]{nbt}}
	 * by its 1.21 names. The carried parser looks blocks up in a 1.21 registry the Translator doesn't fill: a jigsaw's
	 * {@code final_state} didn't parse and the mods' template pools left their jigsaw blocks in place.
	 */
	public static net.minecraft.commands.arguments.blocks.BlockStateParser.BlockResult parseForBlock(io.github.xponer2.translator.registry.HolderLookup blocks,
			String text, boolean allowNbt) {
		String rest = text.trim();
		NBTTagCompound nbt = null;
		int brace = rest.indexOf('{');
		if (brace >= 0) {
			if (allowNbt) {
				try {
					nbt = net.minecraft.nbt.JsonToNBT.getTagFromJson(rest.substring(brace));
				} catch (net.minecraft.nbt.NBTException e) {
					nbt = null;
				}
			}
			rest = rest.substring(0, brace);
		}
		NBTTagCompound tag = new NBTTagCompound();
		NBTTagCompound properties = new NBTTagCompound();
		int bracket = rest.indexOf('[');
		if (bracket >= 0) {
			int end = rest.indexOf(']', bracket);
			for (String pair : rest.substring(bracket + 1, end < 0 ? rest.length() : end).split(",")) {
				int eq = pair.indexOf('=');
				if (eq > 0) {
					properties.setString(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
				}
			}
			rest = rest.substring(0, bracket);
		}
		tag.setString("Name", rest.trim());
		tag.setTag("Properties", properties);
		IBlockState state = read(tag);
		try {
			Class<?> type = Class.forName("net.minecraft.commands.arguments.blocks.BlockStateParser$BlockResult", true, net.minecraft.launchwrapper.Launch.classLoader);
			var constructor = type.getDeclaredConstructors()[0];
			constructor.setAccessible(true);
			return (net.minecraft.commands.arguments.blocks.BlockStateParser.BlockResult) constructor.newInstance(state, new java.util.HashMap<>(), nbt);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("No carried BlockStateParser.BlockResult", e);
		}
	}
}
