/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.datafixers.util.Either;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge 21's {@code GuiGraphics.renderComponentTooltipFromElements} on 20.6 (added to the class): a tooltip of text
 * lines and tooltip components in any order (JEI's), gathered through the same GatherComponents event 20.6 fires for
 * its own tooltips, each text line wrapped to the width a listener asks for.
 */
@Mixin(GuiGraphics.class)
abstract class GuiGraphicsTooltipMixin {
    @Shadow
    private ItemStack tooltipStack;

    @Shadow
    public abstract int guiWidth();

    @Shadow
    public abstract int guiHeight();

    @Shadow
    private void renderTooltipInternal(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner) {
        throw new AssertionError();
    }

    public void renderComponentTooltipFromElements(Font font, List<Either<FormattedText, TooltipComponent>> elements, int mouseX, int mouseY,
            ItemStack stack) {
        RenderTooltipEvent.GatherComponents event = new RenderTooltipEvent.GatherComponents(stack, this.guiWidth(), this.guiHeight(),
                new ArrayList<>(elements), -1);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return;
        }
        int maxWidth = event.getMaxWidth();
        List<ClientTooltipComponent> components = new ArrayList<>();
        for (Either<FormattedText, TooltipComponent> element : event.getTooltipElements()) {
            element.ifLeft(text -> {
                if (maxWidth > 0) {
                    font.split(text, maxWidth).forEach(line -> components.add(ClientTooltipComponent.create(line)));
                } else {
                    components.add(ClientTooltipComponent.create(Language.getInstance().getVisualOrder(text)));
                }
            }).ifRight(component -> components.add(ClientTooltipComponent.create(component)));
        }
        this.tooltipStack = stack;
        this.renderTooltipInternal(font, components, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE);
        this.tooltipStack = ItemStack.EMPTY;
    }
}
