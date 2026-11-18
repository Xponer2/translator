/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.provided.network;

import java.util.function.Function;

import ladder.carried.net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

/**
 * NeoForge 20.5's {@code RegisterPayloadHandlersEvent}, posted to every mod's bus from 20.4's
 * {@code RegisterPayloadHandlerEvent} (PayloadBridge). 1.21's registrar takes the network version (20.4's took the
 * namespace).
 */
public class RegisterPayloadHandlersEvent extends Event implements IModBusEvent {
    private final Function<String, IPayloadRegistrar> lower;

    public RegisterPayloadHandlersEvent(Function<String, IPayloadRegistrar> lower) {
        this.lower = lower;
    }

    public PayloadRegistrar registrar(String version) {
        return new PayloadRegistrar(version, this.lower, false, HandlerThread.MAIN);
    }
}
