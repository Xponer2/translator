package io.github.xponer2.translator.client;

import io.github.xponer2.translator.registry.BlockEntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * NeoForge renderer registration, posted by the Translator on the client in init (the block entity types exist then).
 */
@SideOnly(Side.CLIENT)
public class EntityRenderersEvent extends Event {
	public static class RegisterRenderers extends EntityRenderersEvent {
		public <T extends TileEntity> void registerBlockEntityRenderer(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> provider) {
			TesrAdapter.register(type, provider);
		}
	}
}
