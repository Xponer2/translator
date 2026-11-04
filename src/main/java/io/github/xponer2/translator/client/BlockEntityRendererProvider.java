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

        /** 1.21 {@code context.bakeLayer(layer)}: the model parts of a layer the mod registered (its block entity models). */
        public io.github.xponer2.translator.client.model.ModelPart bakeLayer(Object location) {
            return io.github.xponer2.translator.client.model.EntityModelSet.INSTANCE.bakeLayer(location);
        }

        public io.github.xponer2.translator.client.model.EntityModelSet getModelSet() {
            return io.github.xponer2.translator.client.model.EntityModelSet.INSTANCE;
        }

        public net.minecraft.client.renderer.BlockRendererDispatcher getBlockRenderDispatcher() {
            return Minecraft.getMinecraft().getBlockRendererDispatcher();
        }

        public net.minecraft.client.renderer.RenderItem getItemRenderer() {
            return Minecraft.getMinecraft().getRenderItem();
        }

        public net.minecraft.client.gui.FontRenderer getFont() {
            return Minecraft.getMinecraft().fontRenderer;
        }

        public net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher getBlockEntityRenderDispatcher() {
            return net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.instance;
        }
    }
}
