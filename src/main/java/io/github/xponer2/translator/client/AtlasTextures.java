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
package io.github.xponer2.translator.client;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.util.Collections;

import io.github.xponer2.translator.Translator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 stitches every texture of textures/block and textures/item into the blocks atlas; 1.12.2 only the ones its
 * models use. Renderers of ported mods look sprites up by id, so the textures of the mods that require the Translator
 * are added to the atlas (found on the class path: mod jars and, in development, resource folders).
 */
@SideOnly(Side.CLIENT)
public final class AtlasTextures {
	@SubscribeEvent
	public void onStitch(TextureStitchEvent.Pre event) {
		for (var mod : Loader.instance().getActiveModList()) {
			if (Translator.isPortedMod(mod.getModId())) {
				for (var folder : new String[] {"block", "item"}) {
					register(event.getMap(), mod.getModId(), folder);
				}
			}
		}
		// the original 1.21.1 mods started at runtime: the same, and the sources their atlases/blocks.json adds
		// (Sophisticated Backpacks' backpack model takes its textures from block/: without them it was missing-texture)
		int added = 0;
		for (File jar : io.github.xponer2.translator.runtime.RuntimeMods.jars()) {
			added += registerJar(event.getMap(), jar);
		}
		if (added > 0) {
			Translator.LOGGER.info("Blocks atlas: {} textures of the 1.21.1 mods added, as 1.21 stitches them", added);
		}
	}

	/** Every textures/block and textures/item png of the jar, and the directories and single textures of its atlas sources. */
	private static int registerJar(TextureMap map, File jar) {
		int added = 0;
		try (var zip = new java.util.zip.ZipFile(jar)) {
			var directories = new java.util.ArrayList<String[]>();
			var entries = Collections.list(zip.entries());
			for (var entry : entries) {
				var name = entry.getName();
				if (name.startsWith("assets/") && name.endsWith("/atlases/blocks.json")) {
					try (var in = zip.getInputStream(entry); var reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
						var json = new com.google.gson.JsonParser().parse(reader).getAsJsonObject();
						for (var source : json.getAsJsonArray("sources")) {
							var object = source.getAsJsonObject();
							String type = object.has("type") ? object.get("type").getAsString().replace("minecraft:", "") : "";
							if ("directory".equals(type) && object.has("source")) {
								directories.add(new String[] {object.get("source").getAsString(),
										object.has("prefix") ? object.get("prefix").getAsString() : object.get("source").getAsString() + "/"});
							} else if ("single".equals(type) && object.has("resource")) {
								map.registerSprite(new ResourceLocation(object.get("resource").getAsString()));
								added++;
							}
						}
					} catch (RuntimeException e) {
						Translator.LOGGER.warn("Couldn't read the atlas sources {} of {}", name, jar.getName(), e);
					}
				}
			}
			for (var entry : entries) {
				var name = entry.getName();
				if (!name.startsWith("assets/") || !name.endsWith(".png")) {
					continue;
				}
				int slash = name.indexOf('/', "assets/".length());
				if (slash < 0 || !name.startsWith("/textures/", slash)) {
					continue;
				}
				String namespace = name.substring("assets/".length(), slash);
				String path = name.substring(slash + "/textures/".length(), name.length() - 4);
				if (path.startsWith("block/") || path.startsWith("item/")) {
					map.registerSprite(new ResourceLocation(namespace, path));
					added++;
					continue;
				}
				for (String[] directory : directories) {
					if (path.startsWith(directory[0] + "/")) {
						map.registerSprite(new ResourceLocation(namespace, directory[1] + path.substring(directory[0].length() + 1)));
						added++;
						break;
					}
				}
			}
		} catch (IOException e) {
			Translator.LOGGER.warn("Couldn't list the textures of {}", jar.getName(), e);
		}
		return added;
	}

	private static void register(TextureMap map, String namespace, String folder) {
		var prefix = "assets/" + namespace + "/textures/";
		try {
			for (var url : Collections.list(AtlasTextures.class.getClassLoader().getResources(prefix + folder))) {
				if ("file".equals(url.getProtocol())) {
					walk(map, namespace, new File(url.toURI()), folder);
				} else if ("jar".equals(url.getProtocol())) {
					var connection = (JarURLConnection) url.openConnection();
					connection.setUseCaches(false);
					for (var entry : Collections.list(connection.getJarFile().entries())) {
						var name = entry.getName();
						if (name.startsWith(prefix + folder + "/") && name.endsWith(".png")) {
							map.registerSprite(new ResourceLocation(namespace, name.substring(prefix.length(), name.length() - 4)));
						}
					}
				}
			}
		} catch (IOException | URISyntaxException e) {
			Translator.LOGGER.warn("Couldn't list the {} textures of {}", folder, namespace, e);
		}
	}

	private static void walk(TextureMap map, String namespace, File folder, String path) {
		var files = folder.listFiles();
		if (files == null) {
			return;
		}

		for (var file : files) {
			var name = path + "/" + file.getName();
			if (file.isDirectory()) {
				walk(map, namespace, file, name);
			} else if (name.endsWith(".png")) {
				map.registerSprite(new ResourceLocation(namespace, name.substring(0, name.length() - 4)));
			}
		}
	}
}
