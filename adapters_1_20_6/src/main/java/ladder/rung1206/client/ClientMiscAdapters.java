/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.client;

/** 1.21's small client members on 1.20.6 (apart from the common adapters: a dedicated server never loads this class). */
public final class ClientMiscAdapters {
    private ClientMiscAdapters() {
    }

    /** 1.21's {@code ConfirmLinkScreen.confirmLinkNow(screen, url, trusted)}: 1.20.6's own takes every link as trusted */
    public static void confirmLinkNow(net.minecraft.client.gui.screens.Screen screen, String url, boolean trusted) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmLinkScreen(open -> {
            if (open) {
                net.minecraft.Util.getPlatform().openUri(url);
            }
            minecraft.setScreen(screen);
        }, url, trusted));
    }

    /** 1.21's {@code Options.getMenuBackgroundBlurriness()}: 0-10 (1.20.6 keeps 0-1) */
    public static int getMenuBackgroundBlurriness(net.minecraft.client.Options options) {
        return (int) Math.round(options.getMenuBackgroundBlurriness() * 10.0);
    }

    /** {@code ModelBakery.MISSING_MODEL_VARIANT}: 20.6's missing model location */
    public static net.minecraft.client.resources.model.ModelResourceLocation missingModelVariant() {
        return net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_LOCATION;
    }
}
