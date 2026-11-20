/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.adapter.Dispatch;
import ladder.adapter.render.ListRenderAccess;
import ladder.adapter.render.ListRenderApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;

/**
 * 1.20.4's list drawing with 1.20.5's hooks: where renderWidget draws the list's background (and later its shadows),
 * a converted mod list's own renderListBackground (renderListSeparators) draws instead; 1.20.4's part stays its own
 * otherwise, and is what the hooks' super calls draw.
 */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin implements ListRenderAccess {
    @Shadow
    private boolean renderBackground;

    @Shadow
    public abstract int getRight();

    @Shadow
    public abstract int getBottom();

    @Shadow
    public abstract double getScrollAmount();

    @Redirect(method = "renderWidget", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderBackground:Z",
            opcode = Opcodes.GETFIELD, ordinal = 0))
    private boolean ladder$listBackground(AbstractSelectionList<?> self, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (self instanceof ListRenderApi api && Dispatch.declares(self.getClass(), "renderListBackground", GuiGraphics.class)) {
            api.renderListBackground(graphics);
            return false;
        }
        return this.renderBackground;
    }

    @Redirect(method = "renderWidget", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderBackground:Z",
            opcode = Opcodes.GETFIELD, ordinal = 1))
    private boolean ladder$listSeparators(AbstractSelectionList<?> self, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (self instanceof ListRenderApi api && Dispatch.declares(self.getClass(), "renderListSeparators", GuiGraphics.class)) {
            api.renderListSeparators(graphics);
            return false;
        }
        return this.renderBackground;
    }

    @Override
    public void ladder$vanillaBackground(GuiGraphics graphics) {
        if (!this.renderBackground) {
            return;
        }
        AbstractSelectionList<?> self = (AbstractSelectionList<?>) (Object) this;
        graphics.setColor(0.125F, 0.125F, 0.125F, 1.0F);
        graphics.blit(Screen.BACKGROUND_LOCATION, self.getX(), self.getY(), (float) this.getRight(), (float) (this.getBottom() + (int) this.getScrollAmount()),
                self.getWidth(), self.getHeight(), 32, 32);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void ladder$vanillaSeparators(GuiGraphics graphics) {
        if (!this.renderBackground) {
            return;
        }
        AbstractSelectionList<?> self = (AbstractSelectionList<?>) (Object) this;
        graphics.fillGradient(RenderType.guiOverlay(), self.getX(), self.getY(), this.getRight(), self.getY() + 4, 0xFF000000, 0, 0);
        graphics.fillGradient(RenderType.guiOverlay(), self.getX(), this.getBottom() - 4, this.getRight(), this.getBottom(), 0, 0xFF000000, 0);
    }
}
