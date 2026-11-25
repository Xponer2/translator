/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Either;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge 21's tooltip gathering on 20.6's: 21 splits ClientHooks.gatherTooltipComponents into the tooltip's elements
 * (the texts, the item's component second) and gatherTooltipComponentsFromElements of them; 20.6 does both in
 * gatherTooltipComponents (an M row: the mods' points in FromElements are there, after the same elements are made).
 */
public final class TooltipAdapters {
    private TooltipAdapters() {
    }

    /** FromElements' elements of 20.6's texts and item component (an A row's plan): the same list 20.6 makes */
    public static List<Either<FormattedText, TooltipComponent>> elements(List<? extends FormattedText> texts, Optional<TooltipComponent> component) {
        List<Either<FormattedText, TooltipComponent>> elements = new ArrayList<>();
        for (FormattedText text : texts) {
            elements.add(Either.left(text));
        }
        component.ifPresent(c -> elements.add(1, Either.right(c)));
        return elements;
    }

    /**
     * NeoForge 21's {@code ClientHooks.gatherTooltipComponentsFromElements(stack, elements, ...)} (an X row): elements of
     * 20.6's shape (texts, at most one component second) go through 20.6's gatherTooltipComponents, so its mods' points
     * apply as on 21; any others are gathered the same way here (the event, then the wrapping)
     */
    public static List<ClientTooltipComponent> gatherTooltipComponentsFromElements(ItemStack stack, List<Either<FormattedText, TooltipComponent>> elements,
            int mouseX, int screenWidth, int screenHeight, Font fallbackFont) {
        List<FormattedText> texts = new ArrayList<>();
        TooltipComponent component = null;
        boolean shaped = true;
        for (int i = 0; i < elements.size() && shaped; i++) {
            Either<FormattedText, TooltipComponent> element = elements.get(i);
            if (element.left().isPresent()) {
                texts.add(element.left().get());
            } else if (i == 1 && component == null) {
                component = element.right().orElse(null);
            } else {
                shaped = false;
            }
        }
        if (shaped && !(elements.size() == 1 && component != null)) {
            return ClientHooks.gatherTooltipComponents(stack, texts, Optional.ofNullable(component), mouseX, screenWidth, screenHeight, fallbackFont);
        }
        Font font = ClientHooks.getTooltipFont(stack, fallbackFont);
        RenderTooltipEvent.GatherComponents event = new RenderTooltipEvent.GatherComponents(stack, screenWidth, screenHeight, new ArrayList<>(elements), -1);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return List.of();
        }
        int width = 0;
        for (Either<FormattedText, TooltipComponent> element : event.getTooltipElements()) {
            width = Math.max(width, element.map(font::width, c -> 0));
        }
        boolean wrap = false;
        int tooltipX = mouseX + 12;
        if (tooltipX + width + 4 > screenWidth) {
            tooltipX = mouseX - 16 - width;
            if (tooltipX < 4) {
                width = mouseX > screenWidth / 2 ? mouseX - 12 - 8 : screenWidth - 16 - mouseX;
                wrap = true;
            }
        }
        if (event.getMaxWidth() > 0 && width > event.getMaxWidth()) {
            width = event.getMaxWidth();
            wrap = true;
        }
        List<ClientTooltipComponent> out = new ArrayList<>();
        for (Either<FormattedText, TooltipComponent> element : event.getTooltipElements()) {
            if (element.right().isPresent()) {
                out.add(ClientTooltipComponent.create(element.right().get()));
                continue;
            }
            FormattedText text = element.left().get();
            if (!wrap) {
                out.add(ClientTooltipComponent.create(text instanceof Component c ? c.getVisualOrderText() : Language.getInstance().getVisualOrder(text)));
            } else if (text instanceof Component c && c.getString().isEmpty()) {
                out.add(ClientTooltipComponent.create(c.getVisualOrderText()));
            } else {
                font.split(text, width).forEach(line -> out.add(ClientTooltipComponent.create(line)));
            }
        }
        return out;
    }
}
