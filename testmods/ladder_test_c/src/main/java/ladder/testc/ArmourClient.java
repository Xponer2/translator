/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/** Client only: which armour textures the armour layer draws for a zombie wearing the given pieces. */
final class ArmourClient {
    private static final Pattern TEXTURE = Pattern.compile("[a-z0-9_.-]+:textures/models/armor/[a-z0-9_./-]+\\.png");

    private ArmourClient() {
    }

    /** the armour textures drawn for a zombie wearing the given pieces, in order */
    static String drawn(ItemStack... pieces) {
        Minecraft minecraft = Minecraft.getInstance();
        Zombie zombie = EntityType.ZOMBIE.create(minecraft.level);
        zombie.setPos(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ());
        for (ItemStack piece : pieces) {
            zombie.setItemSlot(((ArmorItem) piece.getItem()).getEquipmentSlot(), piece);
        }
        List<String> textures = new ArrayList<>();
        MultiBufferSource.BufferSource real = minecraft.renderBuffers().bufferSource();
        MultiBufferSource recording = type -> {
            Matcher m = TEXTURE.matcher(type.toString());
            if (m.find()) {
                textures.add(m.group());
            }
            return real.getBuffer(type);
        };
        minecraft.getEntityRenderDispatcher().render(zombie, 0.0, 0.0, 0.0, 0.0F, 0.0F, new PoseStack(), recording, 15728880);
        real.endBatch();
        return String.join(" ", textures);
    }
}
