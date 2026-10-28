package io.github.xponer2.translator.menu;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 menu type -> screen factories. Filled by the mods in {@link RegisterMenuScreensEvent} (posted by
 * {@link #registerAll()} on the client in init).
 */
@SideOnly(Side.CLIENT)
public final class MenuScreens {
	private static final Map<MenuType<?>, ScreenConstructor<?, ?>> SCREENS = new HashMap<>();

	private MenuScreens() {
	}

	public static <M extends Container, S extends GuiScreen> void register(MenuType<? extends M> type, ScreenConstructor<M, S> constructor) {
		SCREENS.put(type, constructor);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public static <M extends Container> GuiScreen create(MenuType<M> type, M menu, InventoryPlayer playerInventory, ITextComponent title) {
		var constructor = (ScreenConstructor<M, ?>) SCREENS.get(type);
		return constructor == null ? null : constructor.create(menu, playerInventory, title);
	}

	public static void registerAll() {
		MinecraftForge.EVENT_BUS.post(new RegisterMenuScreensEvent());
	}

	@FunctionalInterface
	public interface ScreenConstructor<M extends Container, S extends GuiScreen> {
		S create(M menu, InventoryPlayer playerInventory, ITextComponent title);
	}

	public static class RegisterMenuScreensEvent extends Event {
		public <M extends Container, S extends GuiScreen> void register(MenuType<? extends M> type, ScreenConstructor<M, S> constructor) {
			MenuScreens.register(type, constructor);
		}
	}
}
