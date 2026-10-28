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
