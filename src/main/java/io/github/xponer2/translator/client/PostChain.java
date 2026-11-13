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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.gson.JsonSyntaxException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code PostChain} (S row) for the post effects mods make or extend (Moonlight's composed post
 * chain, which Supplementaries' mob head views use): 1.12.2's shader group with the 1.21 constructor. 1.12.2's
 * constructor reads the effect itself, by a private method a subclass can't replace, and takes 1.12.2's resource
 * manager: here it starts from an empty effect, then reads the effect through {@code load} (1.12.2's name
 * parseGroup), which a mod's subclass overrides as in 1.21.
 */
@SideOnly(Side.CLIENT)
public class PostChain extends ShaderGroup {
	private static final ResourceLocation EMPTY = new ResourceLocation("translator", "shaders/post/empty.json");

	public PostChain(TextureManager textureManager, io.github.xponer2.translator.resources.ResourceManager resources, Framebuffer screenTarget,
			ResourceLocation location) throws IOException, JsonSyntaxException {
		super(textureManager, Minecraft.getMinecraft().getResourceManager(), screenTarget, EMPTY);
		try {
			ObfuscationReflectionHelper.setPrivateValue(ShaderGroup.class, this, location.toString(), "field_148034_c");
		} catch (RuntimeException e) {
			// the name stays the empty effect's
		}
		this.parseGroup(textureManager, location);
	}

	/** 1.21 {@code load(textureManager, location)} (1.12.2's name): the effect's targets and passes, from its file. */
	public void parseGroup(TextureManager textureManager, ResourceLocation location) throws IOException, JsonSyntaxException {
		try {
			PARSE.invoke(this, textureManager, location);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof IOException io) {
				throw io;
			}
			if (e.getCause() instanceof RuntimeException runtime) {
				throw runtime;
			}
			throw new IOException(e.getCause());
		} catch (IllegalAccessException e) {
			throw new IOException(e);
		}
	}

	private static final Method PARSE = ObfuscationReflectionHelper.findMethod(ShaderGroup.class, "func_152765_a", void.class, TextureManager.class,
			ResourceLocation.class);
}
