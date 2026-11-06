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
package io.github.xponer2.translator.client.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code PackRepository} (what {@code minecraft.getResourcePackRepository()} gives): the client's resource packs, read
 * from 1.12.2's list. The game's own resources first ("vanilla"), then the selected packs in their order. JourneyMap
 * reads the selected packs' names to start mapping; with none it never mapped and its map stayed black.
 */
@SideOnly(Side.CLIENT)
public final class PackRepository {
	private static final PackRepository INSTANCE = new PackRepository();

	private PackRepository() {
	}

	/** {@code minecraft.getResourcePackRepository()}: 1.12.2's method of that name gives its own repository. */
	public static PackRepository getResourcePackRepository(Minecraft minecraft) {
		return INSTANCE;
	}

	private static Map<String, Pack> packs(boolean selectedOnly) {
		var packs = new LinkedHashMap<String, Pack>();
		packs.put("vanilla", new Pack("vanilla", "Default", "The default look of Minecraft", true));
		packs.put("mod_resources", new Pack("mod_resources", "Mod Resources", "The resources of the mods", true));
		ResourcePackRepository repository = Minecraft.getMinecraft().getResourcePackRepository();
		if (repository != null) {
			for (ResourcePackRepository.Entry entry : selectedOnly ? repository.getRepositoryEntries() : repository.getRepositoryEntriesAll()) {
				String name = entry.getResourcePackName();
				packs.put("file/" + name, new Pack("file/" + name, name, entry.getTexturePackDescription(), false));
			}
		}
		return packs;
	}

	public Collection<Pack> getSelectedPacks() {
		return new ArrayList<>(packs(true).values());
	}

	public Collection<Pack> getAvailablePacks() {
		return new ArrayList<>(packs(false).values());
	}

	public Collection<String> getSelectedIds() {
		return new ArrayList<>(packs(true).keySet());
	}

	public Collection<String> getAvailableIds() {
		return new ArrayList<>(packs(false).keySet());
	}

	@Nullable
	public Pack getPack(String id) {
		return packs(false).get(id);
	}

	public boolean isAvailable(String id) {
		return packs(false).containsKey(id);
	}

	/** Packs are chosen in the game's own screen on 1.12.2: nothing to reload or select from here. */
	public void reload() {
	}
}
