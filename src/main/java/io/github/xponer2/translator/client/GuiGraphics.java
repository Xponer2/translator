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
public final class GuiGraphics {
    private final Minecraft minecraft;

    public GuiGraphics(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public GuiGraphics() {
        this(Minecraft.getMinecraft());
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

    public void fill(int x1, int y1, int x2, int y2, int color) {
        Gui.drawRect(x1, y1, x2, y2, color);
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
