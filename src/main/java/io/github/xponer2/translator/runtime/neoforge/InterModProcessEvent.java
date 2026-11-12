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
package io.github.xponer2.translator.runtime.neoforge;

/** FML lifecycle event, posted by the runtime loader at the matching 1.12.2 stage. */
public class InterModProcessEvent extends ParallelDispatchEvent {
	/**
	 * FML {@code getIMCStream()}: the messages other mods sent the mod whose listener runs (Curios' slot types, Mekanism's
	 * and Iron's Spells' integrations). The messages are FML's own (its InterModComms, carried); the event bus marks
	 * the mod of each listener of this event.
	 */
	public java.util.stream.Stream<?> getIMCStream() {
		return this.getIMCStream(method -> true);
	}

	/** FML {@code getIMCStream(methods)}: those of the messages whose method the filter takes. */
	public java.util.stream.Stream<?> getIMCStream(java.util.function.Predicate<String> methods) {
		ModContainer mod = ModLoadingContext.get().getActiveContainer();
		if (mod == null) {
			return java.util.stream.Stream.empty();
		}
		try {
			Class<?> comms = Class.forName("net.neoforged.fml.InterModComms", true, net.minecraft.launchwrapper.Launch.classLoader);
			return (java.util.stream.Stream<?>) comms.getMethod("getMessages", String.class, java.util.function.Predicate.class)
					.invoke(null, mod.getModId(), methods);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the messages to " + mod.getModId(), e);
			return java.util.stream.Stream.empty();
		}
	}
}
