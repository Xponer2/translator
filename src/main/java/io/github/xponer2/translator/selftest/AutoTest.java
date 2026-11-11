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
package io.github.xponer2.translator.selftest;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The self-test without a person at the keyboard: with {@code -Dtranslator.autotest=<world folder>} the client opens
 * (or creates: superflat, creative, commands on) that world from the main menu, runs {@code /translatortest} once the
 * world has loaded, and quits the game when the report is written (or after 2 hours, or when the server stopped).
 * A script can then build, start, wait for the game to exit and read the report and the log; it starts the game again
 * after a crash, the test goes on after what crashed it. {@code -Dtranslator.autotest.shard=k/n} tests every n-th block
 * and item (copies of the game share a test), {@code -Dtranslator.autotest.window=x,y} puts the window there.
 */
@SideOnly(Side.CLIENT)
public final class AutoTest {
	private static final Logger LOGGER = LogManager.getLogger("Translator AutoTest");
	private static final long TIMEOUT_MS = 120 * 60 * 1000L;
	/** How long the client may go without a tick while the test runs before the watchdog quits the game. */
	private static final long STUCK_MS = 3 * 60 * 1000L;

	private final String folder;
	private volatile int state;
	private int ticks;
	private long started;
	private ScreenTour tour;
	/** The server of the world left for the save-and-reload check, until it has stopped (and saved). */
	private net.minecraft.server.MinecraftServer leaving;
	private volatile long lastTick;
	private volatile Thread client;

	private AutoTest(String folder) {
		this.folder = folder;
	}

	/** Starts when the system property is set (client only). */
	public static void init() {
		String folder = System.getProperty("translator.autotest");
		if (folder == null || folder.isEmpty()) {
			return;
		}
		LOGGER.info("Automatic self-test in world '{}'", folder);
		Minecraft mc = Minecraft.getMinecraft();
		// several copies side by side: none pauses when another one has the focus
		mc.gameSettings.pauseOnLostFocus = false;
		String window = System.getProperty("translator.autotest.window");
		if (window != null && window.matches("-?\\d+,-?\\d+")) {
			String[] at = window.split(",");
			org.lwjgl.opengl.Display.setLocation(Integer.parseInt(at[0]), Integer.parseInt(at[1]));
		}
		AutoTest test = new AutoTest(folder);
		MinecraftForge.EVENT_BUS.register(test);
		Thread watchdog = new Thread(test::watch, "Translator AutoTest watchdog");
		watchdog.setDaemon(true);
		watchdog.start();
	}

