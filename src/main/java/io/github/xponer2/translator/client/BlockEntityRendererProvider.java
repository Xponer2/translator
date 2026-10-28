package io.github.xponer2.translator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;

/**
 * Creates a tile entity renderer (1.21 {@code BlockEntityRendererProvider}). Client only.
 */
@FunctionalInterface
public interface BlockEntityRendererProvider<T extends TileEntity> {
    BlockEntityRenderer<T> create(Context context);

    final class Context {
        public static final Context INSTANCE = new Context();

        private Context() { }

        public Minecraft getMinecraft() {
            return Minecraft.getMinecraft();
        }

        /** 1.21 entity render dispatcher (render calls go to {@link Rendering#renderEntity}). */
        public net.minecraft.client.renderer.entity.RenderManager getEntityRenderer() {
            return Minecraft.getMinecraft().getRenderManager();
        }
    }
}
