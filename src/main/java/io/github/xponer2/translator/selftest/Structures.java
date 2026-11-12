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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

import io.github.xponer2.translator.world.StructureGeneration;

/**
 * {@code /translatorstructures [modid,...]}: every structure of the 1.21.1 mods ({@code data/<mod>/worldgen/structure})
 * made and placed whole by its own 1.21.1 code, each in its own spot far from the player (a row along x, 32 chunks
 * apart), whatever its biome and placement say: whether it can be made at all here. A structure that throws is a
 * problem (the code it reached, from the exception); one that makes no start was refused by its own checks there (the
 * ground, the height) and is a note; one placed says its pieces and size. Report:
 * {@code translator/selftest/structures-<time>.md}; the automatic test runs it with
 * {@code -Dtranslator.autotest.structures=true}.
 */
public final class Structures {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator Self-Test");
	/** Set when a run has written its report (the automatic test waits for it). */
	public static volatile boolean finished;

	private Structures() {
	}

	public static final class Command extends CommandBase {
		@Override
		public String getName() {
			return "translatorstructures";
		}

		@Override
		public String getUsage(ICommandSender sender) {
			return "/translatorstructures [modid,...]";
		}

		@Override
		public int getRequiredPermissionLevel() {
			return 2;
		}

		@Override
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			EntityPlayerMP player = getCommandSenderAsPlayer(sender);
			String filter = args.length > 0 ? args[0] : System.getProperty("translator.autotest.mods");
			try {
				String said = run(server, player.getServerWorld(), player.chunkCoordX, player.chunkCoordZ, filter);
				player.sendMessage(new TextComponentString(said));
				LOGGER.info(said);
			} catch (IOException e) {
				throw new CommandException("Translator structures: " + e);
			} finally {
				finished = true;
			}
		}
	}

	private static String run(MinecraftServer server, WorldServer world, int playerChunkX, int playerChunkZ, @Nullable String filter) throws IOException {
		List<String> placed = new ArrayList<>();
		List<String> refused = new ArrayList<>();
		List<String> failed = new ArrayList<>();
		int index = 0;
		for (ResourceLocation id : StructureGeneration.modStructures()) {
			if (!SelfTest.wanted(filter, id.getNamespace())) {
				continue;
			}
			int chunkX = playerChunkX + 64 + 32 * index;
			int chunkZ = playerChunkZ + 64;
			index++;
			try {
				String result = StructureGeneration.placeForTest(world, id, chunkX, chunkZ);
				if (result == null) {
					refused.add("- " + id + ": no start at chunk " + chunkX + ", " + chunkZ);
				} else {
					placed.add("- " + id + ": " + result);
				}
			} catch (Throwable e) {
				StackTraceElement where = null;
				for (StackTraceElement element : e.getStackTrace()) {
					if (!element.getClassName().startsWith("java.") && !element.getClassName().startsWith("sun.")) {
						where = element;
						break;
					}
				}
				failed.add("- " + id + ": " + e + (where == null ? "" : " (at " + where + ")"));
				LOGGER.warn("Structure {} failed in the self-test", id, e);
			}
		}
		String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
		File file = new File(server.getDataDirectory(), "translator/selftest/structures-" + time + ".md");
		file.getParentFile().mkdirs();
		List<String> lines = new ArrayList<>();
		lines.add("# Structures of the 1.21.1 mods: " + placed.size() + " placed, " + refused.size() + " made no start, " + failed.size() + " failed");
		lines.add("");
		lines.add("Each made and placed whole in its own spot (its biome and placement not asked); dimension " + world.provider.getDimension() + ".");
		lines.add("");
		lines.add("## Failed (" + failed.size() + ")");
		lines.addAll(failed);
		lines.add("");
		lines.add("## Made no start (" + refused.size() + ")");
		lines.addAll(refused);
		lines.add("");
		lines.add("## Placed (" + placed.size() + ")");
		lines.addAll(placed);
		Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
		return "Translator structures: " + placed.size() + " placed, " + refused.size() + " made no start, " + failed.size() + " failed; report "
				+ file.getName();
	}
}
