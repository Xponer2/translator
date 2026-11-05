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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Biomes;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * The 1.21.1 mods' Nether and End biomes (their is_nether and is_end tags: Biomes O' Plenty's crystalline chasm,
 * visceral heap, ...) in the vanilla Nether and End. 1.12.2 has one biome in each (Hell, the End), so those biomes never
 * generated. Here the dimension's biome provider becomes patches of the vanilla biome and the mods' ones (the vanilla
 * one keeps about 40 %); the End's main island stays the End. The 1.21 noise parameters aren't followed.
 */
public final class VanillaDimensionBiomes {
	/** The End's main island in 1.21: within 64 chunks of the centre. */
	private static final int END_ISLAND = 1024;

	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {
		World world = event.getWorld();
		if (world.isRemote) {
			// the client takes the biomes of each chunk from the server
			return;
		}
		WorldProvider provider = world.provider;
		boolean nether = provider instanceof WorldProviderHell;
		if (!nether && !(provider instanceof WorldProviderEnd)) {
			return;
		}
		List<Biome> mods = nether ? DataBiomes.nether() : DataBiomes.end();
		if (mods.isEmpty()) {
			return;
		}
		Biome vanilla = nether ? Biomes.HELL : Biomes.SKY;
		var biomes = new ArrayList<Biome>();
		for (int i = Math.max(1, (mods.size() * 2 + 2) / 3); i > 0; i--) {
			biomes.add(vanilla);
		}
		biomes.addAll(mods);
		try {
			ReflectionHelper.setPrivateValue(WorldProvider.class, provider,
					new DataBiomeProvider(world.getSeed(), biomes, nether ? null : vanilla, nether ? 0 : END_ISLAND), "biomeProvider", "field_76578_c");
		} catch (RuntimeException e) {
			org.apache.logging.log4j.LogManager.getLogger("Translator Runtime").warn("Couldn't place the mods' biomes in {}: {}",
					provider.getDimensionType().getName(), e.toString());
		}
	}
}
