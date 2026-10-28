package io.github.xponer2.translator.client;

/**
 * The damage/hurt overlay of 1.21 ({@code OverlayTexture}); 1.12.2 has none, only the "no overlay" value is used.
 */
public final class OverlayTexture {
    public static final int NO_OVERLAY = pack(0, 10);

    private OverlayTexture() { }

    public static int pack(int u, int v) {
        return u | v << 16;
    }
}