	/**
	 * Quits a game whose client stopped ticking during the test. When the server stops after a player was disconnected,
	 * 1.12.2's client waits for the stopped server to save the player, which never happens: without this the copy
	 * would wait there until the test's time is up.
	 */
	private void watch() {
		while (true) {
			try {
				Thread.sleep(10_000L);
			} catch (InterruptedException e) {
				return;
			}
			long last = this.lastTick;
			if ((this.state == 2 || this.state >= 4) && this.state != 5 && last != 0 && System.currentTimeMillis() - last > STUCK_MS) {
				var stuck = new StringBuilder();
				Thread thread = this.client;
				if (thread != null) {
					for (StackTraceElement element : thread.getStackTrace()) {
						stuck.append("\n\tat ").append(element);
					}
				}
				LOGGER.error("Automatic self-test: the client stopped responding for {} s, quitting{}", (System.currentTimeMillis() - last) / 1000, stuck);
				LogManager.shutdown();
				Runtime.getRuntime().halt(2);
			}
		}
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		this.ticks++;
		this.lastTick = System.currentTimeMillis();
		this.client = Thread.currentThread();
		if (this.started != 0 && System.currentTimeMillis() - this.started > TIMEOUT_MS) {
			LOGGER.error("Automatic self-test timed out, quitting");
			mc.shutdown();
			return;
		}
		switch (this.state) {
			case 0 -> {
				// the main menu: the mods are loaded
				if (mc.currentScreen instanceof GuiMainMenu) {
					this.started = System.currentTimeMillis();
					File save = new File(mc.gameDir, "saves/" + this.folder);
					WorldSettings settings = null;
					if (!new File(save, "level.dat").isFile()) {
						settings = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands();
					}
					LOGGER.info("Opening world '{}' ({})", this.folder, settings == null ? "existing" : "new superflat");
					mc.launchIntegratedServer(this.folder, this.folder, settings);
					this.state = 1;
					this.ticks = 0;
				}
			}
			case 1 -> {
				// in the world: give the chunks around the player 5 seconds
				if (mc.player != null && mc.world != null && mc.currentScreen == null) {
					if (this.ticks > 100) {
						LOGGER.info("Running /translatortest");
						mc.player.sendChatMessage("/translatortest");
						this.state = 2;
						this.ticks = 0;
					}
				} else {
					this.ticks = 0;
				}
			}
			case 2 -> {
				// the test runs on the server thread; done when no run is left (after it started)
				if (this.ticks > 40 && SelfTest.running() == null) {
					// the first copy (or the only one) also tours the screens of the mods that change the interface
					String shard = System.getProperty("translator.autotest.shard");
					if (shard == null || shard.startsWith("1/")) {
						// then what the mods keep across leaving the world and coming back, then the screens
						LOGGER.info("Self-test finished, placing block entities and mobs for the save-and-reload check");
						mc.player.sendChatMessage("/translatorpersist place");
						this.state = 6;
					} else {
						LOGGER.info("Self-test finished, quitting");
						this.state = 3;
					}
					this.ticks = 0;
				} else if (this.ticks > 40 && (mc.getIntegratedServer() == null || !mc.getIntegratedServer().isServerRunning() || mc.world == null)) {
					// a crash of the server thread: the client stays, showing it; the script starts the game again
					LOGGER.error("Automatic self-test: the server stopped, quitting");
					mc.shutdown();
				} else if (mc.currentScreen != null && mc.currentScreen.doesGuiPauseGame()) {
					// a screen that pauses the game (opened by a mod on the client) would stop the test
					LOGGER.info("Closing {}: it pauses the game", mc.currentScreen.getClass().getName());
					mc.displayGuiScreen(null);
				}
			}
			case 3 -> {
				if (this.ticks > 40) {
					if (Boolean.getBoolean("translator.autotest.stay")) {
						// -Dtranslator.autotest.stay=true: the game stays in the world for a look (the report is written)
						LOGGER.info("Self-test finished, staying in the world (translator.autotest.stay)");
						this.state = 5;
						this.started = 0;
					} else {
						mc.shutdown();
					}
				}
			}
			case 4 -> {
				if (this.tour.tick()) {
					LOGGER.info("Self-test finished, quitting");
					this.state = 3;
					this.ticks = 0;
				}
			}
			case 6 -> {
				// the placed blocks tick a little, then what they save is noted
				if (this.ticks > 60) {
					mc.player.sendChatMessage("/translatorpersist snapshot");
					this.state = 7;
					this.ticks = 0;
				}
			}
			case 7 -> {
				// save and quit to the title, as the pause menu's button does
				if (this.ticks > 20) {
					LOGGER.info("Leaving the world for the save-and-reload check");
					this.leaving = mc.getIntegratedServer();
					mc.world.sendQuittingDisconnectingPacket();
					mc.loadWorld(null);
					mc.displayGuiScreen(new GuiMainMenu());
					this.state = 8;
					this.ticks = 0;
				}
			}
			case 8 -> {
				// the world opens again once its server has stopped (it saves while stopping)
				boolean stopped = this.leaving == null || this.leaving.isServerStopped();
				if (this.ticks > 40 && (stopped || this.ticks > 20 * 60)) {
					if (!stopped) {
						LOGGER.error("Automatic self-test: the left world's server didn't stop in a minute, opening it anyway");
					}
					this.leaving = null;
					LOGGER.info("Opening world '{}' again", this.folder);
					mc.launchIntegratedServer(this.folder, this.folder, null);
					this.state = 9;
					this.ticks = 0;
				}
			}
			case 9 -> {
				// back in the world: the chunks around the player 5 seconds, then the comparison
				if (mc.player != null && mc.world != null && mc.currentScreen == null) {
					if (this.ticks > 100) {
						mc.player.sendChatMessage("/translatorpersist check");
						this.state = 10;
						this.ticks = 0;
					}
				} else if (mc.currentScreen instanceof GuiMainMenu && this.ticks > 20 * 60) {
					LOGGER.error("Automatic self-test: the world didn't open again, quitting");
					mc.shutdown();
				} else if (!(mc.currentScreen instanceof GuiMainMenu)) {
					this.ticks = 0;
				}
			}
			case 10 -> {
				if (this.ticks > 40) {
					LOGGER.info("Save-and-reload check done, touring the screens");
					this.tour = new ScreenTour(mc);
					this.state = 4;
					this.ticks = 0;
				}
			}
			default -> {
			}
		}
	}
}
