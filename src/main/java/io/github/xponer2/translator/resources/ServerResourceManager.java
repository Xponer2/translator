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
package io.github.xponer2.translator.resources;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.util.ResourceLocation;

/**
 * The server's data (1.21 {@code ResourceManager} of the server resources): the {@code data/} files of the 1.21.1 mods'
 * jars and of the player's Minecraft 1.21.1 (vanilla's, below the mods'), what a mod's server reload listener reads
 * (Iron's Spells' spell data, Curios' slots, the mods' own recipe-like files). Indexed once; files are read when opened.
 */
public final class ServerResourceManager implements ResourceManager {
	private static ServerResourceManager instance;

	/** By id, the files of each pack that has it, the last pack on top (1.21's stack order). */
	private final Map<ResourceLocation, List<Resource>> stacks = new LinkedHashMap<>();
	private final Set<String> namespaces = new TreeSet<>();
	private final Map<File, ZipFile> open = new LinkedHashMap<>();

	private ServerResourceManager() {
		List<File> packs = new ArrayList<>();
		File vanilla = io.github.xponer2.translator.runtime.Minecraft1211.clientJar();
		if (vanilla != null && vanilla.isFile()) {
			packs.add(vanilla);
		}
		packs.addAll(io.github.xponer2.translator.runtime.RuntimeMods.jars());
		for (File jar : packs) {
			ZipFile zip;
			try {
				zip = new ZipFile(jar);
			} catch (IOException e) {
				continue;
			}
			this.open.put(jar, zip);
			String pack = jar.getName();
			for (ZipEntry entry : Collections.list(zip.entries())) {
				String name = entry.getName();
				if (entry.isDirectory() || !name.startsWith("data/")) {
					continue;
				}
				int slash = name.indexOf('/', "data/".length());
				if (slash < 0) {
					continue;
				}
				String namespace = name.substring("data/".length(), slash);
				ResourceLocation id;
				try {
					id = new ResourceLocation(namespace, name.substring(slash + 1));
				} catch (RuntimeException e) {
					continue;
				}
				this.namespaces.add(namespace);
				this.stacks.computeIfAbsent(id, k -> new ArrayList<>()).add(new Resource(pack, () -> zip.getInputStream(entry)));
			}
		}
	}

	/** The server's data, indexed when first asked for. */
	public static synchronized ServerResourceManager get() {
		if (instance == null) {
			instance = new ServerResourceManager();
		}
		return instance;
	}

	@Override
	public Optional<Resource> getResource(ResourceLocation id) {
		List<Resource> stack = this.stacks.get(id);
		return stack == null || stack.isEmpty() ? Optional.empty() : Optional.of(stack.get(stack.size() - 1));
	}

	@Override
	public List<Resource> getResourceStack(ResourceLocation id) {
		List<Resource> stack = this.stacks.get(id);
		return stack == null ? Collections.emptyList() : Collections.unmodifiableList(stack);
	}

	@Override
	public Map<ResourceLocation, Resource> listResources(String folder, Predicate<ResourceLocation> filter) {
		String prefix = folder.endsWith("/") ? folder : folder + "/";
		Map<ResourceLocation, Resource> found = new TreeMap<>(java.util.Comparator.comparing(ResourceLocation::toString));
		for (Map.Entry<ResourceLocation, List<Resource>> entry : this.stacks.entrySet()) {
			ResourceLocation id = entry.getKey();
			if (id.getPath().startsWith(prefix) && filter.test(id) && !entry.getValue().isEmpty()) {
				found.put(id, entry.getValue().get(entry.getValue().size() - 1));
			}
		}
		return found;
	}

	@Override
	public Set<String> getNamespaces() {
		return Collections.unmodifiableSet(this.namespaces);
	}
}
