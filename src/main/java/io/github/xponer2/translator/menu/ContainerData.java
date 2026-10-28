package io.github.xponer2.translator.menu;

/**
 * Integers a container syncs to its screen (1.21 {@code ContainerData}): progress, fuel, energy, ...
 */
public interface ContainerData {
    int get(int index);

    void set(int index, int value);

    int getCount();
}
