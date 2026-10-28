package io.github.xponer2.translator.menu;

import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.util.BuiltInRegistries;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client side of {@link Menus}: opens the container and screen of a menu the server opened.
 */
@SideOnly(Side.CLIENT)
final class ClientMenus {
	private static final Logger LOGGER = LogManager.getLogger("Cucumber Menus");

	private ClientMenus() {
	}

	static void schedule(Menus.OpenMenuMessage message) {
		Minecraft.getMinecraft().addScheduledTask(() -> open(message));
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void open(Menus.OpenMenuMessage message) {
		var mc = Minecraft.getMinecraft();
		var player = mc.player;
		MenuType type = BuiltInRegistries.MENU.get(message.type);
		if (player == null || type == null) {
			LOGGER.warn("Can't open menu {}: unknown menu type", message.type);
			return;
		}

		var container = type.create(message.windowId, player.inventory, new RegistryFriendlyByteBuf(message.data));
		var screen = MenuScreens.create(type, container, player.inventory, message.title);
		if (screen == null) {
			LOGGER.warn("Can't open menu {}: no screen registered", message.type);
			return;
		}

		mc.displayGuiScreen(screen);
		// GuiContainer#initGui made it the open container of the player
		player.openContainer.windowId = message.windowId;
	}
}
