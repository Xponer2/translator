/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testf;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge 21.1 additions: a menu provider's client data (writeClientSideData: the client's menu is made of it) and the
 * dimension transition screens (ClientSide). The server opens the menu for a player that joined.
 */
@Mod("ladder_test_f")
public final class LadderTestF {
    static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "ladder_test_f");
    static final DeferredHolder<MenuType<?>, MenuType<DataMenu>> DATA_MENU = MENUS.register("data",
            () -> IMenuTypeExtension.create((id, inventory, buffer) -> new DataMenu(id, buffer.readVarInt(), buffer.readUtf())));

    /** what the client's menu was made of (its data from the server), or "-" */
    static volatile String clientMenu = "-";
    private static ServerPlayer joined;
    private static int ticks;

    public LadderTestF(IEventBus modBus) {
        MENUS.register(modBus);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                joined = player;
                ticks = 0;
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            if (joined != null && ++ticks == 20) {
                // the provider's own data first, then the opener's (21.1's order)
                joined.openMenu(new DataProvider(), buffer -> buffer.writeUtf("opener"));
                joined = null;
            }
        });
        if (FMLEnvironment.dist.isClient()) {
            ClientSide.init(modBus);
        }
    }

    /** a menu provider that writes client data (NeoForge 21.1's writeClientSideData) */
    static final class DataProvider implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.literal("Data");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new DataMenu(id, 0, "");
        }

        @Override
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(42 + menu.containerId * 0);
        }
    }

    static final class DataMenu extends AbstractContainerMenu {
        DataMenu(int id, int value, String opener) {
            super(DATA_MENU.get(), id);
            if (!opener.isEmpty()) {
                clientMenu = value + " " + opener;
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
