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
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.client.config.GuiUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The 1.21 GUI drawing API on 1.12.2 (immediate GL drawing with the {@code Gui} helpers). Screens create one per
 * frame; {@link #pose()} is the GL matrix. Client only.
 */
public class GuiGraphics {
    private final Minecraft minecraft;

    public GuiGraphics(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public GuiGraphics() {
        this(Minecraft.getMinecraft());
    }

    /** 1.21 {@code new GuiGraphics(minecraft, bufferSource)}: the buffer source is the Translator's one anyway. */
    public GuiGraphics(Minecraft minecraft, MultiBufferSource bufferSource) {
        this(minecraft);
    }

    /** 1.21's private {@code new GuiGraphics(minecraft, pose, bufferSource)} (a mod's own GuiGraphics, Storage Drawers'). */
    public GuiGraphics(Minecraft minecraft, PoseStack pose, MultiBufferSource bufferSource) {
        this(minecraft);
    }

    /** 1.21's fields (mods read them through access wideners): the GL matrix and the Translator's buffers. */
    public final PoseStack pose = PoseStack.INSTANCE;
    public final MultiBufferSource bufferSource = MultiBufferSource.INSTANCE;

    /** A sprite of the blocks atlas stretched to width x height (1.21 blit(x, y, z, width, height, sprite)). */
    public void blit(int x, int y, int z, int width, int height, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        if (sprite instanceof GuiSprite) {
            this.blitSprite(sprite, x, y, z, width, height);
            return;
        }
        this.flushOpen();
        this.minecraft.getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        new Gui().drawTexturedModalRect(x, y, sprite, width, height);
    }

    /**
     * 1.21 {@code blit(x, y, z, width, height, sprite, red, green, blue, alpha)}: the same, tinted (the empty-slot icons a
     * CyclingSlotBackground fades between: smithing and the mods' upgrade screens).
     */
    public void blit(int x, int y, int z, int width, int height, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, float red, float green,
            float blue, float alpha) {
        GlStateManager.color(red, green, blue, alpha);
        try {
            this.blit(x, y, z, width, height, sprite);
        } finally {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    /** A GUI sprite at a depth, stretched, tiled or nine-sliced as its .mcmeta says (1.21). */
    public void blitSprite(ResourceLocation sprite, int x, int y, int z, int width, int height) {
        this.flushOpen();
        int row = "minecraft".equals(sprite.getNamespace()) ? buttonRow(sprite.getPath()) : -1;
        if (row >= 0) {
            // the vanilla buttons: rows of the 1.12.2 widgets.png
            RenderSystem.applyShaderColor();
            GlStateManager.enableBlend();
            this.minecraft.getTextureManager().bindTexture(WIDGETS);
            int half = width / 2;
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, row, half, height, 256, 256);
            Gui.drawModalRectWithCustomSizedTexture(x + half, y, 200 - (width - half), row, width - half, height, 256, 256);
            return;
        }
        GuiSprite gui = GuiSprite.of(sprite);
        switch (gui.scaling) {
        case "tile":
            this.blitTiledSprite(gui, x, y, z, width, height, 0, 0, gui.scalingWidth, gui.scalingHeight, gui.scalingWidth, gui.scalingHeight);
            break;
        case "nine_slice":
            this.blitNineSlicedSprite(gui, x, y, z, width, height);
            break;
        default:
            this.blitSprite(gui, x, y, z, width, height);
        }
    }

    /** A part (u, v, width, height) of a GUI sprite of textureWidth x textureHeight. */
    public void blitSprite(ResourceLocation sprite, int textureWidth, int textureHeight, int u, int v, int x, int y, int width, int height) {
        this.blitSprite(GuiSprite.of(sprite), textureWidth, textureHeight, u, v, x, y, 0, width, height);
    }

    public void blitSprite(ResourceLocation sprite, int textureWidth, int textureHeight, int u, int v, int x, int y, int z, int width, int height) {
        this.blitSprite(GuiSprite.of(sprite), textureWidth, textureHeight, u, v, x, y, z, width, height);
    }

    // the sprite forms 1.21 keeps private (mods widen them: JEI, Jade): a sprite of the GUI atlas (a GuiSprite here) or
    // of the blocks atlas

    /** A whole sprite stretched over the rectangle. */
    public void blitSprite(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, int x, int y, int z, int width, int height) {
        if (width != 0 && height != 0) {
            this.innerBlit(spriteTexture(sprite), x, x + width, y, y + height, z, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV());
        }
    }

    /** A part (u, v, width, height) of a sprite taken as textureWidth x textureHeight pixels. */
    public void blitSprite(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, int textureWidth, int textureHeight, int u, int v, int x, int y,
            int z, int width, int height) {
        if (width == 0 || height == 0 || textureWidth <= 0 || textureHeight <= 0) {
            return;
        }
        float u0 = spriteU(sprite, (float) u / textureWidth);
        float u1 = spriteU(sprite, (float) (u + width) / textureWidth);
        float v0 = spriteV(sprite, (float) v / textureHeight);
        float v1 = spriteV(sprite, (float) (v + height) / textureHeight);
        this.innerBlit(spriteTexture(sprite), x, x + width, y, y + height, z, u0, u1, v0, v1);
    }

    /** A part of a sprite repeated over the rectangle (1.21 tiled and nine-slice sprites). */
    public void blitTiledSprite(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, int x, int y, int z, int width, int height, int u, int v,
            int spriteWidth, int spriteHeight, int textureWidth, int textureHeight) {
        if (width <= 0 || height <= 0 || spriteWidth <= 0 || spriteHeight <= 0) {
            return;
        }
        for (int i = 0; i < width; i += spriteWidth) {
            int w = Math.min(spriteWidth, width - i);
            for (int j = 0; j < height; j += spriteHeight) {
                int h = Math.min(spriteHeight, height - j);
                this.blitSprite(sprite, textureWidth, textureHeight, u, v, x + i, y + j, z, w, h);
            }
        }
    }

    private void blitNineSlicedSprite(GuiSprite sprite, int x, int y, int z, int width, int height) {
        this.blitNineSliced(sprite, sprite.scalingWidth, sprite.scalingHeight, sprite.left, sprite.top, sprite.right, sprite.bottom, x, y, z, width,
                height);
    }

    /**
     * 1.21 {@code blitNineSlicedSprite(sprite, nineSlice, x, y, z, width, height)} (private in 1.21, JEI widens it): the
     * carried NineSlice read by its record accessors.
     */
    public void blitNineSlicedSprite(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, Object nineSlice, int x, int y, int z, int width,
            int height) {
        try {
            Object border = nineSlice.getClass().getMethod("border").invoke(nineSlice);
            Class<?> sides = border.getClass();
            this.blitNineSliced(sprite, (Integer) nineSlice.getClass().getMethod("width").invoke(nineSlice),
                    (Integer) nineSlice.getClass().getMethod("height").invoke(nineSlice), (Integer) sides.getMethod("left").invoke(border),
                    (Integer) sides.getMethod("top").invoke(border), (Integer) sides.getMethod("right").invoke(border),
                    (Integer) sides.getMethod("bottom").invoke(border), x, y, z, width, height);
        } catch (ReflectiveOperationException | RuntimeException e) {
            this.blitSprite(sprite, x, y, z, width, height);
        }
    }

    /** 1.21 nine-slice: the corners as they are, the edges and the middle tiled. */
    private void blitNineSliced(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, int sw, int sh, int borderLeft, int borderTop,
            int borderRight, int borderBottom, int x, int y, int z, int width, int height) {
        int left = Math.min(borderLeft, width / 2);
        int right = Math.min(borderRight, width / 2);
        int top = Math.min(borderTop, height / 2);
        int bottom = Math.min(borderBottom, height / 2);
        if (width == sw && height == sh) {
            this.blitSprite(sprite, sw, sh, 0, 0, x, y, z, width, height);
        } else if (height == sh) {
            this.blitSprite(sprite, sw, sh, 0, 0, x, y, z, left, height);
            this.blitTiledSprite(sprite, x + left, y, z, width - right - left, height, left, 0, sw - right - left, sh, sw, sh);
            this.blitSprite(sprite, sw, sh, sw - right, 0, x + width - right, y, z, right, height);
        } else if (width == sw) {
            this.blitSprite(sprite, sw, sh, 0, 0, x, y, z, width, top);
            this.blitTiledSprite(sprite, x, y + top, z, width, height - bottom - top, 0, top, sw, sh - bottom - top, sw, sh);
            this.blitSprite(sprite, sw, sh, 0, sh - bottom, x, y + height - bottom, z, width, bottom);
        } else {
            this.blitSprite(sprite, sw, sh, 0, 0, x, y, z, left, top);
            this.blitTiledSprite(sprite, x + left, y, z, width - right - left, top, left, 0, sw - right - left, top, sw, sh);
            this.blitSprite(sprite, sw, sh, sw - right, 0, x + width - right, y, z, right, top);
            this.blitSprite(sprite, sw, sh, 0, sh - bottom, x, y + height - bottom, z, left, bottom);
            this.blitTiledSprite(sprite, x + left, y + height - bottom, z, width - right - left, bottom, left, sh - bottom, sw - right - left, bottom, sw,
                    sh);
            this.blitSprite(sprite, sw, sh, sw - right, sh - bottom, x + width - right, y + height - bottom, z, right, bottom);
            this.blitTiledSprite(sprite, x, y + top, z, left, height - bottom - top, 0, top, left, sh - bottom - top, sw, sh);
            this.blitTiledSprite(sprite, x + left, y + top, z, width - right - left, height - bottom - top, left, top, sw - right - left, sh - bottom - top,
                    sw, sh);
            this.blitTiledSprite(sprite, x + width - right, y + top, z, right, height - bottom - top, sw - right, top, right, sh - bottom - top, sw, sh);
        }
    }

    private static ResourceLocation spriteTexture(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        return sprite instanceof GuiSprite gui ? gui.texture() : net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE;
    }

    private static float spriteU(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, float fraction) {
        return sprite.getMinU() + (sprite.getMaxU() - sprite.getMinU()) * fraction;
    }

    private static float spriteV(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, float fraction) {
        return sprite.getMinV() + (sprite.getMaxV() - sprite.getMinV()) * fraction;
    }

    /** A textured rectangle at a depth, in the current color (1.21 innerBlit). */
    private void innerBlit(ResourceLocation texture, int x1, int x2, int y1, int y2, int z, float u0, float u1, float v0, float v1) {
        this.flushOpen();
        this.minecraft.getTextureManager().bindTexture(texture);
        GlStateManager.enableBlend();
        RenderSystem.applyShaderColor();
        var tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        var buffer = tessellator.getBuffer();
        buffer.begin(org.lwjgl.opengl.GL11.GL_QUADS, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x1, y1, z).tex(u0, v0).endVertex();
        buffer.pos(x1, y2, z).tex(u0, v1).endVertex();
        buffer.pos(x2, y2, z).tex(u1, v1).endVertex();
        buffer.pos(x2, y1, z).tex(u1, v0).endVertex();
        tessellator.draw();
    }

    public void renderItem(ItemStack stack, int x, int y, int seed) {
        this.renderItem(stack, x, y);
    }

    public void renderItem(ItemStack stack, int x, int y, int seed, int z) {
        this.renderItem(stack, x, y);
    }

    public void renderItem(@Nullable net.minecraft.entity.EntityLivingBase entity, ItemStack stack, int x, int y, int seed) {
        this.renderItem(stack, x, y);
    }

    /** 1.21 bounds of the GUI depth. */
    public static final float MAX_GUI_Z = 10000.0F;
    public static final float MIN_GUI_Z = -10000.0F;

    /**
     * A gradient in a render type, as 1.21 (guiOverlay: the slot highlight over the items, with no depth test; it was
     * drawn the plain way, under the items).
     */
    public void fillGradient(@Nullable RenderType type, int x1, int y1, int x2, int y2, int colorFrom, int colorTo, int z) {
        if (type == null) {
            this.fillGradient(x1, y1, x2, y2, z, colorFrom, colorTo);
            return;
        }
        var consumer = MultiBufferSource.INSTANCE.getBuffer(type);
        consumer.addVertex(x1, y1, z).setColor(colorFrom);
        consumer.addVertex(x1, y2, z).setColor(colorTo);
        consumer.addVertex(x2, y2, z).setColor(colorTo);
        consumer.addVertex(x2, y1, z).setColor(colorFrom);
        GlStateManager.shadeModel(org.lwjgl.opengl.GL11.GL_SMOOTH);
        MultiBufferSource.INSTANCE.endBatch();
        GlStateManager.shadeModel(org.lwjgl.opengl.GL11.GL_FLAT);
    }

    /** 1.21 fill in a render type at a depth. */
    public void fill(@Nullable RenderType type, int x1, int y1, int x2, int y2, int z, int color) {
        if (type == null) {
            this.fill(x1, y1, x2, y2, z, color);
            return;
        }
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        // the vertex order of 1.21 (from the larger corner), which faces the GUI camera
        var consumer = MultiBufferSource.INSTANCE.getBuffer(type);
        consumer.addVertex(maxX, maxY, z).setColor(color);
        consumer.addVertex(maxX, minY, z).setColor(color);
        consumer.addVertex(minX, minY, z).setColor(color);
        consumer.addVertex(minX, maxY, z).setColor(color);
        MultiBufferSource.INSTANCE.endBatch();
    }

    /** 1.21 {@code fillRenderType(type, x1, y1, x2, y2, z)}: a rectangle of a type that colors it itself (the end portal). */
    public void fillRenderType(RenderType type, int x1, int y1, int x2, int y2, int z) {
        var consumer = MultiBufferSource.INSTANCE.getBuffer(type);
        consumer.addVertex(x1, y1, z);
        consumer.addVertex(x1, y2, z);
        consumer.addVertex(x2, y2, z);
        consumer.addVertex(x2, y1, z);
        MultiBufferSource.INSTANCE.endBatch();
    }

    public void hLine(@Nullable RenderType type, int minX, int maxX, int y, int color) {
        if (maxX < minX) {
            int swap = minX;
            minX = maxX;
            maxX = swap;
        }
        this.fill(type, minX, y, maxX + 1, y + 1, color);
    }

    public void vLine(@Nullable RenderType type, int x, int minY, int maxY, int color) {
        if (maxY < minY) {
            int swap = minY;
            minY = maxY;
            maxY = swap;
        }
        this.fill(type, x, minY + 1, x + 1, maxY, color);
    }

    /** 1.21 {@code renderOutline(x, y, width, height, color)}: a one pixel frame. */
    public void renderOutline(int x, int y, int width, int height, int color) {
        this.fill(x, y, x + width, y + 1, color);
        this.fill(x, y + height - 1, x + width, y + height, color);
        this.fill(x, y + 1, x + 1, y + height - 1, color);
        this.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    /** 1.21 {@code drawManaged(drawing)}: batches drawn after it (here every draw is at once). */
    public void drawManaged(Runnable drawing) {
        this.flushOpen();
        drawing.run();
        this.flushOpen();
    }

    /** 1.21 {@code drawStringWithBackdrop}: text on the half dark backdrop of the accessibility text background. */
    public int drawStringWithBackdrop(FontRenderer font, ITextComponent text, int x, int y, int width, int color) {
        int alpha = 0x7F * (color >>> 24) / 255;
        if (alpha != 0) {
            this.fill(x - 2, y - 2, x + width + 2, y + 9 + 2, alpha << 24);
        }
        return this.drawString(font, text, x, y, color, true);
    }

    /** 1.21 {@code renderComponentHoverEffect(font, style, x, y)}: the hover text or item of a chat style. */
    public void renderComponentHoverEffect(FontRenderer font, @Nullable net.minecraft.util.text.Style style, int mouseX, int mouseY) {
        if (style == null || style.getHoverEvent() == null) {
            return;
        }
        var hover = style.getHoverEvent();
        if (hover.getAction() == net.minecraft.util.text.event.HoverEvent.Action.SHOW_TEXT) {
            this.renderTooltipLines(font, font.listFormattedStringToWidth(hover.getValue().getFormattedText(), Math.max(this.guiWidth() / 2, 200)),
                    mouseX, mouseY);
        } else if (hover.getAction() == net.minecraft.util.text.event.HoverEvent.Action.SHOW_ITEM) {
            try {
                var stack = new ItemStack(net.minecraft.nbt.JsonToNBT.getTagFromJson(hover.getValue().getUnformattedText()));
                if (!stack.isEmpty()) {
                    this.renderTooltip(font, stack, mouseX, mouseY);
                }
            } catch (net.minecraft.nbt.NBTException e) {
                // not an item: no tooltip, as 1.12.2's chat
            }
        }
    }

    /** NeoForge {@code renderComponentTooltip(font, lines, x, y, stack)}: the lines with the stack's tooltip events. */
    public void renderComponentTooltip(FontRenderer font, List<? extends ITextComponent> lines, int mouseX, int mouseY, ItemStack stack) {
        var strings = new ArrayList<String>();
        for (var line : lines) {
            strings.add(line.getFormattedText());
        }
        this.renderTooltipLines(font, strings, mouseX, mouseY, stack);
    }

    /** NeoForge {@code renderComponentTooltipFromElements}: the text elements (Either's left side); images are left out. */
    public void renderComponentTooltipFromElements(FontRenderer font, List<?> elements, int mouseX, int mouseY, ItemStack stack) {
        var strings = new ArrayList<String>();
        for (Object element : elements) {
            Object text = element instanceof com.mojang.datafixers.util.Either<?, ?> either ? either.left().orElse(null) : element;
            String line = tooltipText(text);
            if (line != null) {
                strings.add(line);
            }
        }
        this.renderTooltipLines(font, strings, mouseX, mouseY, stack);
    }

    public void renderFakeItem(ItemStack stack, int x, int y, int seed) {
        this.renderItem(stack, x, y);
    }

    /** The batches of the buffer source still open (a mod's own getBuffer) drawn first: they share the Tessellator. */
    private void flushOpen() {
        if (BufferVertexConsumer.drawing(net.minecraft.client.renderer.Tessellator.getInstance().getBuffer())) {
            MultiBufferSource.INSTANCE.endBatch();
        }
    }

    /**
     * 1.21 {@code renderTooltipInternal(font, components, x, y, positioner)}: the text of each tooltip component (a
     * text line holds a FormattedCharSequence, a text component here) drawn the 1.12.2 way; images are left out.
     */
    public void renderTooltipInternal(FontRenderer font, List<?> components, int mouseX, int mouseY, Object positioner) {
        var lines = new java.util.ArrayList<String>();
        for (Object component : components) {
            String text = tooltipText(component);
            if (text != null) {
                lines.add(text);
            }
        }
        if (!lines.isEmpty()) {
            this.renderTooltipLines(font, lines, mouseX, mouseY);
        }
    }

    @javax.annotation.Nullable
    private static String tooltipText(@javax.annotation.Nullable Object component) {
        if (component == null) {
            return null;
        }
        if (component instanceof ITextComponent text) {
            return text.getFormattedText();
        }
        for (Class<?> type = component.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                if (ITextComponent.class.isAssignableFrom(field.getType()) || String.class == field.getType()) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(component);
                        if (value instanceof ITextComponent text) {
                            return text.getFormattedText();
                        }
                        if (value instanceof String string) {
                            return string;
                        }
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        // not readable: the next field
                    }
                }
            }
        }
        return null;
    }

    /** A tooltip placed by a 1.21 positioner: placed the 1.12.2 way. */
    public void renderTooltip(FontRenderer font, List<ITextComponent> lines, Object positioner, int mouseX, int mouseY) {
        this.renderTooltip(font, lines, mouseX, mouseY);
    }

    public void renderTooltip(FontRenderer font, List<ITextComponent> lines, java.util.Optional<?> image, ItemStack stack, int mouseX, int mouseY) {
        this.renderTooltip(font, lines, mouseX, mouseY);
    }

    public boolean containsPointInScissor(int x, int y) {
        return true;
    }

    /** Draws what was batched (immediate here: the buffer source's open batch). */
    public void flush() {
        MultiBufferSource.INSTANCE.endBatch();
    }

    /** 1.21 shader color: sprites and blits are drawn in it (see RenderSystem.setShaderColor). */
    public void setColor(float red, float green, float blue, float alpha) {
        RenderSystem.setShaderColor(red, green, blue, alpha);
    }

    public void hLine(int minX, int maxX, int y, int color) {
        this.fill(Math.min(minX, maxX), y, Math.max(minX, maxX) + 1, y + 1, color);
    }

    public void vLine(int x, int minY, int maxY, int color) {
        this.fill(x, Math.min(minY, maxY) + 1, x + 1, Math.max(minY, maxY), color);
    }

    public void fill(@Nullable RenderType type, int x1, int y1, int x2, int y2, int color) {
        this.fill(type, x1, y1, x2, y2, 0, color);
    }

    /** At a depth: over (or under) the items, which 1.21 screens draw at 100 to 200. */
    public void fill(int x1, int y1, int x2, int y2, int z, int color) {
        if (z == 0) {
            this.fill(x1, y1, x2, y2, color);
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, (float) z);
        this.fill(x1, y1, x2, y2, color);
        GlStateManager.popMatrix();
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int z, int colorFrom, int colorTo) {
        if (z == 0) {
            this.fillGradient(x1, y1, x2, y2, colorFrom, colorTo);
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, (float) z);
        this.fillGradient(x1, y1, x2, y2, colorFrom, colorTo);
        GlStateManager.popMatrix();
    }

    /** 1.21 blit of a region scaled to width x height. */
    public void blit(ResourceLocation texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, int textureWidth,
            int textureHeight) {
        this.flushOpen();
        this.minecraft.getTextureManager().bindTexture(texture);
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        Gui.drawScaledCustomSizeModalRect(x, y, u, v, uWidth, vHeight, width, height, textureWidth, textureHeight);
    }

    /** 1.21 blit with a z offset (blitOffset): drawn at the current depth. */
    public void blit(ResourceLocation texture, int x, int y, int z, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        this.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public int drawString(FontRenderer font, ITextComponent text, float x, float y, int color, boolean shadow) {
        return font.drawString(text.getFormattedText(), x, y, color, shadow);
    }

    public int drawString(FontRenderer font, @Nullable String text, float x, float y, int color, boolean shadow) {
        return text == null ? 0 : font.drawString(text, x, y, color, shadow);
    }

    public void drawWordWrap(FontRenderer font, ITextComponent text, int x, int y, int width, int color) {
        font.drawSplitString(text.getFormattedText(), x, y, width, color);
    }

    /** 1.21 tooltip with an image component: the text lines (1.12.2 tooltips have no images). */
    public void renderTooltip(FontRenderer font, List<ITextComponent> lines, java.util.Optional<?> image, int mouseX, int mouseY) {
        this.renderTooltip(font, lines, mouseX, mouseY);
    }

    public PoseStack pose() {
        return PoseStack.INSTANCE;
    }

    public MultiBufferSource bufferSource() {
        return MultiBufferSource.INSTANCE;
    }

    public int guiWidth() {
        return new ScaledResolution(this.minecraft).getScaledWidth();
    }

    public int guiHeight() {
        return new ScaledResolution(this.minecraft).getScaledHeight();
    }

    // textures: 1.21 blit without texture size = a 256x256 texture

    public void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        this.blit(texture, x, y, (float) u, (float) v, width, height, 256, 256);
    }

    public void blit(ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        this.flushOpen();
        this.minecraft.getTextureManager().bindTexture(texture);
        RenderSystem.applyShaderColor();
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        this.blit(texture, x, y, (float) u, (float) v, width, height, textureWidth, textureHeight);
    }

    // 1.21 GUI sprites (textures/gui/sprites/<path>.png). The vanilla button sprites are the rows of the 1.12.2
    // widgets.png; any other sprite is its whole texture stretched to the size (no nine-slice).

    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");

    public void blitSprite(ResourceLocation sprite, int x, int y, int width, int height) {
        this.blitSprite(sprite, x, y, 0, width, height);
    }

    private static int buttonRow(String path) {
        switch (path) {
            case "widget/button":
                return 66;
            case "widget/button_disabled":
                return 46;
            case "widget/button_highlighted":
                return 86;
            default:
                return -1;
        }
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        this.flushOpen();
        Gui.drawRect(x1, y1, x2, y2, color);
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
        this.flushOpen();
        GuiUtils.drawGradientRect(0, x1, y1, x2, y2, colorFrom, colorTo);
    }

    /** Clips drawing to a rectangle in GUI coordinates (one level: 1.21 keeps a stack). */
    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        int scale = new ScaledResolution(this.minecraft).getScaleFactor();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(minX * scale, this.minecraft.displayHeight - maxY * scale, Math.max(0, maxX - minX) * scale,
                Math.max(0, maxY - minY) * scale);
    }

    public void disableScissor() {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }

    /**
     * 1.21's {@code applyScissor(rectangle)} (a mod's own GuiGraphics calls it: GuideMe's scaled one): clip to the
     * rectangle (a 1.21 ScreenRectangle, read by its record accessors), none when null.
     */
    protected void applyScissor(@javax.annotation.Nullable Object rectangle) {
        this.flushOpen();
        if (rectangle == null) {
            this.disableScissor();
            return;
        }
        try {
            int left = (Integer) rectangle.getClass().getMethod("left").invoke(rectangle);
            int top = (Integer) rectangle.getClass().getMethod("top").invoke(rectangle);
            int width = (Integer) rectangle.getClass().getMethod("width").invoke(rectangle);
            int height = (Integer) rectangle.getClass().getMethod("height").invoke(rectangle);
            this.enableScissor(left, top, left + width, top + height);
        } catch (ReflectiveOperationException | RuntimeException e) {
            this.disableScissor();
        }
    }

    // text

    public int drawString(FontRenderer font, @Nullable String text, int x, int y, int color, boolean shadow) {
        return text == null ? 0 : font.drawString(text, x, y, color, shadow);
    }

    public int drawString(FontRenderer font, @Nullable String text, int x, int y, int color) {
        return this.drawString(font, text, x, y, color, true);
    }

    public int drawString(FontRenderer font, ITextComponent text, int x, int y, int color, boolean shadow) {
        return this.drawString(font, text.getFormattedText(), x, y, color, shadow);
    }

    public int drawString(FontRenderer font, ITextComponent text, int x, int y, int color) {
        return this.drawString(font, text.getFormattedText(), x, y, color, true);
    }

    public void drawCenteredString(FontRenderer font, String text, int x, int y, int color) {
        this.drawString(font, text, x - font.getStringWidth(text) / 2, y, color, true);
    }

    public void drawCenteredString(FontRenderer font, ITextComponent text, int x, int y, int color) {
        this.drawCenteredString(font, text.getFormattedText(), x, y, color);
    }

    // tooltips

    public void renderTooltip(FontRenderer font, ITextComponent text, int mouseX, int mouseY) {
        this.renderTooltipLines(font, Collections.singletonList(text.getFormattedText()), mouseX, mouseY);
    }

    public void renderTooltip(FontRenderer font, List<ITextComponent> lines, int mouseX, int mouseY) {
        this.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    public void renderComponentTooltip(FontRenderer font, List<ITextComponent> lines, int mouseX, int mouseY) {
        var strings = new ArrayList<String>();
        for (var line : lines) {
            strings.add(line.getFormattedText());
        }
        this.renderTooltipLines(font, strings, mouseX, mouseY);
    }

    public void renderTooltip(FontRenderer font, ItemStack stack, int mouseX, int mouseY) {
        var lines = stack.getTooltip(this.minecraft.player, this.minecraft.gameSettings.advancedItemTooltips
                ? net.minecraft.client.util.ITooltipFlag.TooltipFlags.ADVANCED : net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL);
        GuiUtils.preItemToolTip(stack);
        this.renderTooltipLines(font, lines, mouseX, mouseY);
        GuiUtils.postItemToolTip();
    }

    private void renderTooltipLines(FontRenderer font, List<String> lines, int mouseX, int mouseY) {
        this.flushOpen();
        var resolution = new ScaledResolution(this.minecraft);
        GuiUtils.drawHoveringText(lines, mouseX, mouseY, resolution.getScaledWidth(), resolution.getScaledHeight(), -1, font);
    }

    private void renderTooltipLines(FontRenderer font, List<String> lines, int mouseX, int mouseY, ItemStack stack) {
        if (lines.isEmpty()) {
            return;
        }
        if (stack.isEmpty()) {
            this.renderTooltipLines(font, lines, mouseX, mouseY);
            return;
        }
        GuiUtils.preItemToolTip(stack);
        this.renderTooltipLines(font, lines, mouseX, mouseY);
        GuiUtils.postItemToolTip();
    }

    // items

    public void renderItem(ItemStack stack, int x, int y) {
        this.flushOpen();
        RenderHelper.enableGUIStandardItemLighting();
        this.minecraft.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
    }

    public void renderFakeItem(ItemStack stack, int x, int y) {
        this.renderItem(stack, x, y);
    }

    public void renderItemDecorations(FontRenderer font, ItemStack stack, int x, int y) {
        this.renderItemDecorations(font, stack, x, y, null);
    }

    public void renderItemDecorations(FontRenderer font, ItemStack stack, int x, int y, @Nullable String text) {
        this.minecraft.getRenderItem().renderItemOverlayIntoGUI(font, stack, x, y, text);
    }
}
