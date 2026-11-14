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
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
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
        // a vanilla type's 1.12.2 tile entity (Twilight Forest's chest renderer on BlockEntityType.CHEST): the tile entities
        // of the mods' blocks get the mod's renderer, vanilla's keep 1.12.2's (a mod's 1.21 one would draw every vanilla
        // chest; with none, Twilight Forest's chests were vanilla chests)
        if (tileClass.getName().startsWith("net.minecraft.")) {
            try {
                var vanilla = TileEntityRendererDispatcher.instance.renderers.get(tileClass);
                ClientRegistry.bindTileEntitySpecialRenderer((Class) tileClass,
                        new ModBlocks<>(new TesrAdapter<>(provider.create(BlockEntityRendererProvider.Context.INSTANCE)), (TileEntitySpecialRenderer) vanilla));
            } catch (RuntimeException | LinkageError | com.google.common.util.concurrent.ExecutionError | AssertionError e) {
                io.github.xponer2.translator.runtime.RuntimeStubs.failed("the block entity renderer of " + type, e);
            }
            return;
        }
        try {
            ClientRegistry.bindTileEntitySpecialRenderer((Class) tileClass, new TesrAdapter<>(provider.create(BlockEntityRendererProvider.Context.INSTANCE)));
        } catch (RuntimeException | LinkageError | com.google.common.util.concurrent.ExecutionError | AssertionError e) {
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
        } catch (RuntimeException | LinkageError | com.google.common.util.concurrent.ExecutionError | AssertionError e) {
            // a renderer of an original 1.21.1 mod that fails: logged once, the block entity is drawn without it
            io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.renderer.getClass().getName() + ".render", e);
        } finally {
            PoseStack.restore(depth);
            MultiBufferSource.INSTANCE.endBatch();
            GlStateManager.popMatrix();
        }
    }

    /**
     * The renderer of a vanilla tile entity class a mod registered a renderer for: the mod's for the tile entities of a
     * mod's block, the vanilla one for vanilla's.
     */
    static final class ModBlocks<T extends TileEntity> extends TileEntitySpecialRenderer<T> {
        private final TesrAdapter<T> mod;
        @javax.annotation.Nullable
        private final TileEntitySpecialRenderer<T> vanilla;

        ModBlocks(TesrAdapter<T> mod, @javax.annotation.Nullable TileEntitySpecialRenderer<T> vanilla) {
            this.mod = mod;
            this.vanilla = vanilla;
        }

        private boolean ofMod(T tile) {
            var world = tile.getWorld();
            if (world == null) {
                return false;
            }
            var id = world.getBlockState(tile.getPos()).getBlock().getRegistryName();
            return id != null && !"minecraft".equals(id.getNamespace());
        }

        @Override
        public void render(T tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
            if (this.ofMod(tile)) {
                this.mod.render(tile, x, y, z, partialTicks, destroyStage, alpha);
            } else if (this.vanilla != null) {
                this.vanilla.render(tile, x, y, z, partialTicks, destroyStage, alpha);
            }
        }

        @Override
        public void setRendererDispatcher(TileEntityRendererDispatcher dispatcher) {
            super.setRendererDispatcher(dispatcher);
            this.mod.setRendererDispatcher(dispatcher);
            if (this.vanilla != null) {
                this.vanilla.setRendererDispatcher(dispatcher);
            }
        }

        @Override
        public boolean isGlobalRenderer(T tile) {
            return this.ofMod(tile) ? this.mod.isGlobalRenderer(tile) : this.vanilla != null && this.vanilla.isGlobalRenderer(tile);
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
