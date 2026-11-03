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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

import net.minecraft.util.ResourceLocation;

/**
 * The resources of the game (1.21 {@code ResourceManager} and {@code ResourceProvider}, neoforge-1.21.1-runtime.tsv):
 * on the client the 1.12.2 resource manager's assets, listed from the 1.21.1 mod jars.
 */
public interface ResourceManager {
	Optional<Resource> getResource(ResourceLocation id);

	List<Resource> getResourceStack(ResourceLocation id);

	/** The files under a folder (assets/<namespace>/<folder>/...) whose ids pass the filter, by id. */
	Map<ResourceLocation, Resource> listResources(String folder, Predicate<ResourceLocation> filter);

	Set<String> getNamespaces();

	default Map<ResourceLocation, List<Resource>> listResourceStacks(String folder, Predicate<ResourceLocation> filter) {
		var stacks = new TreeMap<ResourceLocation, List<Resource>>(java.util.Comparator.comparing(ResourceLocation::toString));
		for (ResourceLocation id : this.listResources(folder, filter).keySet()) {
			stacks.put(id, this.getResourceStack(id));
		}
		return stacks;
	}

	default Resource getResourceOrThrow(ResourceLocation id) throws FileNotFoundException {
		return this.getResource(id).orElseThrow(() -> new FileNotFoundException(id.toString()));
	}

	default InputStream open(ResourceLocation id) throws IOException {
		return this.getResourceOrThrow(id).open();
	}

	default BufferedReader openAsReader(ResourceLocation id) throws IOException {
		return this.getResourceOrThrow(id).openAsReader();
	}

	default java.util.stream.Stream<Object> listPacks() {
		return java.util.stream.Stream.empty();
	}

	/** The client's resources: the 1.12.2 resource manager's. */
	static ResourceManager client() {
		return Client.INSTANCE;
	}

	final class Client implements ResourceManager {
		static final Client INSTANCE = new Client();

		private Client() {
		}

		private static net.minecraft.client.resources.IResourceManager manager() {
			return net.minecraft.client.Minecraft.getMinecraft().getResourceManager();
		}

		private static Resource wrap(ResourceLocation id, net.minecraft.client.resources.IResource resource) {
			String pack = resource.getResourcePackName();
			try {
				resource.close();
			} catch (IOException e) {
				// reopened when read
			}
			return new Resource(pack, () -> manager().getResource(id).getInputStream());
		}

		@Override
		public Optional<Resource> getResource(ResourceLocation id) {
			try {
				return Optional.of(wrap(id, manager().getResource(id)));
			} catch (IOException | RuntimeException e) {
				return Optional.empty();
			}
		}

		@Override
		public List<Resource> getResourceStack(ResourceLocation id) {
			var stack = new ArrayList<Resource>();
			try {
				for (var resource : manager().getAllResources(id)) {
					String pack = resource.getResourcePackName();
					stack.add(new Resource(pack, resource::getInputStream));
				}
			} catch (IOException | RuntimeException e) {
				// none
			}
			return stack;
		}

		@Override
		public Map<ResourceLocation, Resource> listResources(String folder, Predicate<ResourceLocation> filter) {
			var found = new TreeMap<ResourceLocation, Resource>(java.util.Comparator.comparing(ResourceLocation::toString));
			for (ResourceLocation id : io.github.xponer2.translator.runtime.client.RuntimeResourcePack.list(folder)) {
				if (filter.test(id)) {
					this.getResource(id).ifPresent(resource -> found.put(id, resource));
				}
			}
			return found;
		}

		@Override
		public Set<String> getNamespaces() {
			return manager().getResourceDomains();
		}
	}
}
