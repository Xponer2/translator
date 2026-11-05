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

import io.github.xponer2.translator.registry.BlockEntityType;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.client.registry.ClientRegistry;

/**
 * Draws a 1.21 tile entity renderer as a 1.12.2 TESR: the GL matrix is moved to the tile entity (1.21 renderers draw
 * relative to the block) and the packed light is the block's light. Client only.
 */
public final class TesrAdapter<T extends TileEntity> extends TileEntitySpecialRenderer<T> {
    private final BlockEntityRenderer<T> renderer;

    public TesrAdapter(BlockEntityRenderer<T> renderer) {
        this.renderer = renderer;
    }

    /**
     * 1.21 {@code registerBlockEntityRenderer(type, provider)}: after the block entity type was registered (init).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends TileEntity> void register(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> provider) {
        // never thrown into the mod: its client setup goes on with its other renderers (Biomes O' Plenty's stopped here)
        if (type == null || provider == null) {
            io.github.xponer2.translator.runtime.RuntimeStubs.missing("a block entity renderer of a type that isn't there");
            return;
        }
        var tileClass = type.getTileClass();
        if (tileClass == null) {
            io.github.xponer2.translator.runtime.RuntimeStubs.missing("the block entity renderer of " + type + " (its type has no class yet)");
            return;
        }
        // signs: SignRenderers draws them (the 1.21 sign renderer reads what 1.12.2's sign hasn't)
        if (io.github.xponer2.translator.runtime.mc.SignBlockEntity.class.isAssignableFrom(tileClass)) {
            return;
        }
        try {
            ClientRegistry.bindTileEntitySpecialRenderer((Class) tileClass, new TesrAdapter<>(provider.create(BlockEntityRendererProvider.Context.INSTANCE)));
        } catch (RuntimeException | LinkageError e) {
            io.github.xponer2.translator.runtime.RuntimeStubs.failed("the block entity renderer of " + type, e);
        }
    }

    @Override
    public void render(T tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        var world = tile.getWorld();
        var light = world != null ? world.getCombinedLight(tile.getPos(), 0) : 0xF000F0;
        int depth = PoseStack.depth();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        try {
            this.renderer.render(tile, partialTicks, PoseStack.INSTANCE, MultiBufferSource.INSTANCE, light, OverlayTexture.NO_OVERLAY);
        } catch (RuntimeException | LinkageError e) {
            // a renderer of an original 1.21.1 mod that fails: logged once, the block entity is drawn without it
            io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.renderer.getClass().getName() + ".render", e);
        } finally {
            PoseStack.restore(depth);
            MultiBufferSource.INSTANCE.endBatch();
            GlStateManager.popMatrix();
        }
    }

    /** The 1.21 renderer this adapter draws with. */
    public BlockEntityRenderer<T> renderer() {
        return this.renderer;
    }

    @Override
    public boolean isGlobalRenderer(T tile) {
        return this.renderer.isGlobalRenderer(tile);
    }
}
