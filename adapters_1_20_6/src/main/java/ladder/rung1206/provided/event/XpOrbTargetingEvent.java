/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.provided.event;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

/**
 * NeoForge 21's {@code XpOrbTargetingEvent} (a P row): which player an experience orb follows, asked when it has none
 * near (XpOrbTargetingMixin posts it where 1.20.6 takes the nearest player). Unanswered, it is the nearest player
 * within the scan distance, as 1.20.6's orb takes; a listener may name another player, or none.
 */
public class XpOrbTargetingEvent extends Event {
    private final ExperienceOrb xpOrb;
    private final double scanDistance;
    @Nullable
    private Optional<Player> followingPlayer;

    public XpOrbTargetingEvent(ExperienceOrb xpOrb, double scanDistance) {
        this.xpOrb = xpOrb;
        this.scanDistance = scanDistance;
    }

    @Nullable
    public Player getFollowingPlayer() {
        return this.followingPlayer != null ? this.followingPlayer.orElse(null) : this.xpOrb.level().getNearestPlayer(this.xpOrb, this.scanDistance);
    }

    public void setFollowingPlayer(@Nullable Player player) {
        this.followingPlayer = Optional.ofNullable(player);
    }

    public ExperienceOrb getXpOrb() {
        return this.xpOrb;
    }

    public double getScanDistance() {
        return this.scanDistance;
    }
}
