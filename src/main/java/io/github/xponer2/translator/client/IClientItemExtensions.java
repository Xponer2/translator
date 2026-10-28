package io.github.xponer2.translator.client;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The parts of NeoForge's client item extensions 1.12.2 can do: the first person hand transform (GL transforms,
 * applied before the item in that hand is drawn).
 */
@SideOnly(Side.CLIENT)
public interface IClientItemExtensions {
	/** True when the extension replaced the vanilla transform (1.12.2 still applies it; kept for the 1.21 shape). */
	default boolean applyHandTransform(EntityPlayerSP player, EnumHandSide arm, ItemStack stack, float partialTick, float equipProcess, float swingProcess) {
		return false;
	}
}
