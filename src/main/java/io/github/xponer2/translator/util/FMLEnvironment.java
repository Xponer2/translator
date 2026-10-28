package io.github.xponer2.translator.util;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

/**
 * NeoForge FMLEnvironment: the physical side and whether this is a release (obfuscated) environment.
 */
public final class FMLEnvironment {
	/** False in the development environment. */
	public static final boolean production = !Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"));
	public static final Side dist = FMLCommonHandler.instance().getSide();

	private FMLEnvironment() {
	}
}
