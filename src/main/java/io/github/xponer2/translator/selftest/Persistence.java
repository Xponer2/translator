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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import javax.annotation.Nullable;

import io.github.xponer2.translator.runtime.RuntimeMods;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * {@code /translatorpersist place|snapshot|check}: what the 1.21.1 mods keep across a save and a reload. {@code place}
 * puts every block of theirs that has a block entity and every mob of theirs (without its AI, under a roof) in a grid
 * next to the player; {@code snapshot}, just before the game is left, notes what each one saved; {@code check}, after
 * the world was opened again, compares: a block or a mob that is gone, became another class, or lost saved data is a
 * problem (a backpack whose contents vanish, a mob that isn't there after the reload); values that changed (timers) are
 * notes. The report: {@code translator/selftest/persistence-<time>.md}. The automatic test runs it after the self-test.
 */
public final class Persistence {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator Self-Test");
	/** Saved data of an entity that changes by itself between two saves: not compared. */
	private static final Set<String> VOLATILE = com.google.common.collect.ImmutableSet.of("Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air", "OnGround", "Dimension",
			"PortalCooldown", "UUIDMost", "UUIDLeast", "UUID", "HurtTime", "HurtByTimestamp", "DeathTime", "FallFlying", "Invulnerable",
			"WorldUUIDMost", "WorldUUIDLeast", "Leashed");

	private Persistence() {
	}

	private static File file(MinecraftServer server) {
		return new File(server.getDataDirectory(), "translator/selftest/persistence.dat");
	}

	public static final class Command extends CommandBase {
		@Override
		public String getName() {
			return "translatorpersist";
		}

		@Override
		public String getUsage(ICommandSender sender) {
			return "/translatorpersist place [modid] | snapshot | check";
		}

		@Override
		public int getRequiredPermissionLevel() {
			return 2;
		}

		@Override
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			EntityPlayerMP player = getCommandSenderAsPlayer(sender);
			String what = args.length > 0 ? args[0] : "";
			try {
				String said = switch (what) {
					case "place" -> place(player, args.length > 1 ? args[1] : System.getProperty("translator.autotest.mods"));
					case "snapshot" -> snapshot(player);
					case "check" -> check(player);
					default -> throw new CommandException(this.getUsage(sender));
				};
				player.sendMessage(new TextComponentString(said));
				LOGGER.info(said);
			} catch (IOException e) {
				throw new CommandException("Translator persistence: " + e);
			}
		}
	}

	/** Where the grid starts: beside the player, clear of the self-test's cell in front of them. */
	private static BlockPos origin(EntityPlayerMP player) {
		return new BlockPos(player.posX, player.posY, player.posZ).add(6, 0, 6);
	}

	private static String place(EntityPlayerMP player, @Nullable String filter) throws IOException {
		WorldServer world = player.getServerWorld();
		BlockPos origin = origin(player);
		var blocks = new NBTTagList();
		var mobs = new NBTTagList();
		var skipped = new NBTTagList();
		int index = 0;
		for (Block block : ForgeRegistries.BLOCKS) {
			ResourceLocation id = block.getRegistryName();
			if (id == null || !RuntimeMods.isRuntimeNamespace(id.getNamespace()) || !SelfTest.wanted(filter, id.getNamespace())) {
				continue;
			}
			IBlockState state = block.getDefaultState();
			try {
				if (!block.hasTileEntity(state)) {
					continue;
				}
				BlockPos pos = origin.add((index % 16) * 2, 0, (index / 16) * 2);
				index++;
				clear(world, pos, 2);
				world.setBlockState(pos, state, 3);
				var entry = new NBTTagCompound();
				entry.setLong("pos", pos.toLong());
				entry.setString("id", id.toString());
				blocks.appendTag(entry);
			} catch (RuntimeException | LinkageError e) {
				skipped.appendTag(note(id.toString(), "couldn't be placed: " + e));
			}
		}
		// the mobs in rows after the blocks, four apart, each under a roof (no sun burns them before the reload)
		int row = (index + 15) / 16 * 2 + 3;
		int count = 0;
		for (EntityEntry entry : ForgeRegistries.ENTITIES) {
			ResourceLocation id = entry.getRegistryName();
			if (id == null || !RuntimeMods.isRuntimeNamespace(id.getNamespace()) || !SelfTest.wanted(filter, id.getNamespace())
					|| !EntityLiving.class.isAssignableFrom(entry.getEntityClass())) {
				continue;
			}
			if (world.getDifficulty() == EnumDifficulty.PEACEFUL && IMob.class.isAssignableFrom(entry.getEntityClass())) {
				skipped.appendTag(note(id.toString(), "a monster in a peaceful world (removed by the game)"));
				continue;
			}
			BlockPos pos = origin.add((count % 8) * 4, 0, row + (count / 8) * 4);
			try {
				Entity entity = entry.newInstance(world);
				if (!entity.isNonBoss()) {
					skipped.appendTag(note(id.toString(), "a boss (left out: it may build or break things around it)"));
					continue;
				}
				count++;
				clear(world, pos, 3);
				world.setBlockState(pos.up(3), Blocks.STONE.getDefaultState(), 2 | 16);
				entity.setLocationAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
				EntityLiving living = (EntityLiving) entity;
				living.setNoAI(true);
				living.enablePersistence();
				if (!world.spawnEntity(entity)) {
					skipped.appendTag(note(id.toString(), "the world didn't take it"));
					continue;
				}
				var mob = new NBTTagCompound();
				mob.setUniqueId("uuid", entity.getUniqueID());
				mob.setString("id", id.toString());
				mobs.appendTag(mob);
			} catch (RuntimeException | LinkageError e) {
				skipped.appendTag(note(id.toString(), "couldn't be spawned: " + e));
			}
		}
		var root = new NBTTagCompound();
		root.setTag("blocks", blocks);
		root.setTag("mobs", mobs);
		root.setTag("skipped", skipped);
		File file = file(world.getMinecraftServer());
		file.getParentFile().mkdirs();
		CompressedStreamTools.write(root, file);
		return "Translator persistence: " + blocks.tagCount() + " block entities and " + mobs.tagCount() + " mobs placed ("
				+ skipped.tagCount() + " left out); /translatorpersist snapshot before leaving, check after coming back";
	}

	/** A cell on a stone floor with {@code height} blocks of air above it. */
	private static void clear(WorldServer world, BlockPos pos, int height) {
		world.setBlockState(pos.down(), Blocks.STONE.getDefaultState(), 2 | 16);
		for (int dy = 0; dy < height; dy++) {
			world.setBlockState(pos.up(dy), Blocks.AIR.getDefaultState(), 2 | 16);
		}
	}

	private static NBTTagCompound note(String id, String text) {
		var note = new NBTTagCompound();
		note.setString("id", id);
		note.setString("note", text);
		return note;
	}

	private static String snapshot(EntityPlayerMP player) throws IOException {
		WorldServer world = player.getServerWorld();
		File file = file(world.getMinecraftServer());
		NBTTagCompound root = CompressedStreamTools.read(file);
		if (root == null) {
			return "Translator persistence: nothing placed (/translatorpersist place first)";
		}
		int saved = 0;
		NBTTagList blocks = root.getTagList("blocks", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < blocks.tagCount(); i++) {
			NBTTagCompound entry = blocks.getCompoundTagAt(i);
			long where = entry.getLong("pos");
			String placed = entry.getString("id");
			BlockPos at = BlockPos.fromLong(where);
			TileEntity tile = world.getTileEntity(at);
			// what is there now: a block that turns into another by itself (a launcher's arm, a temporary block) is no
			// save problem
			String now = String.valueOf(world.getBlockState(at).getBlock().getRegistryName());
			entry.removeTag("data");
			entry.removeTag("class");
			if (tile != null) {
				entry.setString("class", tile.getClass().getName());
				try {
					entry.setTag("data", tile.writeToNBT(new NBTTagCompound()).copy());
					saved++;
				} catch (RuntimeException | LinkageError e) {
					entry.setString("error", "saving failed: " + e);
				}
			}
			// what a block entity's saving did to the shared tags aside, the entry keeps where and what it is (two of
			// Iron's Spells' lost them: the check looked at 0,0,0)
			entry.setLong("pos", where);
			entry.setString("id", placed);
			entry.setString("now", now);
		}
		NBTTagList mobs = root.getTagList("mobs", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < mobs.tagCount(); i++) {
			NBTTagCompound mob = mobs.getCompoundTagAt(i);
			Entity entity = world.getEntityFromUuid(mob.getUniqueId("uuid"));
			mob.removeTag("data");
			mob.removeTag("class");
			if (entity != null && !entity.isDead) {
				mob.setString("class", entity.getClass().getName());
				try {
					mob.setTag("data", entity.writeToNBT(new NBTTagCompound()));
					saved++;
				} catch (RuntimeException | LinkageError e) {
					mob.setString("error", "saving failed: " + e);
				}
			}
		}
		CompressedStreamTools.write(root, file);
		return "Translator persistence: " + saved + " saved for the comparison; leave the world and come back, then /translatorpersist check";
	}

	private static String check(EntityPlayerMP player) throws IOException {
		WorldServer world = player.getServerWorld();
		NBTTagCompound root = CompressedStreamTools.read(file(world.getMinecraftServer()));
		if (root == null) {
			return "Translator persistence: nothing placed (/translatorpersist place first)";
		}
		// mod -> lines; a line starting with "FAIL" is a problem
		var byMod = new TreeMap<String, List<String>>();
		int total = 0;
		int kept = 0;
		NBTTagList blocks = root.getTagList("blocks", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < blocks.tagCount(); i++) {
			NBTTagCompound entry = blocks.getCompoundTagAt(i);
			String id = entry.getString("id");
			BlockPos pos = BlockPos.fromLong(entry.getLong("pos"));
			total++;
			var problems = new ArrayList<String>();
			var notes = new ArrayList<String>();
			ResourceLocation now = world.getBlockState(pos).getBlock().getRegistryName();
			TileEntity tile = world.getTileEntity(pos);
			if (entry.hasKey("error")) {
				problems.add(entry.getString("error") + " (before leaving)");
			}
			String saved = entry.hasKey("now") ? entry.getString("now") : id;
			if (!saved.equals(id)) {
				notes.add("it turned into " + saved + " by itself before leaving");
			}
			if (!saved.equals(String.valueOf(now))) {
				problems.add("the block is " + now + " after the reload (was " + saved + ")");
			} else if (!saved.equals(id)) {
				// another block now: its block entity isn't this one's
			} else if (tile == null) {
				problems.add(entry.hasKey("class") ? "its block entity is gone after the reload"
						: "it never got its block entity (placed, but none was made: not a save problem)");
			} else if (entry.hasKey("class") && !entry.getString("class").equals(tile.getClass().getName())) {
				problems.add("its block entity is a " + tile.getClass().getName() + " after the reload (was " + entry.getString("class") + ")");
			} else if (entry.hasKey("data")) {
				try {
					compare(entry.getCompoundTag("data"), tile.writeToNBT(new NBTTagCompound()), com.google.common.collect.ImmutableSet.of("x", "y", "z"), problems, notes);
				} catch (RuntimeException | LinkageError e) {
					problems.add("saving it after the reload failed: " + e);
				}
			}
			kept += problems.isEmpty() ? 1 : 0;
			add(byMod, "block " + id, problems, notes);
		}
		NBTTagList mobs = root.getTagList("mobs", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < mobs.tagCount(); i++) {
			NBTTagCompound mob = mobs.getCompoundTagAt(i);
			String id = mob.getString("id");
			UUID uuid = mob.getUniqueId("uuid");
			total++;
			var problems = new ArrayList<String>();
			var notes = new ArrayList<String>();
			Entity entity = world.getEntityFromUuid(uuid);
			if (mob.hasKey("error")) {
				problems.add(mob.getString("error") + " (before leaving)");
			}
			if (!mob.hasKey("class")) {
				problems.add("it was gone before leaving already (it died or despawned)");
			} else if (entity == null || entity.isDead) {
				problems.add("it is gone after the reload (not saved or not loaded back)");
			} else if (!mob.getString("class").equals(entity.getClass().getName())) {
				problems.add("it is a " + entity.getClass().getName() + " after the reload (was " + mob.getString("class") + ")");
			} else if (mob.hasKey("data")) {
				try {
					compare(mob.getCompoundTag("data"), entity.writeToNBT(new NBTTagCompound()), VOLATILE, problems, notes);
				} catch (RuntimeException | LinkageError e) {
					problems.add("saving it after the reload failed: " + e);
				}
			}
			kept += problems.isEmpty() ? 1 : 0;
			add(byMod, "mob " + id, problems, notes);
		}
		NBTTagList skipped = root.getTagList("skipped", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < skipped.tagCount(); i++) {
			NBTTagCompound note = skipped.getCompoundTagAt(i);
			String id = note.getString("id");
			byMod.computeIfAbsent(id.substring(0, Math.max(0, id.indexOf(':'))), k -> new ArrayList<>())
					.add("- left out " + id + ": " + note.getString("note"));
		}
		var report = new StringBuilder("# Translator save and reload\n\n").append(kept).append(" of ").append(total)
				.append(" block entities and mobs of the 1.21.1 mods came back as they were saved.\n");
		for (Map.Entry<String, List<String>> mod : byMod.entrySet()) {
			report.append("\n## ").append(mod.getKey()).append("\n\n");
			for (String line : mod.getValue()) {
				report.append(line).append('\n');
			}
		}
		File out = new File(world.getMinecraftServer().getDataDirectory(), "translator/selftest/persistence-"
				+ new java.text.SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new java.util.Date()) + ".md");
		java.nio.file.Files.write(out.toPath(), report.toString().getBytes(StandardCharsets.UTF_8));
		return "Translator persistence: " + kept + " of " + total + " came back as saved, report " + out.getPath();
	}

	private static void add(Map<String, List<String>> byMod, String what, List<String> problems, List<String> notes) {
		String id = what.substring(what.indexOf(' ') + 1);
		List<String> lines = byMod.computeIfAbsent(id.substring(0, Math.max(0, id.indexOf(':'))), k -> new ArrayList<>());
		if (problems.isEmpty()) {
			lines.add("- OK " + what + (notes.isEmpty() ? "" : " (" + String.join("; ", notes) + ")"));
			return;
		}
		lines.add("- **FAIL** " + what);
		for (String problem : problems) {
			lines.add("  - " + problem);
		}
		for (String note : notes) {
			lines.add("  - note: " + note);
		}
	}

	/** Saved data before and after: a key lost is a problem (data gone), a value changed a note (timers change). */
	private static void compare(NBTTagCompound before, NBTTagCompound after, Set<String> ignored, List<String> problems, List<String> notes) {
		var lost = new ArrayList<String>();
		var changed = new ArrayList<String>();
		for (String key : new java.util.TreeSet<>(before.getKeySet())) {
			if (ignored.contains(key)) {
				continue;
			}
			if (!after.hasKey(key)) {
				lost.add(key);
			} else if (!before.getTag(key).equals(after.getTag(key))) {
				changed.add(key);
			}
		}
		if (!lost.isEmpty()) {
			problems.add("saved data lost in the reload: " + String.join(", ", lost));
		}
		if (!changed.isEmpty()) {
			notes.add("changed across the reload: " + String.join(", ", changed));
		}
	}
}
