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
package io.github.xponer2.translator.menu;

import java.util.function.Consumer;

import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 1.21 menu opening (NeoForge {@code player.openMenu(provider, extraData)}) on 1.12.2: the server creates the
 * container, the client gets the menu type, window id, title and extra data in one packet and creates its
 * container with the menu type and its screen with {@link MenuScreens}. Call {@link #init()} in pre-init.
 */
public final class Menus {
	private static SimpleNetworkWrapper channel;

	private Menus() {
	}

	public static void init() {
		channel = NetworkRegistry.INSTANCE.newSimpleChannel("cucumber_menus");
		channel.registerMessage(OpenMenuMessage.Handler.class, OpenMenuMessage.class, 0, Side.CLIENT);
	}

	public static void openMenu(EntityPlayer player, MenuProvider provider) {
		openMenu(player, provider, buffer -> { });
	}

	public static void openMenu(EntityPlayer player, MenuProvider provider, BlockPos pos) {
		openMenu(player, provider, buffer -> buffer.writeBlockPos(pos));
	}

	public static void openMenu(EntityPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData) {
		if (!(player instanceof EntityPlayerMP serverPlayer)) {
			return;
		}

		if (serverPlayer.openContainer != serverPlayer.inventoryContainer) {
			serverPlayer.closeScreen();
		}

		serverPlayer.getNextWindowId();
		int windowId = serverPlayer.currentWindowId;
		var container = provider.createMenu(windowId, serverPlayer.inventory, serverPlayer);
		if (container == null) {
			return;
		}

		container.windowId = windowId;
		var type = container instanceof AbstractContainerMenu menu ? menu.getType().getId() : null;
		if (type == null) {
			throw new IllegalStateException("Menu " + container.getClass().getName() + " has no registered menu type");
		}

		var data = new RegistryFriendlyByteBuf(Unpooled.buffer());
		extraData.accept(data);
		var title = provider.getDisplayName();
		channel.sendTo(new OpenMenuMessage(type, windowId, title == null ? new TextComponentString("") : title, data), serverPlayer);

		serverPlayer.openContainer = container;
		container.addListener(serverPlayer);
		MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(serverPlayer, container));
	}

	public static final class OpenMenuMessage implements IMessage {
		ResourceLocation type;
		int windowId;
		ITextComponent title;
		ByteBuf data;

		public OpenMenuMessage() {
		}

		OpenMenuMessage(ResourceLocation type, int windowId, ITextComponent title, ByteBuf data) {
			this.type = type;
			this.windowId = windowId;
			this.title = title;
			this.data = data;
		}

		@Override
		public void toBytes(ByteBuf buf) {
			var buffer = new PacketBuffer(buf);
			buffer.writeResourceLocation(this.type);
			buffer.writeVarInt(this.windowId);
			buffer.writeTextComponent(this.title);
			buffer.writeVarInt(this.data.readableBytes());
			buffer.writeBytes(this.data, this.data.readerIndex(), this.data.readableBytes());
		}

		@Override
		public void fromBytes(ByteBuf buf) {
			var buffer = new PacketBuffer(buf);
			this.type = buffer.readResourceLocation();
			this.windowId = buffer.readVarInt();
			try {
				this.title = buffer.readTextComponent();
			} catch (java.io.IOException e) {
				this.title = new TextComponentString("");
			}
			this.data = Unpooled.copiedBuffer(buffer.readBytes(buffer.readVarInt()));
		}

		public static final class Handler implements IMessageHandler<OpenMenuMessage, IMessage> {
			@Override
			public IMessage onMessage(OpenMenuMessage message, MessageContext ctx) {
				// the client classes are only loaded here, on the client
				ClientMenus.schedule(message);
				return null;
			}
		}
	}
}
