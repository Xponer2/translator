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

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import io.github.xponer2.translator.client.resources.Pack;
import io.github.xponer2.translator.resources.Resource;
import io.github.xponer2.translator.resources.ResourceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Static stand-ins for the 1.21 {@code Pack} of the client's pack list (neoforge-1.21.1-redirects.tsv): its resources.
 * Client only.
 */
@SideOnly(Side.CLIENT)
public final class PackBridges {
	private PackBridges() {
	}

	/**
	 * 1.21 {@code pack.open()}: the pack's resources as a 1.21 PackResources (carried), read through the game's
	 * resources and kept to the ones from this pack: "vanilla" the game's own, "file/name" that resource pack,
	 * "mod_resources" the rest. JourneyMap reads its icon sets from the selected packs so, Moonlight builds its filtered
	 * resource managers (its generated textures) from them, Chat Heads checks a pack's file.
	 */
	@Nullable
	public static net.minecraft.server.packs.PackResources open(Pack pack) {
		ClassLoader loader = PackBridges.class.getClassLoader();
		try {
			Class<?> resources = Class.forName("net.minecraft.server.packs.PackResources", true, loader);
			Class<?> supplier = Class.forName("net.minecraft.server.packs.resources.IoSupplier", true, loader);
			String id = pack.getId();
			Object location = location(pack, loader);
			return (net.minecraft.server.packs.PackResources) Proxy.newProxyInstance(loader, new Class<?>[] {resources}, (proxy, method, args) -> switch (method.getName()) {
				case "location" -> location;
				case "getResource" -> args.length == 2 && clientType(args[0]) ? resource(supplier, id, (ResourceLocation) args[1]) : null;
				case "listResources" -> {
					if (args.length == 4 && clientType(args[0])) {
						list(supplier, id, (String) args[1], (String) args[2], args[3]);
					}
					yield null;
				}
				case "getNamespaces" -> args != null && args.length == 1 && clientType(args[0]) ? namespaces(id) : java.util.Collections.emptySet();
				case "packId" -> id;
				case "knownPackInfo" -> Optional.empty();
				case "isHidden" -> false;
				case "toString" -> "Pack " + id;
				case "hashCode" -> System.identityHashCode(proxy);
				case "equals" -> proxy == args[0];
				default -> defaultValue(method);
			});
		} catch (ClassNotFoundException | RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("Pack.open", e);
			return null;
		}
	}

	/**
	 * 1.21 {@code pack.location()} (carried PackLocationInfo): its id, title and source, built in for the game's and the
	 * mods' resources (Moonlight sorts packs by it).
	 */
	@Nullable
	public static net.minecraft.server.packs.PackLocationInfo location(Pack pack) {
		return (net.minecraft.server.packs.PackLocationInfo) location(pack, PackBridges.class.getClassLoader());
	}

	@Nullable
	private static Object location(Pack pack, ClassLoader loader) {
		try {
			Class<?> source = Class.forName("net.minecraft.server.packs.repository.PackSource", true, loader);
			Object kind = source.getField(pack.getId().startsWith("file/") ? "DEFAULT" : "BUILT_IN").get(null);
			Class<?> info = Class.forName("net.minecraft.server.packs.PackLocationInfo", true, loader);
			return info.getConstructor(String.class, net.minecraft.util.text.ITextComponent.class, source, Optional.class)
					.newInstance(pack.getId(), pack.getTitle(), kind, Optional.empty());
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("Pack.location", e);
			return null;
		}
	}

	/** Client resources (PackType.CLIENT_RESOURCES); the client has no server data here. */
	private static boolean clientType(Object type) {
		return type instanceof Enum<?> value && "CLIENT_RESOURCES".equals(value.name());
	}

	@Nullable
	private static Object resource(Class<?> supplier, String packId, ResourceLocation location) {
		for (Resource resource : ResourceManager.client().getResourceStack(location)) {
			if (ofPack(packId, resource.sourcePackId())) {
				return opener(supplier, resource);
			}
		}
		return null;
	}

	private static void list(Class<?> supplier, String packId, String namespace, String path, Object output) {
		@SuppressWarnings("unchecked")
		BiConsumer<Object, Object> accept = (BiConsumer<Object, Object>) output;
		var stacks = ResourceManager.client().listResourceStacks(path, location -> location.getNamespace().equals(namespace));
		for (var entry : stacks.entrySet()) {
			for (Resource resource : entry.getValue()) {
				if (ofPack(packId, resource.sourcePackId())) {
					accept.accept(entry.getKey(), opener(supplier, resource));
					break;
				}
			}
		}
	}

	private static Set<String> namespaces(String packId) {
		var namespaces = new LinkedHashSet<String>();
		for (String namespace : ResourceManager.client().getNamespaces()) {
			boolean vanilla = "minecraft".equals(namespace) || "realms".equals(namespace);
			if (!"vanilla".equals(packId) || vanilla) {
				namespaces.add(namespace);
			}
		}
		return namespaces;
	}

	/** Whether a resource of the 1.12.2 pack of that name is in the 1.21 pack of that id. */
	private static boolean ofPack(String packId, @Nullable String source) {
		String defaultName = defaultPackName();
		if ("vanilla".equals(packId)) {
			return source != null && source.equals(defaultName);
		}
		if (packId.startsWith("file/")) {
			return packId.substring("file/".length()).equals(source);
		}
		// mod_resources: neither the game's own nor a selected resource pack
		return source == null || !(source.equals(defaultName) || selectedFilePacks().contains(source));
	}

	private static String defaultPackName() {
		var repository = Minecraft.getMinecraft().getResourcePackRepository();
		return repository == null ? "Default" : repository.rprDefaultResourcePack.getPackName();
	}

	private static Set<String> selectedFilePacks() {
		var names = new LinkedHashSet<String>();
		var repository = Minecraft.getMinecraft().getResourcePackRepository();
		if (repository != null) {
			List<net.minecraft.client.resources.ResourcePackRepository.Entry> entries = repository.getRepositoryEntries();
			for (var entry : entries) {
				names.add(entry.getResourcePackName());
			}
		}
		return names;
	}

	/** A 1.21 IoSupplier of the resource's stream. */
	private static Object opener(Class<?> supplier, Resource resource) {
		return Proxy.newProxyInstance(supplier.getClassLoader(), new Class<?>[] {supplier}, (proxy, method, args) -> switch (method.getName()) {
			case "get" -> (InputStream) resource.open();
			case "toString" -> "IoSupplier " + resource;
			case "hashCode" -> System.identityHashCode(proxy);
			case "equals" -> proxy == args[0];
			default -> defaultValue(method);
		});
	}

	@Nullable
	private static Object defaultValue(Method method) {
		Class<?> type = method.getReturnType();
		if (type == boolean.class) {
			return false;
		}
		if (type == int.class || type == long.class || type == short.class || type == byte.class || type == char.class || type == float.class
				|| type == double.class) {
			return type == long.class ? 0L : type == float.class ? 0.0F : type == double.class ? 0.0D : type == char.class ? (Object) (char) 0 : 0;
		}
		if (type == Optional.class) {
			return Optional.empty();
		}
		return null;
	}
}
