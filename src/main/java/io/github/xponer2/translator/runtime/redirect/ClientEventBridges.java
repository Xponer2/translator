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
package io.github.xponer2.translator.runtime.redirect;

import java.util.UUID;

import io.github.xponer2.translator.client.DeltaTracker;
import io.github.xponer2.translator.client.MultiBufferSource;
import io.github.xponer2.translator.client.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;

/**
 * 1.21 client event getters the 1.12.2 events lack (R lines). Client only.
 * <ul>
 * <li>HUD events (RenderGuiEvent / RenderGuiLayerEvent on RenderGameOverlayEvent): the partial tick is a
 * {@link DeltaTracker}.</li>
 * <li>Chat: 1.12.2 chat packets carry no sender, so every message is a system message (the nil UUID, as 1.21 gives
 * for system messages).</li>
 * <li>RenderHandEvent (= 1.12.2 RenderSpecificHandEvent): the pose is the GL matrix of the hand, buffers are drawn at
 * once, the light is the one at the player's eyes.</li>
 * </ul>
 */
public final class ClientEventBridges {
    private static final UUID NIL_UUID = new UUID(0L, 0L);

    private ClientEventBridges() {
    }

    public static DeltaTracker getPartialTick(RenderGameOverlayEvent event) {
        return DeltaTracker.INSTANCE;
    }

    public static UUID getSender(ClientChatReceivedEvent event) {
        return NIL_UUID;
    }

    public static boolean isSystem(ClientChatReceivedEvent event) {
        return true;
    }

    public static PoseStack getPoseStack(RenderSpecificHandEvent event) {
        return PoseStack.INSTANCE;
    }

    public static MultiBufferSource getMultiBufferSource(RenderSpecificHandEvent event) {
        return MultiBufferSource.INSTANCE;
    }

    /** The packed light (block &lt;&lt; 4 | sky &lt;&lt; 20, the same layout in 1.12.2 and 1.21) at the player's eyes. */
    public static int getPackedLight(RenderSpecificHandEvent event) {
        var mc = Minecraft.getMinecraft();
        var player = mc.player;
        if (player == null || mc.world == null) {
            return 0xF000F0;
        }
        return mc.world.getCombinedLight(new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ), 0);
    }
}
