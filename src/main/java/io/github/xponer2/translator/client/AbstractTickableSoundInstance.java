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

import java.util.Random;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

/**
 * 1.21 {@code AbstractTickableSoundInstance} (neoforge-1.21.1-supers.tsv): a sound that follows something (Mining
 * Gadgets' laser, Create's fans and whistles, Twilight Forest's cicadas). The mod moves it in {@link #tick} with the
 * 1.21 double position and ends it with {@link #stop}; 1.12.2 calls update and isDonePlaying and reads the position
 * as floats.
 */
public abstract class AbstractTickableSoundInstance extends MovingSound {
	protected double x;
	protected double y;
	protected double z;
	protected boolean relative;
	protected Random random;

	protected AbstractTickableSoundInstance(SoundEvent sound, SoundCategory source, Random random) {
		super(sound, source);
		this.random = random;
	}

	public abstract void tick();

	public boolean isStopped() {
		return this.donePlaying;
	}

	protected final void stop() {
		this.donePlaying = true;
	}

	public double getX() {
		return this.x;
	}

	public double getY() {
		return this.y;
	}

	public double getZ() {
		return this.z;
	}

	public boolean isRelative() {
		return this.relative;
	}

	/** 1.21: a sound that starts at volume 0 still plays (1.12.2 has no such sounds). */
	public boolean canStartSilent() {
		return false;
	}

	public boolean canPlaySound() {
		return true;
	}

	@Override
	public void update() {
		this.tick();
	}

	@Override
	public boolean isDonePlaying() {
		return this.isStopped();
	}

	@Override
	public float getXPosF() {
		return (float) this.x;
	}

	@Override
	public float getYPosF() {
		return (float) this.y;
	}

	@Override
	public float getZPosF() {
		return (float) this.z;
	}
}
