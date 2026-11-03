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

    /** A sprite of the blocks atlas stretched to width x height (1.21 blit(x, y, z, width, height, sprite)). */
    public void blit(int x, int y, int z, int width, int height, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        this.minecraft.getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        new Gui().drawTexturedModalRect(x, y, sprite, width, height);
    }

    public void blitSprite(ResourceLocation sprite, int x, int y, int z, int width, int height) {
        this.blitSprite(sprite, x, y, width, height);
    }

    /** A part (u, v, width, height) of a GUI sprite of textureWidth x textureHeight. */
    public void blitSprite(ResourceLocation sprite, int textureWidth, int textureHeight, int u, int v, int x, int y, int width, int height) {
        this.minecraft.getTextureManager().bindTexture(spriteTexture(sprite));
        GlStateManager.enableBlend();
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight);
    }

    private static ResourceLocation spriteTexture(ResourceLocation sprite) {
        return new ResourceLocation(sprite.getNamespace(), "textures/gui/sprites/" + sprite.getPath() + ".png");
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

    public void fillGradient(RenderType type, int x1, int y1, int x2, int y2, int colorFrom, int colorTo, int z) {
        this.fillGradient(x1, y1, x2, y2, colorFrom, colorTo);
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

    public void setColor(float red, float green, float blue, float alpha) {
        net.minecraft.client.renderer.GlStateManager.color(red, green, blue, alpha);
    }

    public void hLine(int minX, int maxX, int y, int color) {
        this.fill(Math.min(minX, maxX), y, Math.max(minX, maxX) + 1, y + 1, color);
    }

    public void vLine(int x, int minY, int maxY, int color) {
        this.fill(x, Math.min(minY, maxY) + 1, x + 1, Math.max(minY, maxY), color);
    }

    public void fill(RenderType type, int x1, int y1, int x2, int y2, int color) {
        this.fill(x1, y1, x2, y2, color);
    }

    public void fill(int x1, int y1, int x2, int y2, int z, int color) {
        this.fill(x1, y1, x2, y2, color);
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int z, int colorFrom, int colorTo) {
        this.fillGradient(x1, y1, x2, y2, colorFrom, colorTo);
    }

    /** 1.21 blit of a region scaled to width x height. */
    public void blit(ResourceLocation texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, int textureWidth,
            int textureHeight) {
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
        this.minecraft.getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        this.blit(texture, x, y, (float) u, (float) v, width, height, textureWidth, textureHeight);
    }

    // 1.21 GUI sprites (textures/gui/sprites/<path>.png). The vanilla button sprites are the rows of the 1.12.2
    // widgets.png; any other sprite is its whole texture stretched to the size (no nine-slice).

    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");

    public void blitSprite(ResourceLocation sprite, int x, int y, int width, int height) {
        int row = "minecraft".equals(sprite.getNamespace()) ? buttonRow(sprite.getPath()) : -1;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        if (row >= 0) {
            this.minecraft.getTextureManager().bindTexture(WIDGETS);
            int half = width / 2;
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, row, half, height, 256, 256);
            Gui.drawModalRectWithCustomSizedTexture(x + half, y, 200 - (width - half), row, width - half, height, 256, 256);
            return;
        }
        this.minecraft.getTextureManager().bindTexture(new ResourceLocation(sprite.getNamespace(), "textures/gui/sprites/" + sprite.getPath() + ".png"));
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
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
        Gui.drawRect(x1, y1, x2, y2, color);
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
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
        var resolution = new ScaledResolution(this.minecraft);
        GuiUtils.drawHoveringText(lines, mouseX, mouseY, resolution.getScaledWidth(), resolution.getScaledHeight(), -1, font);
    }

    // items

    public void renderItem(ItemStack stack, int x, int y) {
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
