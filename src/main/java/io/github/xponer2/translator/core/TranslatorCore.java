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
package io.github.xponer2.translator.core;

import java.io.File;
import java.util.Map;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/**
 * The Translator as a coremod: the 1.21.1 mods are converted before Minecraft's classes load, and their mixin configs
 * go to Mixin then, so their mixins into Minecraft classes apply (in pre-init those classes are loaded already). The
 * mods are still started in pre-init ({@code runtime.RuntimeMods}). Mixin's transformer is checked ({@link SafeMixins}):
 * a mixin that makes a Minecraft class invalid is left out instead of stopping the game. Nothing here touches a
 * Minecraft class.
 */
@IFMLLoadingPlugin.Name("Translator")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public final class TranslatorCore implements IFMLLoadingPlugin {
	public TranslatorCore() {
		// the coremod's own classes and the ASM they use load untransformed (a transformer of ours loading them would
		// come back to itself)
		net.minecraft.launchwrapper.Launch.classLoader.addTransformerExclusion("io.github.xponer2.translator.core.");
		net.minecraft.launchwrapper.Launch.classLoader.addTransformerExclusion("xyz.wagyourtail.jvmdg.shade.asm.");
	}

	@Override
	public String[] getASMTransformerClass() {
		// wraps Mixin's transformer once Mixin has registered it (SafeMixins)
		return new String[] { SafeMixins.Installer.class.getName(), EnumExtender.class.getName(), AccessWidener.class.getName(),
				BlockIdLimit.class.getName(), MenuClicks.class.getName(), ParticleFriction.class.getName(), SlotDrawing.class.getName(),
				ModLootTables.class.getName(), TileEntityCreate.class.getName(), BufferBuilderBegin.class.getName() };
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		Object gameDir = data.get("mcLocation");
		if (gameDir instanceof File) {
			io.github.xponer2.translator.runtime.Conversion.runEarly((File) gameDir);
		}
		SafeMixins.install();
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
