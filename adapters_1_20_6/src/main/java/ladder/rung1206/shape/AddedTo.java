/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.shape;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A template class whose static methods an add-static shape patch copies into a game class (the value: its internal
 * name). The adapters' build rewrites the other adapters' calls of the template's methods to calls of the game class's,
 * so their bytecode makes the call 1.21 makes (a mod's injection point there).
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface AddedTo {
    String value();
}
