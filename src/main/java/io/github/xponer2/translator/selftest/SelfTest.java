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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.github.xponer2.translator.runtime.RuntimeMods;
import io.github.xponer2.translator.runtime.RuntimeStubs;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * {@code /translatortest [modid]}: every block and item of the original 1.21.1 mods tried in the world next to the
 * player, one step per server tick (the client opens each screen in between). A block is placed (its block entity
 * made, saved and loaded, ticked), right-clicked (a menu that opens gets every slot clicked, on the server and with
 * the mouse in its screen on the client) and broken, its model's faces checked; an item is used in the air (held for a
 * while when it is used over time, on the server and the client), on a block, swung and used to mine stone, saved and
 * loaded; on the client the models are checked. What failed, and which 1.21 methods the Translator can't run yet were called on the way, go to
 * {@code translator/selftest/report-<time>.md}: what to fix next, by how many blocks and items it breaks.
 */
public final class SelfTest {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator Self-Test");

	private SelfTest() {
	}

	public static final class Command extends CommandBase {
		@Override
		public String getName() {
			return "translatortest";
		}

		@Override
		public String getUsage(ICommandSender sender) {
			return "/translatortest [modid]";
		}

		@Override
		public int getRequiredPermissionLevel() {
			return 2;
		}

		@Override
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			EntityPlayerMP player = getCommandSenderAsPlayer(sender);
			if (running != null) {
				throw new CommandException("A self-test is running already");
			}
			running = new Run(player, args.length > 0 ? args[0] : null);
			MinecraftForge.EVENT_BUS.register(running);
			player.sendMessage(new TextComponentString("Translator self-test: " + running.blocks.size() + " blocks, "
					+ running.items.size() + " items (stand still, the test builds next to you)"));
			if (FMLCommonHandler.instance().getSide().isClient()) {
				SelfTestClient.checkModels(running.blocks, running.items);
			}
		}
	}

	private static Run running;

	/** What one block or item did. */
	static final class Record {
		final String mod;
		final String id;
		final String kind;
		final List<String> problems = new ArrayList<>();
		final Set<String> stubs = new LinkedHashSet<>();
		final List<String> notes = new ArrayList<>();
		/** Written to the automatic test's records already. */
		transient boolean kept;

		Record(String mod, String id, String kind) {
			this.mod = mod;
			this.id = id;
			this.kind = kind;
		}

		boolean ok() {
			return this.problems.isEmpty() && this.stubs.isEmpty();
		}
	}

	static final class Run {
		final EntityPlayerMP player;
		final WorldServer world;
		final BlockPos cell;
		final List<Block> blocks = new ArrayList<>();
		final List<Item> items = new ArrayList<>();
		final ArrayDeque<Runnable> steps = new ArrayDeque<>();
		final List<Record> records = new ArrayList<>();
		/** Unbridged calls of other threads while the test ran (the client every frame): not caused by one block. */
		final Map<String, Integer> background = new java.util.concurrent.ConcurrentHashMap<>();
		Record current;
		final long started = System.currentTimeMillis();
		/** Where the player stood: an item that moves them (a teleport, another dimension) is noted, they come back. */
		final int dimension;
		final double x;
		final double y;
		final double z;
		final File file;
		/** The automatic test's records on disk (resumed after a crash), null for a test by hand. */
		File progress;

		Run(EntityPlayerMP player, String filter) {
			this.player = player;
			this.world = (WorldServer) player.world;
			this.dimension = player.dimension;
			this.x = player.posX;
			this.y = player.posY;
			this.z = player.posZ;
			this.file = new File(this.world.getMinecraftServer().getDataDirectory(), "translator/selftest/report-"
					+ new java.text.SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new java.util.Date()) + ".md");
			// the cell two blocks in front of the player, on a stone floor, everything above cleared
			EnumFacing facing = player.getHorizontalFacing();
			this.cell = new BlockPos(player.posX, player.posY, player.posZ).offset(facing, 2);
			// -Dtranslator.autotest.shard=k/n: every n-th block and item from the k-th (copies of the game share a test)
			int shard = 0;
			int shards = 1;
			String part = System.getProperty("translator.autotest.shard");
			if (part != null && part.matches("\\d+/\\d+")) {
				shard = Integer.parseInt(part.substring(0, part.indexOf('/'))) - 1;
				shards = Math.max(1, Integer.parseInt(part.substring(part.indexOf('/') + 1)));
			}
			int index = 0;
			for (Block block : ForgeRegistries.BLOCKS) {
				var id = block.getRegistryName();
				if (id != null && RuntimeMods.isRuntimeNamespace(id.getNamespace()) && (filter == null || filter.equals(id.getNamespace()))
						&& index++ % shards == shard) {
					this.blocks.add(block);
				}
			}
			for (Item item : ForgeRegistries.ITEMS) {
				var id = item.getRegistryName();
				if (id != null && RuntimeMods.isRuntimeNamespace(id.getNamespace()) && (filter == null || filter.equals(id.getNamespace()))
						&& index++ % shards == shard) {
					this.items.add(item);
				}
			}
			// the automatic test goes on after a crash: what was tested before it, and what crashed the game, isn't again
			this.progress = System.getProperty("translator.autotest") != null
					? new File(this.world.getMinecraftServer().getDataDirectory(), "translator/selftest/autotest/records.jsonl") : null;
			Set<String> done = this.resume();
			for (Block block : this.blocks) {
				if (!done.contains("block " + block.getRegistryName())) {
					this.planBlock(block);
				}
			}
			for (Item item : this.items) {
				if (!done.contains("item " + item.getRegistryName())) {
					this.planItem(item);
				}
			}
			this.steps.add(this::finish);
		}

		/** The records of the automatic test's earlier starts (before a crash), and what crashed it ("kind id" each). */
		private Set<String> resume() {
			Set<String> done = new java.util.HashSet<>();
			if (this.progress == null) {
				return done;
			}
			var gson = new com.google.gson.Gson();
			try {
				if (this.progress.isFile()) {
					for (String line : java.nio.file.Files.readAllLines(this.progress.toPath(), StandardCharsets.UTF_8)) {
						if (!line.isEmpty()) {
							Record record = gson.fromJson(line, Record.class);
							record.kept = true;
							this.records.add(record);
							done.add(record.kind + " " + record.id);
						}
					}
				}
				// written by the script that restarts the game: "kind id<tab>what the crash report says"
				File crashed = new File(this.progress.getParentFile(), "crashed.txt");
				if (crashed.isFile()) {
					for (String line : java.nio.file.Files.readAllLines(crashed.toPath(), StandardCharsets.UTF_8)) {
						String[] cols = line.split("\t", 2);
						String[] key = cols[0].trim().split(" ", 2);
						if (key.length == 2 && done.add(cols[0].trim())) {
							int colon = key[1].indexOf(':');
							Record record = new Record(colon > 0 ? key[1].substring(0, colon) : key[1], key[1], key[0]);
							record.problems.add("crashed the game: " + (cols.length > 1 ? cols[1] : "?"));
							record.kept = true;
							this.records.add(record);
						}
					}
				}
			} catch (IOException | RuntimeException e) {
				LOGGER.error("Can't read the earlier records of the automatic self-test", e);
			}
			if (!this.records.isEmpty()) {
				LOGGER.info("Self-test resumed: {} blocks and items done before", this.records.size());
				this.saveProgress();
			}
			return done;
		}

		/** All records so far, one JSON line each (the automatic test only). */
		private void saveProgress() {
			if (this.progress == null) {
				return;
			}
			var gson = new com.google.gson.Gson();
			var out = new StringBuilder();
			for (Record record : this.records) {
				out.append(gson.toJson(record)).append('\n');
			}
			try {
				this.progress.getParentFile().mkdirs();
				java.nio.file.Files.write(this.progress.toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				LOGGER.error("Can't write the automatic self-test records", e);
			}
		}

		/** A record is done: its client problems go into it, and it is kept on disk (the automatic test only). */
		private void keep(Record record) {
			List<String> client = this.clientProblems.remove(record.kind + " " + record.id);
			if (client != null) {
				synchronized (client) {
					record.problems.addAll(client);
				}
			}
			if (this.progress == null) {
				return;
			}
			try {
				this.progress.getParentFile().mkdirs();
				java.nio.file.Files.write(this.progress.toPath(), (new com.google.gson.Gson().toJson(record) + "\n").getBytes(StandardCharsets.UTF_8),
						java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
			} catch (IOException e) {
				LOGGER.error("Can't write the automatic self-test records", e);
			}
		}

		/**
		 * The steps after this one wait (up to {@code ticks} server ticks) until the client's part is done, then one tick
		 * more: the packets of its clicks reach the server before the menu closes.
		 */
		private void waitFor(java.util.function.BooleanSupplier done, int ticks) {
			Record record = this.current;
			this.steps.addFirst(new Runnable() {
				private int left = ticks;
				private boolean seen;

				@Override
				public void run() {
					Run.this.current = record;
					if (this.seen) {
						return;
					}
					this.seen = done.getAsBoolean();
					if (--this.left > 0 || this.seen) {
						Run.this.steps.addFirst(this);
					} else {
						record.problems.add("client: no answer in " + ticks + " ticks");
					}
				}
			});
		}

		@SubscribeEvent
		public void onTick(TickEvent.ServerTickEvent event) {
			if (event.phase != TickEvent.Phase.END) {
				return;
			}
			Runnable step = this.steps.poll();
			if (step == null) {
				return;
			}
			Thread server = Thread.currentThread();
			RuntimeStubs.listener = what -> {
				// the server thread runs the step; the client (renderers, HUD, events of every frame) is background
				if (Thread.currentThread() == server && this.current != null) {
					this.current.stubs.add(what);
				} else {
					this.background.merge(what, 1, Integer::sum);
				}
			};
			try {
				this.bringBack();
				step.run();
			} catch (Throwable t) {
				if (this.current != null) {
					this.current.problems.add("unexpected: " + describe(t));
				}
			} finally {
				RuntimeStubs.listener = null;
			}
		}

		/** The player moved by what was tested (another dimension, a teleport): noted, back to where the test runs. */
		private void bringBack() {
			if (this.player.dimension != this.dimension) {
				if (this.current != null) {
					this.current.problems.add("took the player to dimension " + this.player.dimension);
				}
				this.player.changeDimension(this.dimension, (world, entity, yaw) -> entity.setPositionAndUpdate(this.x, this.y, this.z));
			} else if (this.player.getDistanceSq(this.x, this.y, this.z) > 4) {
				if (this.current != null) {
					this.current.problems.add("moved the player " + (int) Math.sqrt(this.player.getDistanceSq(this.x, this.y, this.z)) + " blocks");
				}
				this.player.setPositionAndUpdate(this.x, this.y, this.z);
			}
			if (this.player.isDead || this.player.getHealth() <= 0) {
				if (this.current != null) {
					this.current.problems.add("killed the player");
				}
			}
		}

		private void begin(String id, String kind) {
			if (this.current != null && !this.current.kept) {
				this.current.kept = true;
				this.keep(this.current);
			}
			int colon = id.indexOf(':');
			this.current = new Record(colon > 0 ? id.substring(0, colon) : id, id, kind);
			this.records.add(this.current);
			// the line before a crash names what caused it; the report so far survives one
			LOGGER.info("Testing {} {}", kind, id);
			if (this.records.size() % 50 == 0) {
				this.write();
			}
		}

		private int write() {
			int ok = 0;
			for (Record record : this.records) {
				ok += record.ok() ? 1 : 0;
			}
			try {
				this.file.getParentFile().mkdirs();
				java.nio.file.Files.write(this.file.toPath(), this.report(ok).getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				LOGGER.error("Can't write the self-test report", e);
			}
			return ok;
		}

		/** Runs one part of a step: its failure is a problem of the current record, the step goes on. */
		private boolean attempt(String what, ThrowingRunnable action) {
			try {
				action.run();
				return true;
			} catch (Throwable t) {
				this.current.problems.add(what + ": " + describe(t));
				return false;
			}
		}

		private void clearCell() {
			for (int dy = 0; dy < 3; dy++) {
				this.world.setBlockState(this.cell.up(dy), Blocks.AIR.getDefaultState(), 2 | 16);
			}
			this.world.setBlockState(this.cell.down(), Blocks.STONE.getDefaultState(), 2 | 16);
			for (EntityItem drop : this.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(this.cell).grow(3))) {
				drop.setDead();
			}
		}

		private void planBlock(Block block) {
			String id = String.valueOf(block.getRegistryName());
			Record[] record = new Record[1];
			// place, block entity
			this.steps.add(() -> {
				this.begin(id, "block");
				record[0] = this.current;
				this.closeMenu();
				this.player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
				this.clearCell();
				IBlockState state = block.getDefaultState();
				if (!this.attempt("place", () -> this.world.setBlockState(this.cell, state, 3))) {
					return;
				}
				if (this.world.getBlockState(this.cell).getBlock() != block) {
					this.current.problems.add("place: the block isn't there after placing (" + this.world.getBlockState(this.cell) + ")");
					return;
				}
				if (block.hasTileEntity(state)) {
					TileEntity te = this.world.getTileEntity(this.cell);
					if (te == null) {
						this.current.problems.add("block entity: none made");
					} else {
						this.current.notes.add("block entity " + te.getClass().getSimpleName());
						this.attempt("block entity save/load", () -> {
							NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
							te.readFromNBT(tag);
						});
						if (te instanceof ITickable tickable) {
							this.attempt("block entity tick", () -> {
								for (int i = 0; i < 5; i++) {
									tickable.update();
								}
							});
						}
					}
				}
				this.attempt("neighbor update", () -> this.world.neighborChanged(this.cell, Blocks.STONE, this.cell.down()));
			});
			// right-click with an empty hand
			this.steps.add(() -> {
				this.current = record[0];
				if (this.world.getBlockState(this.cell).getBlock() != block) {
					return;
				}
				Container before = this.player.openContainer;
				IBlockState state = this.world.getBlockState(this.cell);
				this.attempt("right-click", () -> block.onBlockActivated(this.world, this.cell, state, this.player, EnumHand.MAIN_HAND,
						EnumFacing.UP, 0.5F, 1.0F, 0.5F));
				if (this.player.openContainer != before && this.player.openContainer != this.player.inventoryContainer) {
					this.current.notes.add("menu " + this.player.openContainer.getClass().getSimpleName() + " (" + this.player.openContainer.inventorySlots.size() + " slots)");
				}
			});
			// the model's faces as the client draws the block; the menu: its screen on the client, every slot clicked
			this.steps.add(() -> {
				this.current = record[0];
				if (FMLCommonHandler.instance().getSide().isClient() && this.world.getBlockState(this.cell).getBlock() == block) {
					this.waitFor(SelfTestClient.checkFaces(this, id, this.cell.toImmutable(), block)::get, 100);
				}
				this.testMenu();
			});
			// break
			this.steps.add(() -> {
				this.current = record[0];
				this.closeMenu();
				if (this.world.getBlockState(this.cell).getBlock() == block) {
					this.attempt("break", () -> this.world.destroyBlock(this.cell, true));
				}
				this.clearCell();
			});
		}

		private void planItem(Item item) {
			String id = String.valueOf(item.getRegistryName());
			Record[] record = new Record[1];
			this.steps.add(() -> {
				this.begin(id, "item");
				record[0] = this.current;
				this.closeMenu();
				this.clearCell();
				ItemStack stack = new ItemStack(item);
				if (stack.isEmpty()) {
					this.current.problems.add("stack: empty");
					return;
				}
				this.attempt("name", stack::getDisplayName);
				this.attempt("save/load", () -> {
					ItemStack copy = new ItemStack(stack.writeToNBT(new NBTTagCompound()));
					if (copy.getItem() != item) {
						throw new IllegalStateException("loaded as " + copy.getItem().getRegistryName());
					}
				});
				this.player.setHeldItem(EnumHand.MAIN_HAND, stack.copy());
				Container before = this.player.openContainer;
				this.attempt("use", () -> this.player.getHeldItemMainhand().useItemRightClick(this.world, this.player, EnumHand.MAIN_HAND));
				if (this.player.openContainer != before && this.player.openContainer != this.player.inventoryContainer) {
					this.current.notes.add("menu " + this.player.openContainer.getClass().getSimpleName() + " (" + this.player.openContainer.inventorySlots.size() + " slots)");
				}
				this.holdUse();
				// the same on the client, as a player's right-click runs it there too
				if (FMLCommonHandler.instance().getSide().isClient()) {
					this.waitFor(SelfTestClient.useItem(this, id, item)::get, 100);
				}
			});
			this.steps.add(() -> {
				this.current = record[0];
				this.testMenu();
			});
			this.steps.add(() -> {
				this.current = record[0];
				this.closeMenu();
				this.player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(item));
				this.attempt("use on a block", () -> this.player.getHeldItemMainhand().onItemUse(this.player, this.world, this.cell.down(),
						EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F));
				this.holdUse();
			});
			// left-click: swung, then mining stone with it (as in survival: the start, the speed, the block broken)
			this.steps.add(() -> {
				this.current = record[0];
				this.closeMenu();
				this.clearCell();
				ItemStack stack = new ItemStack(item);
				this.player.setHeldItem(EnumHand.MAIN_HAND, stack);
				this.attempt("swing", () -> item.onEntitySwing(this.player, stack));
				IBlockState stone = Blocks.STONE.getDefaultState();
				this.world.setBlockState(this.cell, stone, 2 | 16);
				this.attempt("mine: speed", () -> {
					stack.getDestroySpeed(stone);
					item.canHarvestBlock(stone, stack);
				});
				this.attempt("mine: start", () -> item.onBlockStartBreak(stack, this.cell, this.player));
				if (this.world.getBlockState(this.cell).getBlock() == Blocks.STONE) {
					this.attempt("mine: break", () -> {
						this.world.destroyBlock(this.cell, false);
						stack.onBlockDestroyed(this.world, stone, this.cell, this.player);
					});
				}
			});
			this.steps.add(() -> {
				this.current = record[0];
				this.closeMenu();
				this.player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
				this.clearCell();
			});
		}

		/**
		 * An item that is used over time (a bow, food, a mining laser) after its use started: used for a second, then let
		 * go. Called here rather than left to the player's tick, where a failure would stop the server.
		 */
		private void holdUse() {
			if (!this.player.isHandActive()) {
				return;
			}
			ItemStack stack = this.player.getActiveItemStack();
			if (!this.current.notes.contains("used over time")) {
				this.current.notes.add("used over time");
			}
			this.attempt("use: held", () -> {
				int duration = stack.getMaxItemUseDuration();
				for (int tick = 0; tick < 20 && !stack.isEmpty(); tick++) {
					stack.getItem().onUsingTick(stack, this.player, duration - tick);
				}
			});
			this.attempt("use: let go", () -> stack.onPlayerStoppedUsing(this.world, this.player, Math.max(0, stack.getMaxItemUseDuration() - 20)));
			this.player.resetActiveHand();
		}

		/** The open menu (if any): the client's screen for it, then every slot picked up and put back, shift-clicked. */
		private void testMenu() {
			Container menu = this.player.openContainer;
			if (menu == this.player.inventoryContainer) {
				return;
			}
			if (FMLCommonHandler.instance().getSide().isClient()) {
				String screen = SelfTestClient.currentScreen();
				if (screen == null) {
					this.current.problems.add("menu: the server opened " + menu.getClass().getSimpleName() + ", no screen on the client");
				} else {
					this.current.notes.add("screen " + screen);
					// a player's clicks: the screen's own click code, then the packets to this menu
					this.waitFor(SelfTestClient.clickSlots(this, this.current.kind, this.current.id, menu.windowId)::get, 200);
				}
			}
			int slots = menu.inventorySlots.size();
			for (int i = 0; i < slots && this.player.openContainer == menu; i++) {
				int slot = i;
				this.attempt("menu slot " + slot, () -> {
					menu.slotClick(slot, 0, ClickType.PICKUP, this.player);
					menu.slotClick(slot, 0, ClickType.PICKUP, this.player);
					menu.slotClick(slot, 0, ClickType.QUICK_MOVE, this.player);
				});
			}
			this.attempt("menu update", menu::detectAndSendChanges);
			this.player.inventory.setItemStack(ItemStack.EMPTY);
		}

		private void closeMenu() {
			if (this.player.openContainer != this.player.inventoryContainer) {
				try {
					this.player.closeScreen();
				} catch (Throwable t) {
					if (this.current != null) {
						this.current.problems.add("close menu: " + describe(t));
					}
				}
			}
		}

		/** Problems the client found (models), added to the records when the report is written. */
		final Map<String, List<String>> clientProblems = new java.util.concurrent.ConcurrentHashMap<>();

		void addClientProblem(String kind, String id, String problem) {
			this.clientProblems.computeIfAbsent(kind + " " + id, k -> java.util.Collections.synchronizedList(new ArrayList<>())).add(problem);
		}

		private void finish() {
			if (this.current != null && !this.current.kept) {
				this.current.kept = true;
				this.keep(this.current);
			}
			this.current = null;
			for (Record record : this.records) {
				List<String> client = this.clientProblems.get(record.kind + " " + record.id);
				if (client != null) {
					synchronized (client) {
						for (String problem : client) {
							if (!record.problems.contains(problem)) {
								record.problems.add(problem);
							}
						}
					}
				}
			}
			this.saveProgress();
			this.clearCell();
			MinecraftForge.EVENT_BUS.unregister(this);
			running = null;
			int ok = this.write();
			this.player.sendMessage(new TextComponentString("Translator self-test done in " + (System.currentTimeMillis() - this.started) / 1000
					+ " s: " + ok + " of " + this.records.size() + " fully working. Report: " + this.file.getPath()));
			LOGGER.info("Self-test: {} of {} fully working, report {}", ok, this.records.size(), this.file.getAbsolutePath());
		}

		private String report(int ok) {
			var out = new StringBuilder("# Translator self-test\n\n");
			out.append(ok).append(" of ").append(this.records.size()).append(" blocks and items work without a problem or an unbridged call.\n\n");
			// what breaks the most: the unbridged calls and problems by how many blocks and items hit them
			Map<String, Integer> causes = new TreeMap<>();
			for (Record record : this.records) {
				for (String stub : record.stubs) {
					causes.merge(stub, 1, Integer::sum);
				}
				for (String problem : record.problems) {
					causes.merge(problem.replaceAll("menu slot \\d+", "menu slot N"), 1, Integer::sum);
				}
			}
			out.append("## Causes by blocks and items hit\n\n");
			causes.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(80)
					.forEach(e -> out.append("- ").append(e.getValue()).append(": ").append(e.getKey()).append('\n'));
			if (!this.background.isEmpty()) {
				out.append("\n## Background: unbridged calls on the client while the test ran (every frame or tick, not one block)\n\n");
				this.background.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(60)
						.forEach(e -> out.append("- ").append(e.getValue()).append("x: ").append(e.getKey()).append('\n'));
			}
			Map<String, List<Record>> byMod = new LinkedHashMap<>();
			for (Record record : this.records) {
				byMod.computeIfAbsent(record.mod, k -> new ArrayList<>()).add(record);
			}
			for (var entry : byMod.entrySet()) {
				long modOk = entry.getValue().stream().filter(Record::ok).count();
				out.append("\n## ").append(entry.getKey()).append(" (").append(modOk).append(" of ").append(entry.getValue().size()).append(" OK)\n\n");
				for (Record record : entry.getValue()) {
					out.append(record.ok() ? "- OK " : "- **FAIL** ").append(record.kind).append(' ').append(record.id);
					if (!record.notes.isEmpty()) {
						out.append(" (").append(String.join(", ", record.notes)).append(')');
					}
					out.append('\n');
					for (String problem : record.problems) {
						out.append("  - ").append(problem).append('\n');
					}
					for (String stub : record.stubs) {
						out.append("  - not bridged: ").append(stub).append('\n');
					}
				}
			}
			return out.toString();
		}
	}

	static Run running() {
		return running;
	}

	@FunctionalInterface
	interface ThrowingRunnable {
		void run() throws Throwable;
	}

	/** The exception and where in a mod it came from (the first frame outside the game, Forge and the JDK). */
	static String describe(Throwable t) {
		Throwable root = t;
		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		String where = "";
		for (StackTraceElement frame : root.getStackTrace()) {
			String c = frame.getClassName();
			if (!c.startsWith("java.") && !c.startsWith("sun.") && !c.startsWith("net.minecraft.") && !c.startsWith("net.minecraftforge.")
					&& !c.startsWith("io.github.xponer2.translator.selftest.")) {
				where = " at " + c + "." + frame.getMethodName() + ":" + frame.getLineNumber();
				break;
			}
		}
		return root.getClass().getSimpleName() + ": " + root.getMessage() + where;
	}
}
