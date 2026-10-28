package io.github.xponer2.translator.network;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Lets the mods register their payloads (NeoForge {@code RegisterPayloadHandlersEvent}, a mod bus event there).
 * On 1.12.2 it is posted once on {@link MinecraftForge#EVENT_BUS} by {@link #fire()}: a mod registers its handler
 * object on that bus while it is pre-initialized, Cucumber fires the event when it is initialized. Every payload
 * goes to the channel of its id's namespace, so it doesn't matter which mod's registrar registers it.
 */
public class RegisterPayloadHandlersEvent extends Event {
    private static boolean fired;

    public PayloadRegistrar registrar(String version) {
        return new PayloadRegistrar(version);
    }

    /**
     * Posts the event, once.
     */
    public static synchronized void fire() {
        if (!fired) {
            fired = true;
            MinecraftForge.EVENT_BUS.post(new RegisterPayloadHandlersEvent());
        }
    }
}
